package io.github.iprodigy.twitch.config

import io.github.iprodigy.twitch.Bot
import io.github.iprodigy.twitch.mapper
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.toPath
import kotlin.io.path.writeText

private const val CONFIG_FILE_NAME = "./config.json"

object ConfigManager {
    private val log = LoggerFactory.getLogger(javaClass)!!

    val config = readConfig()

    fun hasValidConfig() = config != null && config.accessToken.isNotBlank() && config.chatSocketUrl.isNotBlank() && config.twitchChannelName.isNotBlank()

    @Synchronized
    private fun readConfig(): ConfigSettings? = try {
        val cfg = getConfigResource()
        if (cfg != null) {
            cfg.readText().let {
                log.trace("Read config contents: $it")
                mapper.readValue(it, ConfigSettings::class.java)
            }
        } else {
            log.warn("Failed to locate config")
            null
        }
    } catch (e: Exception) {
        log.error("Failed to read or parse config", e)
        null
    }

    @Synchronized
    internal fun writeConfig() = try {
        getConfigResource()?.apply {
            writeText(mapper.writeValueAsString(Bot.config))
            log.debug("Successfully wrote latest config file")
        }
    } catch (e: Exception) {
        log.error("Failed to write config", e)
    }

    private fun getConfigResource() = try {
        this::class.java.classLoader.getResource(CONFIG_FILE_NAME)?.toURI()?.toPath() ?: Paths.get(CONFIG_FILE_NAME)
    } catch (e: Exception) {
        log.error("Failed to obtain config", e)
        null
    }
}
