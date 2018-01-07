package me.aberrantfox.aegeus

import me.aberrantfox.aegeus.commandframework.produceContainer
import me.aberrantfox.aegeus.extensions.hasRole
import me.aberrantfox.aegeus.extensions.timeToDifference
import me.aberrantfox.aegeus.extensions.unmute
import me.aberrantfox.aegeus.listeners.*
import me.aberrantfox.aegeus.listeners.antispam.DuplicateMessageListener
import me.aberrantfox.aegeus.listeners.antispam.InviteListener
import me.aberrantfox.aegeus.logging.convertChannels
import me.aberrantfox.aegeus.services.*
import me.aberrantfox.aegeus.services.database.setupDatabaseSchema
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild


fun main(args: Array<String>) {
    println("Starting to load hotbot.")
    val container = produceContainer()
    val config = loadConfig(container) ?: return


    saveConfig(config)
    setupDatabaseSchema(config)

    val jda = JDABuilder(AccountType.BOT).setToken(config.token).buildBlocking()
    val logChannel = jda.getTextChannelById(config.logChannel)
    val mutedRole = jda.getRolesByName(config.mutedRole, true).first()
    val tracker = MessageTracker(1)
    val guild = jda.getGuildById(config.guildid)
    container.newLogger(convertChannels(config.logChannels, jda))

    jda.addEventListener(
            CommandListener(config, container, jda, logChannel, guild),
            MemberListener(config),
            InviteListener(config),
            MentionListener(config, jda.selfUser.name),
            VoiceChannelListener(logChannel),
            NewChannelListener(mutedRole),
            DuplicateMessageListener(config, logChannel, tracker),
            RoleListener(config))

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
            it.role.name.toLowerCase() == roleName.toLowerCase()
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
