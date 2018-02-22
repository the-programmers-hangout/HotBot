package me.aberrantfox.hotbot

import me.aberrantfox.hotbot.commandframework.produceContainer
import me.aberrantfox.hotbot.database.getAllMutedMembers
import me.aberrantfox.hotbot.database.loadUpManager
import me.aberrantfox.hotbot.extensions.hasRole
import me.aberrantfox.hotbot.extensions.timeToDifference
import me.aberrantfox.hotbot.extensions.scheduleUnmute
import me.aberrantfox.hotbot.listeners.*
import me.aberrantfox.hotbot.listeners.antispam.DuplicateMessageListener
import me.aberrantfox.hotbot.listeners.antispam.InviteListener
import me.aberrantfox.hotbot.logging.convertChannels
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.database.setupDatabaseSchema
import me.aberrantfox.hotbot.listeners.antispam.NewJoinListener
import me.aberrantfox.hotbot.listeners.antispam.TooManyMentionsListener
import me.aberrantfox.hotbot.permissions.PermissionManager
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild


fun main(args: Array<String>) {
    println("Starting to load hotbot.")
    val container = produceContainer()
    val config = loadConfig() ?: return

    saveConfig(config)

    val helpErrors = HelpConf.getDocumentationErrors(container)
    if (helpErrors.isNotEmpty()) {
        println("The help documentation needs to be updated:")
        helpErrors.forEach(::println)

        if( !(config.botInformation.developmentMode) ) {
            return
        }
    }

    setupDatabaseSchema(config)

    val jda = JDABuilder(AccountType.BOT).setToken(config.serverInformation.token).buildBlocking()
    val logger = convertChannels(config.logChannels, jda)

    jda.guilds.forEach { setupMutedRole(it, config.security.mutedRole) }

    logger.info("connected")
    val mutedRole = jda.getRolesByName(config.security.mutedRole, true).first()
    val tracker = MessageTracker(1)
    val guild = jda.getGuildById(config.serverInformation.guildid)
    val manager = PermissionManager(HashMap(), guild.roles, guild, config)
    val messageService = MService()

    loadUpManager(manager)

    container.newLogger(logger)

    jda.addEventListener(
            CommandListener(config, container, jda, logger, guild, manager, messageService),
            MemberListener(config, logger, messageService),
            InviteListener(config, logger, manager),
            VoiceChannelListener(logger),
            NewChannelListener(mutedRole),
            DuplicateMessageListener(config, logger, tracker),
            RoleListener(config),
            PollListener(),
            BanListener(config),
            TooManyMentionsListener(logger, mutedRole),
            MessageDeleteListener(logger, manager, config),
            NewJoinListener())

    if(config.apiConfiguration.enableCleverBot) {
        println("Enabling cleverbot integration.")
        jda.addEventListener(MentionListener(config, jda.selfUser.name))
    }

    jda.presence.setPresence(OnlineStatus.ONLINE, Game.of("${config.serverInformation.prefix}help"))


    handleLTSMutes(config, jda)
    logger.info("Fully setup, now ready for use.")
}

private fun setupMutedRole(guild: Guild, roleName: String) {
    if (!guild.hasRole(roleName)) guild.controller.createRole().setName(roleName).complete()

    handleRole(guild, roleName)
}

private fun handleRole(guild: Guild, roleName: String) {
    val role = guild.getRolesByName(roleName, true).first()

    guild.textChannels.forEach {
        val hasOverride = it.rolePermissionOverrides.any {
            it.role.name.toLowerCase() == roleName.toLowerCase()
        }

        if (!hasOverride) it.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue()
    }
}

private fun handleLTSMutes(config: Configuration, jda: JDA) {
    getAllMutedMembers().forEach {
        val difference = timeToDifference(it.unmuteTime)
        val guild = jda.getGuildById(it.guildId)
        val user = guild.getMemberById(it.user)

        if(user != null) {
            scheduleUnmute(guild, user.user, config, difference, it)
        }
    }
}
