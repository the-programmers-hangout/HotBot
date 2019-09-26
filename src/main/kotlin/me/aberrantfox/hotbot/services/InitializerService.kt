package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.arguments.*
import me.aberrantfox.hotbot.commands.utility.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.discord.Discord
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.JDA

@Service
class InitializerService(manager: PermissionService,
                         kjdaConfiguration: KConfiguration,
                         discord: Discord,
                         loggingService: LoggingService,
                         config: Configuration,
                         macros: Macros) {
    init {
        kjdaConfiguration.visibilityPredicate = { cmd, user, chan, _ -> manager.canUseCommand(user, cmd.name) }
        LowerUserArg.manager = manager
        LowerMemberArg.manager = manager
        MacroInstanceCopy.macros = macros

        loadPersistence(discord.jda, loggingService.logInstance, config)
        loggingService.logInstance.info("Fully setup, now ready for use.")
    }

    private fun loadPersistence(jda: JDA, logger: BotLogger, config: Configuration) {
        forEachIgnoredID {
            config.security.ignoredIDs.add(it)
        }

        forEachReminder {
            val difference = timeToDifference(it.remindTime)

            jda.retrieveUserById(it.member).queue(
                    { user ->
                        scheduleReminder(user, it.message, difference, logger)
                    },
                    { error ->
                        deleteReminder(it.member, it.message)
                    })
        }
    }
}