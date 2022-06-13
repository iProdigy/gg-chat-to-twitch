package io.github.iprodigy.twitch.config

data class ConfigSettings(
    val clientId: String? = null,
    val clientSecret: String? = null,
    var accessToken: String,
    var refreshToken: String? = null,
    val firstPartyToken: String? = null,
    val chatSocketUrl: String,
    val twitchChannelName: String,
    var twitchMod: Boolean = false,
    var subsOnly: Boolean = false,
    var ignoreBots: Boolean = true,
    var includePronouns: Boolean = true,
    var mirrorBroadcasts: Boolean = true,
    var mirrorPolls: Boolean = true,
    var twitchMessagePrefix: String = "[GGchat]",
    var twitchMessagePostfix: String = "",
    val commandTrigger: String = "-",
    val anyFeaturesRequired: MutableSet<String>? = null
) {
    fun shouldMirrorPolls() = mirrorPolls && firstPartyToken.isNullOrBlank().not()
}
