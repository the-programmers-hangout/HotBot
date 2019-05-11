package me.aberrantfox.hotbot

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.commands.development.EngineContainer
import me.aberrantfox.hotbot.commands.development.EngineContainer.setupScriptEngine
import me.aberrantfox.hotbot.commands.utility.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.startBot
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.internal.command.*
import me.aberrantfox.kjdautils.internal.logging.*
import net.dv8tion.jda.core.JDA

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
    val aliasService = AliasService(manager)

    registerInjectionObject(messageService, config, logger, manager, this.config, aliasService,
            MessageTracker(1), MuteService(jda, config, log = logger))

    configure {
        prefix = config.serverInformation.prefix
        commandPath = "me.aberrantfox.hotbot.commands"
        listenerPath = "me.aberrantfox.hotbot.listeners"
        deleteMode = config.serverInformation.deletionMode
        visibilityPredicate = { cmd, user, chan, _ -> manager.canUseCommand(user, cmd) && manager.canUseCommandInChannel(user, chan.id) }
    }

    LowerUserArg.manager = manager
    aliasService.container = container
    aliasService.loadAliases()

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

    EngineContainer.engine = setupScriptEngine(jda, container, config, logger)

    manager.defaultAndPrunePermissions(container) // call this after loading all commands

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
