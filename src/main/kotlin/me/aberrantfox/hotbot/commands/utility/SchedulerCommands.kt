package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.database.deleteReminder
import me.aberrantfox.hotbot.database.insertReminder
import me.aberrantfox.hotbot.services.LoggingService
import me.aberrantfox.hotbot.utility.futureTime
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import me.aberrantfox.kjdautils.internal.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.arguments.TimeStringArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.User
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

@CommandSet("utility")
fun schedulerCommands(loggingService: LoggingService) = commands {
    command("remindme") {
        description = "A command that'll remind you about something after the specified time."
        expect(TimeStringArg, SentenceArg("Reminder Message"))
        execute {
            val timeMilliSecs = (it.args.component1() as Double).roundToLong() * 1000
            val title = it.args.component2()

            it.respond("Got it, I'll remind you about that in ${timeMilliSecs.convertToTimeString()}")

            insertReminder(it.author.id, title as String, futureTime(timeMilliSecs))
            scheduleReminder(it.author, title, timeMilliSecs, loggingService.logInstance)
        }
    }
}

fun scheduleReminder(user: User, message: String, timeMilli: Long, log: BotLogger) {
    fun remindTask() {
        deleteReminder(user.id, message)
        log.info("${user.fullName()} reminded themselves about: ${message.sanitiseMentions()}")
        user.sendPrivateMessage("Hi, you asked me to remind you about: $message", log)
    }

    if (timeMilli <= 0) {
        remindTask()
        return
    }

    Timer().schedule(timeMilli) { remindTask() }
}
