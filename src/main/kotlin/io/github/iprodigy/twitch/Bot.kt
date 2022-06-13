package io.github.iprodigy.twitch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.auth.providers.TwitchIdentityProvider
import com.github.twitch4j.chat.events.CommandEvent
import com.github.twitch4j.chat.events.channel.ChannelNoticeEvent
import com.github.twitch4j.chat.events.channel.UserStateEvent
import com.github.twitch4j.common.enums.TwitchLimitType
import com.github.twitch4j.common.util.ThreadUtils
import com.github.twitch4j.common.util.TwitchLimitRegistry
import io.github.iprodigy.twitch.config.ConfigManager
import io.github.iprodigy.twitch.util.chatRateLimit
import io.github.iprodigy.twitch.util.executeOrNull
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.concurrent.fixedRateTimer

val mapper = jacksonObjectMapper().apply {
    propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    // registerModule(JavaTimeModule())
}

object Bot {
    internal val log = LoggerFactory.getLogger(javaClass)!!
    internal val config = ConfigManager.config
    private val exec = ThreadUtils.getDefaultScheduledThreadPoolExecutor("ChatMirrorPool", Runtime.getRuntime().availableProcessors())

    private val tip = config?.let { TwitchIdentityProvider(it.clientId, it.clientSecret, "") }
    internal val credential = config?.accessToken?.let {
        OAuth2Credential("twitch", it).apply {
            refreshToken = config.refreshToken
        }.let { cred ->
            tip!!.getAdditionalCredentialInformation(cred).orElseGet {
                tip.refreshCredential(cred).orElse(null)
            }
        }
    }
    val twitchClient = config?.let {
        TwitchClientBuilder.builder()
            .withChatAccount(credential)
            .withChatRateLimit(chatRateLimit(it.twitchMod))
            .withClientId(it.clientId)
            .withClientSecret(it.clientSecret)
            .withCommandTrigger(it.commandTrigger)
            .withCredentialManager(CredentialManagerBuilder.builder().build().apply { if (tip != null) registerIdentityProvider(tip) })
            .withDefaultAuthToken(credential?.takeIf { x -> x.expiresIn == 0 })
            .withDefaultFirstPartyToken(OAuth2Credential("twitch", it.firstPartyToken ?: ""))
            .withEnableChat(true)
            .withEnableGraphQL(it.firstPartyToken.isNullOrBlank().not())
            .withEnableHelix(true)
            .withScheduledThreadPoolExecutor(exec)
            .withWsPingPeriod(WSS_PING_PERIOD)
            .build()
    }
    val channelId: String? by lazy { twitchClient?.helix?.getUsers(null, null, listOf(config!!.twitchChannelName))?.executeOrNull()?.users?.firstOrNull()?.id }

    internal val socketConnection by lazy { config?.let { SiteChatConnection(it.chatSocketUrl, exec) } }

    fun start() {
        assert(hasValidToken())

        log.info("Starting bot...")

        // Start moderation helper service
        ModerationHelper.start(config!!.twitchChannelName)

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

                    config.accessToken = it.accessToken
                    config.refreshToken = it.refreshToken

                    ConfigManager.writeConfig()
                } else {
                    log.warn("Failed to refresh credential")
                }
            }
        }

        // Connect to chat socket (& start the message forwarder)
        log.debug("Attempting to connect to chat socket...")
        socketConnection!!.connect()

        // Track mod status
        twitchClient!!.eventManager.onEvent("bot-mod-tracker", UserStateEvent::class.java) {
            if (config.twitchMod != it.isModerator) {
                config.twitchMod = it.isModerator
                TwitchLimitRegistry.getInstance().setLimit(credential.userId, TwitchLimitType.CHAT_MESSAGE_LIMIT, listOf(chatRateLimit(it.isModerator)))
                log.info("Bot twitch status changed to: ${if (it.isModerator) "modded" else "not modded"}")
            }
        }

        // Forward twitch commands to handler
        twitchClient.chat.joinChannel(config.twitchChannelName)
        twitchClient.eventManager.onEvent("command-tracker", CommandEvent::class.java) {
            if (it.commandPrefix.isNotEmpty())
                TwitchCommandManager.accept(it)
        }

        // Log notices sent by twitch
        twitchClient.eventManager.onEvent("notice-debugger", ChannelNoticeEvent::class.java) {
            log.debug("Received notice (${it.msgId}): ${it.message}")
        }
    }

    internal fun hasValidToken() = credential != null && credential.userId.isNullOrEmpty().not()

    internal fun sendTwitchMessage(message: String, dropCommands: Boolean = true, nonce: String? = null, replyMsgId: String? = null, channelName: String = config!!.twitchChannelName) {
        if (dropCommands && message.startsWith('/')) return
        twitchClient!!.chat.sendMessage(channelName, message, nonce, replyMsgId)
    }
}
