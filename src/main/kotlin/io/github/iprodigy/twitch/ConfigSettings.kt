package io.github.iprodigy.twitch

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.iprodigy.twitch.util.ensureSet

private const val FEATURE_DELIM = ';'

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
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
    private var requireAnyFeatures: String? = null
) {
    @JsonIgnore
    var anyFeaturesRequired: Collection<String> = requireAnyFeatures?.split(FEATURE_DELIM)?.toSet() ?: emptyList()
        set(value) {
            field = value.ensureSet()
            requireAnyFeatures = field.takeIf { it.isNotEmpty() }?.joinToString(FEATURE_DELIM.toString())
        }

    @JsonIgnore
    fun shouldMirrorPolls() = mirrorPolls && firstPartyToken.isNullOrBlank().not()
}
