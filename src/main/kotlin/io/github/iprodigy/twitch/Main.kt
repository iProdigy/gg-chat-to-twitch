package io.github.iprodigy.twitch

fun main() {
    if (Bot.hasValidConfig().not()) {
        Bot.log.error("Exiting due to invalid configuration.")
        return
    }

    Bot.start()
}
