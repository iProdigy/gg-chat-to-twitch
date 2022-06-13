package io.github.iprodigy.twitch

import io.github.iprodigy.twitch.config.ConfigManager

fun main() {
    if (!ConfigManager.hasValidConfig() || !Bot.hasValidToken()) {
        Bot.log.error("Exiting due to invalid configuration.")
        return
    }

    Bot.start()
}
