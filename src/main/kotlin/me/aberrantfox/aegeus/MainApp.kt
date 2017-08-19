package me.aberrantfox.aegeus

import me.aberrantfox.aegeus.services.loadConfig
import me.aberrantfox.aegeus.commandframework.produceCommandMap
import me.aberrantfox.aegeus.commandframework.util.hasRole
import me.aberrantfox.aegeus.listeners.*
import me.aberrantfox.aegeus.services.setupDatabaseSchema
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import java.util.*


fun main(args: Array<String>) {
    println("Starting to load Aegeus bot.")

    val commandMap = produceCommandMap()
    val config = loadConfig(commandMap)

    if (config == null) {
        println("""The default configuration has been generated."
                   Please fill in this configuration in order to use the bot.""")
        System.exit(0)
        return
    }

    setupDatabaseSchema(config)

    val jda = JDABuilder(AccountType.BOT).setToken(config.token).buildBlocking()
    jda.addEventListener(
            CommandListener(config, commandMap),
            MemberListener(config),
            InviteListener(config),
            ResponseListener(config))

    jda.presence.setPresence(OnlineStatus.ONLINE, Game.of("${config.prefix}help"))
    jda.guilds.forEach { setupMutedRole(it, config.mutedRole) }
}

private fun setupMutedRole(guild: Guild, roleName: String) {
    if(!guild.hasRole(roleName)) {
        guild.controller.createRole().setName(roleName).queue {
            handleRole(guild, roleName)
        }
        return
    }

    handleRole(guild, roleName)
}

private fun handleRole(guild: Guild, roleName: String) {
    val role = guild.getRolesByName(roleName, true).first()

    guild.textChannels.forEach {
        val hasOverride = it.permissionOverrides.any {
            it.role.name.toLowerCase() == roleName
        }

        if(!hasOverride) it.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue()
    }
}