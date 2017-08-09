package me.aberrantfox.aegeus

import me.aberrantfox.aegeus.services.loadConfig
import me.aberrantfox.aegeus.commandframework.produceCommandMap
import me.aberrantfox.aegeus.listeners.CommandListener
import me.aberrantfox.aegeus.listeners.InviteListener
import me.aberrantfox.aegeus.listeners.MemberListener
import me.aberrantfox.aegeus.listeners.MuteListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder


fun main(args: Array<String>) {
    println("Starting to load Aegeus bot.")

    val commandMap = produceCommandMap()
    val config = loadConfig(commandMap)

    if(config == null) {
        println("""The default configuration has been generated."
                   Please fill in this configuration in order to use the bot.""")
        System.exit(0)
        return
    }

    val jda  = JDABuilder(AccountType.BOT).setToken(config.token).buildBlocking()
    jda.addEventListener(
            CommandListener(config, commandMap),
            MuteListener(config),
            MemberListener(config),
            InviteListener(config))
}