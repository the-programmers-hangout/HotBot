package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.database.deleteReminder
import me.aberrantfox.hotbot.database.insertReminder
import me.aberrantfox.hotbot.utility.futureTime
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.TimeStringArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.User
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

@CommandSet
fun schedulerCommands(log: BotLogger) = commands {
    command("remindme") {
        expect(TimeStringArg, SentenceArg)
        execute {
            val timeMilliSecs = (it.args.component1() as Double).roundToLong() * 1000
            val title = it.args.component2()

            it.respond("Got it, I'll remind you about that in ${timeMilliSecs.convertToTimeString()}")

            insertReminder(it.author.id, title as String, futureTime(timeMilliSecs))
            scheduleReminder(it.author, title, timeMilliSecs, log)
        }
    }
}

fun scheduleReminder(user: User, message: String, timeMilli: Long, log: BotLogger) {
    fun remindTask() {
        deleteReminder(user.id, message)
        log.info("${user.fullName()} reminded themselves about: $message")
        user.sendPrivateMessage("Hi, you asked me to remind you about: $message")
    }

    if (timeMilli <= 0) {
        remindTask()
        return
    }

    Timer().schedule(timeMilli) { remindTask() }
}
