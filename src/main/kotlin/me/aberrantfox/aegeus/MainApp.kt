package me.aberrantfox.aegeus

import me.aberrantfox.aegeus.services.loadConfig
import me.aberrantfox.aegeus.commandframework.produceCommandMap
import me.aberrantfox.aegeus.commandframework.util.hasRole
import me.aberrantfox.aegeus.commandframework.util.timeToDifference
import me.aberrantfox.aegeus.commandframework.util.unmute
import me.aberrantfox.aegeus.listeners.*
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.database.setupDatabaseSchema
import me.aberrantfox.aegeus.services.saveConfig
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild


fun main(args: Array<String>) {
    println("Starting to load Aegeus bot.")

    val commandMap = produceCommandMap()
    val config = loadConfig(commandMap) ?: return

    saveConfig(config)
    setupDatabaseSchema(config)

    val jda = JDABuilder(AccountType.BOT).setToken(config.token).buildBlocking()
    val logChannel = jda.getTextChannelById(config.logChannel)

    jda.addEventListener(
            CommandListener(config, commandMap, jda, logChannel),
            MemberListener(config),
            InviteListener(config),
            ResponseListener(config),
            MentionListener(),
            VoiceChannelListener(logChannel))

    jda.presence.setPresence(OnlineStatus.ONLINE, Game.of("${config.prefix}help"))
    jda.guilds.forEach { setupMutedRole(it, config.mutedRole) }

    handleLTSMutes(config, jda)
}

private fun setupMutedRole(guild: Guild, roleName: String) {
    if (!guild.hasRole(roleName)) {
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

        if (!hasOverride) it.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue()
    }
}

private fun handleLTSMutes(config: Configuration, jda: JDA) {
    config.mutedMembers.forEach {
        val difference = timeToDifference(it.unmuteTime)
        val guild = jda.getGuildById(it.guildId)
        val user = guild.getMemberById(it.user)

        if(user != null) {
            unmute(guild, user.user, config, difference, it)
        }
    }
}
