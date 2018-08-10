package me.aberrantfox.hotbot

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.commands.development.EngineContainer
import me.aberrantfox.hotbot.commands.development.EngineContainer.setupScriptEngine
import me.aberrantfox.hotbot.commands.utility.canUseMacro
import me.aberrantfox.hotbot.commands.utility.macros
import me.aberrantfox.hotbot.commands.utility.scheduleReminder
import me.aberrantfox.hotbot.commands.utility.setupMacroCommands
import me.aberrantfox.hotbot.database.forEachIgnoredID
import me.aberrantfox.hotbot.database.forEachReminder
import me.aberrantfox.hotbot.database.setupDatabaseSchema
import me.aberrantfox.hotbot.optionallisteners.MentionListener
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.startBot
import me.aberrantfox.kjdautils.extensions.jda.containsInvite
import me.aberrantfox.kjdautils.extensions.jda.containsURL
import me.aberrantfox.kjdautils.extensions.jda.mentionsSomeone
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass
import me.aberrantfox.kjdautils.internal.command.PreconditionResult
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import me.aberrantfox.kjdautils.internal.logging.convertChannels
import net.dv8tion.jda.core.JDA

const val commandPath = "me.aberrantfox.hotbot.commands"

fun main(args: Array<String>) {
    val config = loadConfig() ?: return
    saveConfig(config)
    start(config)
}

private fun start(config: Configuration) = startBot(config.serverInformation.token) {
    setupDatabaseSchema(config)

    logger = convertChannels(config.logChannels, jda)

    val messageService = MService()
    val manager = PermissionManager(jda, config)

    registerInjectionObject(messageService, config, logger, manager, this.config)
    val container = registerCommands(commandPath, config.serverInformation.prefix)
    LowerUserArg.manager = manager

    setupMacroCommands(container, manager)

    val failsBecause: (String?, Boolean) -> PreconditionResult = { reason, condition -> if (condition) Pass else Fail(reason) }
    val commandName: (CommandEvent) -> String = { it.commandStruct.commandName.toLowerCase() }

    registerCommandPreconditions(
            { failsBecause(null, !config.security.ignoredIDs.contains(it.channel.id) && !config.security.ignoredIDs.contains(it.author.id)) },
            { failsBecause(null, manager.canUseCommandInChannel(it.author, it.channel.id)) },
            { failsBecause(null, commandName(it) !in macros || canUseMacro(macros[commandName(it)]!!, it.channel, config.serverInformation.macroDelay)) },
            { failsBecause("Only the owner can invoke commands in lockdown mode", !config.security.lockDownMode || it.author.id == config.serverInformation.ownerID) },
            { failsBecause("You do not have the required permissions to use a command mention", manager.canPerformAction(it.author, config.permissionedActions.commandMention) || !it.message.mentionsSomeone()) },
            { failsBecause("You do not have the required permissions to send an invite.", manager.canPerformAction(it.author, config.permissionedActions.sendInvite) || !it.message.containsInvite()) },
            { failsBecause("You do not have the required permissions to send URLs", commandName(it) in listOf("uploadtext", "suggest") || !it.message.containsURL() || manager.canPerformAction(it.author, config.permissionedActions.sendURL)) },
            { failsBecause("Did you really think I would let you do that? :thinking:", manager.canUseCommand(it.author, commandName(it)) || !it.container.has(commandName(it))) }
    )

    registerInjectionObject(MessageTracker(1), MuteService(jda, config))
    registerListenersByPath("me.aberrantfox.hotbot.listeners")

    if (config.apiConfiguration.enableCleverBot) {
        println("Enabling cleverbot integration.")
        registerListeners(MentionListener(config, jda.selfUser.name, manager))
    }

    EngineContainer.engine = setupScriptEngine(jda, container, config, logger)

    manager.defaultAndPrunePermissions(container)

    loadPersistence(jda, logger, config)

    logger.info("Fully setup, now ready for use.")
}

fun loadPersistence(jda: JDA, logger: BotLogger, config: Configuration) {
    forEachIgnoredID {
        config.security.ignoredIDs.add(it)
    }

    forEachReminder {
        val difference = timeToDifference(it.remindTime)
        val user = jda.getUserById(it.member)
        scheduleReminder(user, it.message, difference, logger)
    }
}
