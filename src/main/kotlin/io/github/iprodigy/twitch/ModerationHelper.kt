package io.github.iprodigy.twitch

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.twitch4j.chat.TwitchChatBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import io.github.iprodigy.twitch.util.ConcurrentBoundedDeque
import io.github.iprodigy.twitch.util.DrainableDeque
import java.util.concurrent.TimeUnit

private const val CACHE_SECONDS = 120L
private const val RECENT_MESSAGE_LIMIT = 16

object ModerationHelper {
    private val recentMessageIdsByName: Cache<String, DrainableDeque<String>> = Caffeine.newBuilder()
        .expireAfterAccess(CACHE_SECONDS, TimeUnit.SECONDS)
        .build()

    private val readConnection = TwitchChatBuilder.builder().build().apply {
        eventManager.onEvent(ChannelMessageEvent::class.java) { e ->
            if (e.user.id == Bot.credential?.userId) {
                e.messageEvent.nonce.ifPresent { nonce ->
                    val msgId = e.messageEvent.messageId.orElse(null) ?: return@ifPresent
                    val delimIndex = nonce.indexOf(':').takeIf { it >= 0 } ?: return@ifPresent
                    val name = nonce.substring(0, delimIndex).lowercase().trim()
                    recentMessageIdsByName.get(name) { ConcurrentBoundedDeque(RECENT_MESSAGE_LIMIT) }!!.offerFirst(msgId)
                }
            }
        }
    }

    fun start(channelName: String) = readConnection.joinChannel(channelName)

    fun purge(name: String) {
        val msgIds = drainRecentMessageIds(name)?.takeIf { it.isNotEmpty() }
        if (msgIds != null && Bot.config!!.twitchMod) {
            msgIds.forEach {
                Bot.sendTwitchMessage("/delete $it", dropCommands = false)
            }
        }
    }

    private fun drainRecentMessageIds(name: String) = recentMessageIdsByName.getIfPresent(name.lowercase())?.drain()
}
