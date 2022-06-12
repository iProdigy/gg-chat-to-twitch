package io.github.iprodigy.twitch

import com.github.twitch4j.client.websocket.WebsocketConnection
import com.github.twitch4j.common.util.CryptoUtils
import io.github.iprodigy.twitch.util.TWITCH_MAX_MESSAGE_LENGTH
import io.github.iprodigy.twitch.util.createPoll
import io.github.iprodigy.twitch.util.pronounsById
import java.util.Collections
import java.util.concurrent.ScheduledExecutorService

private const val NONCE_LENGTH = 6
const val WSS_PING_PERIOD = 30_000

class SiteChatConnection(
    private val baseUrl: String,
    private val executor: ScheduledExecutorService,
    private val pingPeriod: Int = WSS_PING_PERIOD
) {
    private val socket = WebsocketConnection {
        it.baseUrl(baseUrl)
        it.taskExecutor(executor)
        it.wsPingPeriod(pingPeriod)
        it.onTextMessage(this::parseSocketMessage)
    }

    fun connect() = socket.connect()
    fun disconnect() = socket.disconnect()
    fun reconnect() = socket.reconnect()

    private fun parseSocketMessage(msg: String) {
        val space = msg.indexOf(' ')
        if (space < 0 || (space + 1) >= msg.length) return

        val typeStr = msg.substring(0, space)
        when (val type = messageTypesByName[typeStr] ?: MessageType.UNKNOWN) {
            MessageType.MSG, MessageType.BROADCAST, MessageType.BAN, MessageType.MUTE -> {
                val json = msg.substring(space + 1)
                executor.execute {
                    val parsed = try {
                        mapper.readValue(json, SocketChatMessage::class.java)
                    } catch (e: Exception) {
                        Bot.log.warn("Failed to parse socket message: $msg", e)
                        null
                    }

                    if (parsed != null) {
                        Bot.log.trace("Received message: $json")

                        when (type) {
                            MessageType.MSG -> handleMessage(parsed)
                            MessageType.BROADCAST -> handleBroadcast(parsed)
                            MessageType.BAN, MessageType.MUTE -> handlePurge(parsed)
                            else -> Bot.log.trace("Dropping message: $msg")
                        }
                    }
                }
            }
            MessageType.UNKNOWN -> Bot.log.debug("Unknown message type: $msg")
            else -> Bot.log.trace("Ignoring message: $msg")
        }
    }

    private fun handlePurge(message: SocketChatMessage) {
        message.data.takeIf { it.isNotEmpty() }?.run {
            ModerationHelper.purge(this)
        }
    }

    private fun handleBroadcast(message: SocketChatMessage) {
        if (Bot.config!!.mirrorBroadcasts && message.data.startsWith('/').not()) {
            Bot.sendTwitchMessage("/me " + message.data, dropCommands = false)
        }
    }

    private fun handleMessage(message: SocketChatMessage) {
        if (message.nick == null) return
        if (Bot.config!!.ignoreBots && message.isBot()) return
        if (Bot.config.subsOnly && message.isPrivileged().not()) return

        if (message.data.startsWith('/') || message.data.startsWith('!')) {
            this.handleChatCommand(message)
        } else {
            this.handleChatUserMessage(message)
        }
    }

    private fun handleChatCommand(message: SocketChatMessage) {
        if (message.isMod() || message.isAdmin()) {
            if (Bot.config!!.shouldMirrorPolls() && message.data.startsWith("/vote ") && message.data.endsWith('?')) {
                createPoll(title = message.data.substring("/vote ".length).trim())
            }
        }
    }

    private fun handleChatUserMessage(message: SocketChatMessage) {
        val anyFeaturesRequired = Bot.config!!.anyFeaturesRequired
        if (!anyFeaturesRequired.isNullOrEmpty() && (message.features.isNullOrEmpty() || Collections.disjoint(anyFeaturesRequired, message.features))) return

        val pronouns = if (Bot.config.includePronouns) message.pronouns?.let { pronounsById[it] }?.let { " ($it)" } ?: "" else ""
        val msg = "${Bot.config.twitchMessagePrefix} ${message.nick}$pronouns: ${message.data}".trim()
            .take(TWITCH_MAX_MESSAGE_LENGTH - Bot.config.twitchMessagePostfix.length) + Bot.config.twitchMessagePostfix
        Bot.sendTwitchMessage(msg, nonce = "${message.nick}:${message.timestamp ?: CryptoUtils.generateNonce(NONCE_LENGTH)}")
    }
}
