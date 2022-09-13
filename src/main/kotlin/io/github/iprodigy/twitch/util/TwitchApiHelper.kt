package io.github.iprodigy.twitch.util

import com.github.twitch4j.ITwitchClient
import com.github.twitch4j.chat.util.TwitchChatLimitHelper
import com.github.twitch4j.graphql.internal.type.CreatePollChoiceInput
import com.github.twitch4j.graphql.internal.type.CreatePollInput
import io.github.bucket4j.Bandwidth
import io.github.iprodigy.twitch.Bot

private const val DEFAULT_POLL_DURATION = 60
private const val DEFAULT_POLL_POINTS = 1000
const val TWITCH_MAX_MESSAGE_LENGTH = 500

fun chatRateLimit(modded: Boolean): Bandwidth = if (modded) TwitchChatLimitHelper.MOD_MESSAGE_LIMIT else TwitchChatLimitHelper.USER_MESSAGE_LIMIT

fun createPoll(client: ITwitchClient = Bot.twitchClient!!, channelId: String? = Bot.channelId, title: String, choices: List<String> = listOf("Yes (1)", "No (2)")) {
    if (channelId == null) return
    client.graphQL.createPoll(
        null,
        CreatePollInput.builder()
            .title(title)
            .choices(choices.map { CreatePollChoiceInput.builder().title(it).build() })
            .durationSeconds(DEFAULT_POLL_DURATION)
            .ownedBy(channelId)
            .isCommunityPointsVotingEnabled(true)
            .communityPointsCost(DEFAULT_POLL_POINTS)
            .build()
    ).executeOrNull()
}
