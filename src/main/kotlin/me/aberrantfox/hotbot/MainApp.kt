package me.aberrantfox.hotbot

import me.aberrantfox.hotbot.commands.development.EngineContainer
import me.aberrantfox.hotbot.commands.development.EngineContainer.setupScriptEngine
import me.aberrantfox.hotbot.commands.utility.scheduleReminder
import me.aberrantfox.hotbot.database.forEachReminder
import me.aberrantfox.hotbot.database.getAllMutedMembers
import me.aberrantfox.hotbot.database.setupDatabaseSchema
import me.aberrantfox.hotbot.listeners.*
import me.aberrantfox.hotbot.listeners.antispam.DuplicateMessageListener
import me.aberrantfox.hotbot.listeners.antispam.InviteListener
import me.aberrantfox.hotbot.listeners.antispam.NewJoinListener
import me.aberrantfox.hotbot.listeners.antispam.TooManyMentionsListener
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.scheduleUnmute
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.Fail
import me.aberrantfox.kjdautils.api.Pass
import me.aberrantfox.kjdautils.api.PreconditionResult
import me.aberrantfox.kjdautils.api.startBot
import me.aberrantfox.kjdautils.extensions.jda.containsInvite
import me.aberrantfox.kjdautils.extensions.jda.containsURL
import me.aberrantfox.kjdautils.extensions.jda.mentionsSomeone
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import me.aberrantfox.kjdautils.internal.logging.convertChannels
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import org.apache.log4j.*

const val commandPath = "me.aberrantfox.hotbot.commands"

fun main(args: Array<String>) {

    val config = loadConfig() ?: return
    saveConfig(config)

    startBot(config.serverInformation.token) {
        println("Starting to load hotbot.")



        setupLogger()
        setupDatabaseSchema(config)

        logger = convertChannels(config.logChannels, jda)

        val messageService = MService()
        val manager = PermissionManager(jda, config)

        registerInjectionObject(messageService, config, logger, manager)



        val container = registerCommands(commandPath, config.serverInformation.prefix)

        manager.setDefaultPermissions(container)

        val failsBecause: (String?, Boolean) -> PreconditionResult = { reason, condition -> if (condition) Pass else Fail(reason) }

        registerCommandPreconditions(
                { failsBecause("Only the owner can invoke commands in lockdown mode", !config.security.lockDownMode || it.author.id == config.serverInformation.ownerID) },
                { failsBecause(null, !config.security.ignoredIDs.contains(it.channel.id) && !config.security.ignoredIDs.contains(it.author.id)) },
                { failsBecause("You do not have the required permissions to use a command mention", manager.canPerformAction(it.author, config.permissionedActions.commandMention) || !it.message.mentionsSomeone()) },
                { failsBecause("You do not have the required permissions to send an invite.", manager.canPerformAction(it.author, config.permissionedActions.sendInvite) || !it.message.containsInvite()) },
                { failsBecause("You do not have the required permissions to send URLs", manager.canPerformAction(it.author, config.permissionedActions.sendURL) || !it.message.containsURL()) },
                { failsBecause("Did you really think I would let you do that? :thinking:", manager.canUseCommand(it.author, it.command.name)) }
        )



        val helpErrors = HelpConf.getDocumentationErrors(container)

        if (helpErrors.isNotEmpty()) {
            println("The help documentation needs to be updated:")
            helpErrors.forEach(::println)

            if (!config.botInformation.developmentMode) {
                return@startBot
            }
        }


        logger.info("connected")


        jda.guilds.forEach { setupMutedRole(it, config.security.mutedRole) }

        val mutedRole = jda.getRolesByName(config.security.mutedRole, true).first()

        handleLTSMutes(config, jda)



        val tracker = MessageTracker(1)

        registerListeners(
                MemberListener(config, logger, messageService),
                InviteListener(config, logger, manager),
                VoiceChannelListener(logger),
                NewChannelListener(mutedRole),
                ChannelDeleteListener(logger),
                DuplicateMessageListener(config, logger, tracker),
                RoleListener(config),
                PollListener(),
                BanListener(config),
                TooManyMentionsListener(logger, mutedRole),
                MessageDeleteListener(logger, manager, config),
                NewJoinListener()
        )

        if (config.apiConfiguration.enableCleverBot) {
            println("Enabling cleverbot integration.")
            jda.addEventListener(MentionListener(config, jda.selfUser.name))
        }

        EngineContainer.engine = setupScriptEngine(jda, container, config, logger)

        logger.info("Fully setup, now ready for use.")
    }
}

private fun setupLogger() {
    val console = ConsoleAppender()
    val pattern = "%d [%p|%c|%C{1}] %m%n"
    console.layout = PatternLayout(pattern)
    console.threshold = Level.INFO
    console.activateOptions()

    Logger.getRootLogger().addAppender(console)

    val fa = FileAppender()
    fa.name = "FileLogger"
    fa.file = "hotbot.log"
    fa.layout = PatternLayout("%d %-5p [%c{1}] %m%n")
    fa.threshold = Level.DEBUG
    fa.append = true
    fa.activateOptions()

    Logger.getRootLogger().addAppender(fa)
}

private fun setupMutedRole(guild: Guild, roleName: String) {
    val possibleRole = guild.getRolesByName(roleName, true).firstOrNull()
    val mutedRole = possibleRole ?: guild.controller.createRole().setName(roleName).complete()

    guild.textChannels
            .filter {
                it.rolePermissionOverrides.none {
                    it.role.name.toLowerCase() == roleName.toLowerCase()
                }
            }
            .forEach {
                it.createPermissionOverride(mutedRole).setDeny(Permission.MESSAGE_WRITE).queue()
            }
}


private fun handleLTSMutes(config: Configuration, jda: JDA) {
    getAllMutedMembers().forEach {
        val difference = timeToDifference(it.unmuteTime)
        val guild = jda.getGuildById(it.guildId)
        val user = guild.getMemberById(it.user)

        if (user != null) {
            scheduleUnmute(guild, user.user, config, difference, it)
        }
    }
}

private fun loadReminders(jda: JDA, log: BotLogger) {
    forEachReminder {
        val difference = timeToDifference(it.remindTime)
        val user = jda.getUserById(it.member)

        scheduleReminder(user, it.message, difference, log)
    }
}
