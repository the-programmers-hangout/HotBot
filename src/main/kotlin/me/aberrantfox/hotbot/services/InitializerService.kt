package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.arguments.*
import me.aberrantfox.hotbot.commands.utility.*
import me.aberrantfox.hotbot.services.database.setupDatabaseSchema
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.discord.Discord
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.JDA

@Service
class InitializerService(val manager: PermissionService,
                         val kjdaConfiguration: KConfiguration,
                         val discord: Discord,
                         val loggingService: LoggingService,
                         val config: Configuration,
                         val macros: Macros,
                         val databaseService: DatabaseService) {
    init {
        kjdaConfiguration.visibilityPredicate = { cmd, user, chan, _ -> manager.canUseCommand(user, cmd.name) }
        LowerUserArg.manager = manager
        LowerMemberArg.manager = manager
        MacroInstanceCopy.macros = macros

        loadPersistence(discord.jda, loggingService.logInstance, config)
        loggingService.logInstance.info("Fully setup, now ready for use.")
    }

    private fun loadPersistence(jda: JDA, logger: BotLogger, config: Configuration) {
        databaseService.ignores.forEachIgnoredID {
            config.security.ignoredIDs.add(it)
        }

        databaseService.reminders.forEachReminder {
            val difference = timeToDifference(it.remindTime)

            jda.retrieveUserById(it.member).queue(
                    { user ->
                        scheduleReminder(user, it.message, difference, logger, databaseService)
                    },
                    { error ->
                        databaseService.reminders.deleteReminder(it.member, it.message)
                    })
        }
    }
}