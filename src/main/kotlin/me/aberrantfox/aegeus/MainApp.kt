package me.aberrantfox.aegeus

import me.aberrantfox.aegeus.businessobjects.produceConfigOrFail
import me.aberrantfox.aegeus.commandframework.produceCommandMap
import me.aberrantfox.aegeus.listeners.CommandListener
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder


fun main(args: Array<String>) {
    println("Starting to load Aegeus bot.")

    val commandMap = produceCommandMap()
    val config = produceConfigOrFail(commandMap)
    val jda  = JDABuilder(AccountType.BOT).setToken(config.token).buildBlocking()

    jda.addEventListener(CommandListener(jda, config, commandMap))
}