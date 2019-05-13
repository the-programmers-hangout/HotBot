package me.aberrantfox.hotbot

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.commands.development.EngineContainer
import me.aberrantfox.hotbot.commands.development.EngineContainer.setupScriptEngine
import me.aberrantfox.hotbot.commands.utility.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.optionallisteners.MentionListener
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.startBot
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

    val manager = PermissionManager(jda, config)
    val aliasService = AliasService(manager)

    registerInjectionObject(config, logger, manager, this.config, aliasService,
            MessageTracker(1), MuteService(jda, config, log = logger))

    configure {
        prefix = config.serverInformation.prefix
        globalPath = "me.aberrantfox.hotbot"
        deleteMode = config.serverInformation.deletionMode
        visibilityPredicate = { cmd, user, chan, _ -> manager.canUseCommand(user, cmd) && manager.canUseCommandInChannel(user, chan.id) }
    }

    LowerUserArg.manager = manager
    aliasService.container = container
    aliasService.loadAliases()

    setupMacroCommands(container, manager)

    if (config.apiConfiguration.enableCleverBot) {
        println("Enabling cleverbot integration.")
        registerListeners(MentionListener(config, jda, manager))
    }

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
