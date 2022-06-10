package io.github.iprodigy.twitch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.github.twitch4j.chat.TwitchChatBuilder
import com.github.twitch4j.chat.events.channel.UserStateEvent
import com.github.twitch4j.chat.util.TwitchChatLimitHelper
import com.github.twitch4j.client.websocket.WebsocketConnection
import com.github.twitch4j.common.util.ThreadUtils
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.Duration
import kotlin.concurrent.fixedRateTimer
import kotlin.io.path.readText
import kotlin.io.path.toPath
import kotlin.io.path.writeText

private const val CONFIG_FILE_NAME = "./config.json"
private const val WSS_PING_PERIOD = 30_000
private const val TWITCH_MAX_MESSAGE_LENGTH = 500

object Bot {
    val log = LoggerFactory.getLogger(javaClass)!!

    private val mapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // registerModule(JavaTimeModule())
    }
    private val config = readConfig()
    private val exec = ThreadUtils.getDefaultScheduledThreadPoolExecutor("ChatMirrorPool", Runtime.getRuntime().availableProcessors())

    private val tip = config?.let { TwitchIdentityProvider(it.clientId, it.clientSecret, "") }
    private val credential = config?.accessToken?.let {
        OAuth2Credential("twitch", it).apply {
            refreshToken = config.refreshToken
        }.let { cred ->
            tip!!.getAdditionalCredentialInformation(cred).orElseGet {
                tip.refreshCredential(cred).orElse(null)
            }
        }
    }
    private val twitchChat = credential?.let {
        TwitchChatBuilder.builder()
            .withChatAccount(it)
            .withChatRateLimit(
                if (config?.twitchMod == true)
                    TwitchChatLimitHelper.MOD_MESSAGE_LIMIT
                else
                    TwitchChatLimitHelper.USER_MESSAGE_LIMIT
            )
            .withCredentialManager(
                CredentialManagerBuilder.builder().build().apply {
                    if (tip != null) registerIdentityProvider(tip)
                }
            )
            .withScheduledThreadPoolExecutor(exec)
            .withWsPingPeriod(WSS_PING_PERIOD)
            .build()
    }

    private val socketConnection by lazy {
        WebsocketConnection {
            it.baseUrl(config?.chatSocketUrl)
            it.taskExecutor(exec)
            it.wsPingPeriod(WSS_PING_PERIOD)

            it.onTextMessage { msg ->
                if (msg.startsWith("MSG {")) {
                    val json = msg.substring("MSG ".length)
                    exec.execute {
                        val parsed = try {
                            mapper.readValue(json, SocketChatMessage::class.java)
                        } catch (e: Exception) {
                            log.warn("Failed to parse socket chat message: $msg", e)
                            null
                        }

                        if (parsed != null) {
                            log.trace("Received message: $json")
                            handleChatMessage(parsed)
                        }
                    }
                } else {
                    log.trace("Ignoring message: $msg")
                }
            }
        }
    }

    fun start() {
        assert(hasValidConfig())

        log.info("Starting bot...")

        // Keep our chat credential refreshed
        if (credential!!.expiresIn > 0) {
            log.debug("Initializing credential refresh task...")
            fixedRateTimer(initialDelay = Duration.ofSeconds(credential.expiresIn / 2L).toMillis(), period = Duration.ofHours(1L).toMillis()) {
                val refreshed = tip!!.refreshCredential(credential)
                if (refreshed.isPresent) {
                    log.trace("Successfully refreshed credential")

                    val it = refreshed.get()
                    credential.accessToken = it.accessToken
                    credential.refreshToken = it.refreshToken
                    credential.expiresIn = it.expiresIn

                    config!!.accessToken = it.accessToken
                    config.refreshToken = it.refreshToken

                    writeConfig()
                } else {
                    log.warn("Failed to refresh credential")
                }
            }
        }

        // Connect to chat socket (& start the message forwarder)
        log.debug("Attempting to connect to chat socket...")
        socketConnection.connect()

        // Track mod status
        twitchChat!!.eventManager.onEvent("bot-mod-tracker", UserStateEvent::class.java) {
            if (config!!.twitchMod != it.isModerator) {
                config.twitchMod = it.isModerator
                log.info("Bot twitch status changed to: ${if (it.isModerator) "modded" else "not modded"}")
            }
        }
    }

    fun hasValidConfig() = config != null && config.accessToken.isNotBlank() && config.chatSocketUrl.isNotBlank() && config.twitchChannelName.isNotBlank() && checkToken()

    private fun readConfig(): ConfigSettings? = try {
        getConfigResource().let {
            if (it != null) {
                it.readText().let { text ->
                    log.trace("Read config contents: $text")
                    mapper.readValue(text, ConfigSettings::class.java)
                }
            } else {
                log.warn("Failed to locate config")
                null
            }
        }
    } catch (e: Exception) {
        log.error("Failed to read or parse config", e)
        null
    }

    private fun writeConfig() = try {
        getConfigResource()?.apply {
            writeText(mapper.writeValueAsString(config))
            log.debug("Successfully wrote latest config file")
        }
    } catch (e: Exception) {
        log.error("Failed to write config", e)
    }

    private fun getConfigResource() = try {
        this::class.java.classLoader.getResource(CONFIG_FILE_NAME)?.toURI()?.toPath() ?: Paths.get(CONFIG_FILE_NAME)
    } catch (e: Exception) {
        log.error("Failed to obtain config", e)
        null
    }

    private fun checkToken() = credential != null && credential.userId.isNullOrEmpty().not()

    private fun handleChatMessage(message: SocketChatMessage) {
        if (config!!.ignoreBots && message.isBot()) return
        if (config.subsOnly && message.isPrivileged().not()) return

        val pronouns = if (config.includePronouns) message.pronouns?.let { pronounsById[it] }?.let { " ($it)" } ?: "" else ""
        val msg = "${config.twitchMessagePrefix} ${message.nick}$pronouns: ${message.data}".trim().take(TWITCH_MAX_MESSAGE_LENGTH)
        if (message.nick.startsWith('/').not() && message.data.startsWith('/').not())
            twitchChat!!.sendMessage(config.twitchChannelName, msg)
    }
}
