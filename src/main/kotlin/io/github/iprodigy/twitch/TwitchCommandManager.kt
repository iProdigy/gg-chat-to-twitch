package io.github.iprodigy.twitch

import com.github.twitch4j.chat.events.CommandEvent
import com.github.twitch4j.common.enums.CommandPermission

object TwitchCommandManager {
    private val commandHandlers: MutableMap<String, (CommandEvent) -> Unit> = hashMapOf()

    fun accept(event: CommandEvent) {
        if (CommandPermission.MODERATOR in event.permissions || CommandPermission.BROADCASTER in event.permissions) {
            val cmd = event.command.lowercase().let {
                val space = it.indexOf(' ')
                if (space < 0) it else it.substring(0, space)
            }
            commandHandlers[cmd]?.invoke(event)
        }
    }

    init {
        commandHandlers["connect"] = { Bot.socketConnection.connect() }
        commandHandlers["disconnect"] = { Bot.socketConnection.disconnect() }
        commandHandlers["reconnect"] = { Bot.socketConnection.reconnect() }
        commandHandlers["save"] = { Bot.writeConfig() }
        commandHandlers["setprefix"] = { Bot.config!!.twitchMessagePrefix = it.command.substring("setprefix".length) }
        commandHandlers["setpostfix"] = { Bot.config!!.twitchMessagePostfix = it.command.substring("setpostfix".length) }

        fun registerBooleanConfigCommands(label: String, getter: () -> Boolean, setter: (Boolean) -> Unit) {
            commandHandlers["enable" + label.lowercase()] = { setter(true) }
            commandHandlers["disable" + label.lowercase()] = { setter(false) }
            commandHandlers["toggle" + label.lowercase()] = { setter(getter().not()) }
        }

        registerBooleanConfigCommands("subsOnly", { Bot.config!!.subsOnly }, { Bot.config!!.subsOnly = it })
        registerBooleanConfigCommands("bots", { Bot.config!!.ignoreBots.not() }, { Bot.config!!.ignoreBots = !it })
        registerBooleanConfigCommands("pronouns", { Bot.config!!.includePronouns }, { Bot.config!!.includePronouns = it })
        registerBooleanConfigCommands("broadcasts", { Bot.config!!.mirrorBroadcasts }, { Bot.config!!.mirrorBroadcasts = it })
        registerBooleanConfigCommands("polls", { Bot.config!!.mirrorPolls }, { Bot.config!!.mirrorPolls = it })

        commandHandlers["purge"] = { e ->
            val name = e.command.substring("purge".length).trim().takeIf { it.isNotEmpty() }
            val msgIds = name?.let { ModerationHelper.drainRecentMessageIds(it) }?.takeIf { it.isNotEmpty() }
            if (msgIds != null && Bot.config!!.twitchMod) {
                msgIds.forEach {
                    Bot.sendTwitchMessage("/delete $it", dropCommands = false)
                }
            }
        }
    }
}
