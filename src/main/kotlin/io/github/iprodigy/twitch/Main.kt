package io.github.iprodigy.twitch

fun main() {
    if (Bot.hasValidConfig().not()) {
        println("Invalid configuration")
        return
    }

    Bot.start()
}
