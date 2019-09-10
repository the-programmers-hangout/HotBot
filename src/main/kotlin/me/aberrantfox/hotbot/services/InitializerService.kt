package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.arguments.LowerMemberArg
import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.commands.utility.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.discord.Discord
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.exceptions.ErrorResponseException

@Service
class InitializerService(manager: PermissionService, container: CommandsContainer, kjdaConfiguration: KConfiguration,
                         discord: Discord, logger: BotLogger, config: Configuration) {
    init {
        kjdaConfiguration.visibilityPredicate = { cmd, user, chan, _ -> manager.canUseCommand(user, cmd) && manager.canUseCommandInChannel(user, chan.id) }
        LowerUserArg.manager = manager
        LowerMemberArg.manager = manager
        setupMacroCommands(container, manager)
        manager.defaultAndPrunePermissions(container)

        loadPersistence(discord.jda, logger, config)
        logger.info("Fully setup, now ready for use.")
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