package io.github.iprodigy.twitch

import com.github.twitch4j.chat.TwitchChatBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import io.github.iprodigy.twitch.util.ConcurrentBoundedDeque
import io.github.iprodigy.twitch.util.DrainableDeque
import io.github.xanthic.cache.api.Cache
import io.github.xanthic.cache.api.domain.ExpiryType
import io.github.xanthic.cache.ktx.createCache
import io.github.xanthic.cache.ktx.expiryTime
import io.github.xanthic.cache.ktx.expiryType
import io.github.xanthic.cache.ktx.maxSize
import java.time.Duration

private const val CACHE_SECONDS = 120L
private const val CACHE_CAPACITY = 65536L
private const val RECENT_MESSAGE_LIMIT = 16

object ModerationHelper {
    private val recentMessageIdsByName: Cache<String, DrainableDeque<String>> = createCache {
        expiryType = ExpiryType.POST_ACCESS
        expiryTime = Duration.ofSeconds(CACHE_SECONDS)
        maxSize = CACHE_CAPACITY
    }

    private val readConnection = TwitchChatBuilder.builder().build().apply {
        eventManager.onEvent(ChannelMessageEvent::class.java) { e ->
            if (e.user.id == Bot.credential?.userId) {
                e.messageEvent.nonce.ifPresent { nonce ->
                    val msgId = e.messageEvent.messageId.orElse(null) ?: return@ifPresent
                    val delimIndex = nonce.indexOf(':').takeIf { it >= 0 } ?: return@ifPresent
                    val name = nonce.substring(0, delimIndex).lowercase().trim()
                    recentMessageIdsByName.computeIfAbsent(name) { ConcurrentBoundedDeque(RECENT_MESSAGE_LIMIT) }!!.offerFirst(msgId)
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

    private fun drainRecentMessageIds(name: String) = recentMessageIdsByName[name.lowercase()]?.drain()
}
