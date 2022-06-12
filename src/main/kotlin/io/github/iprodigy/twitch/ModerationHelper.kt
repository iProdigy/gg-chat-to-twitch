package io.github.iprodigy.twitch

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.twitch4j.chat.TwitchChatBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import io.github.iprodigy.twitch.util.ConcurrentBoundedDeque
import io.github.iprodigy.twitch.util.drain
import java.util.*
import java.util.concurrent.TimeUnit

object ModerationHelper {
    private val recentMessageIdsByName: Cache<String, Deque<String>> = Caffeine.newBuilder()
        .expireAfterAccess(120, TimeUnit.SECONDS)
        .build()

    private val readConnection = TwitchChatBuilder.builder().build().apply {
        eventManager.onEvent(ChannelMessageEvent::class.java) { e ->
            if (e.user.id == Bot.credential?.userId) {
                e.messageEvent.nonce.ifPresent { nonce ->
                    val msgId = e.messageEvent.messageId.orElse(null) ?: return@ifPresent
                    val delimIndex = nonce.indexOf(':').takeIf { it >= 0 } ?: return@ifPresent
                    val name = nonce.substring(0, delimIndex).lowercase().trim()
                    recentMessageIdsByName.get(name) { ConcurrentBoundedDeque(16) }!!.offerFirst(msgId)
                }
            }
        }
    }

    fun start(channelName: String) = readConnection.joinChannel(channelName)

    fun drainRecentMessageIds(name: String) = recentMessageIdsByName.getIfPresent(name.lowercase())?.drain()
}
