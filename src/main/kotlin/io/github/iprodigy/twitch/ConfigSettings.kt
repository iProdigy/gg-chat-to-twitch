package io.github.iprodigy.twitch

data class ConfigSettings(
    val clientId: String? = null,
    val clientSecret: String? = null,
    var accessToken: String,
    var refreshToken: String? = null,
    val firstPartyToken: String? = null,
    val chatSocketUrl: String,
    val twitchChannelName: String,
    var twitchMod: Boolean = false,
    val subsOnly: Boolean = false,
    val ignoreBots: Boolean = true,
    val includePronouns: Boolean = true,
    val mirrorBroadcasts: Boolean = true,
    val mirrorPolls: Boolean = true,
    val twitchMessagePrefix: String = "[GGchat]"
) {
    fun shouldMirrorPolls() = mirrorPolls && firstPartyToken.isNullOrBlank().not()
}
