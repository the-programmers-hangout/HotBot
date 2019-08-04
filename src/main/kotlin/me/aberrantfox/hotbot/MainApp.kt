package me.aberrantfox.hotbot

import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.api.startBot
import me.aberrantfox.kjdautils.internal.logging.convertChannels

fun main() {
    val config = loadConfig() ?: return
    saveConfig(config)
    start(config)
}

private fun start(config: Configuration) = startBot(config.serverInformation.token) {
    setupDatabaseSchema(config)

    this.logger = convertChannels(config.logChannels, this.discord.jda)

    registerInjectionObject(config, container, logger, this.config)

    configure {
        registerInjectionObject(this)
        prefix = config.serverInformation.prefix
        globalPath = "me.aberrantfox.hotbot"
        deleteMode = config.serverInformation.deletionMode
    }

    AliasService.instance.loadAliases()
}
