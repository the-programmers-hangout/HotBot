package me.aberrantfox.hotbot.services

import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.discord.Discord
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import me.aberrantfox.kjdautils.internal.logging.convertChannels
import java.util.*
import kotlin.concurrent.schedule

@Service
class LoggingService(configuration: Configuration, discord: Discord) {
    lateinit var logInstance: BotLogger
    init {
        Timer("SettingUp", false).schedule(1000) {
            println(discord.jda.guilds.size)
            logInstance = convertChannels(configuration.logChannels, discord.jda)
        }
    }

}