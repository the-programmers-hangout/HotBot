package me.aberrantfox.hotbot.commandframework.commands.utility

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.jda.sendPrivateMessage
import me.aberrantfox.hotbot.extensions.stdlib.convertToTimeString
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

@CommandSet
fun schedulerCommands() = commands {

    val log = this.log

    command("remindme") {
        expect(ArgumentType.TimeString, ArgumentType.Sentence)
        execute {
            val timeMilliSecs = (it.args.component1() as Double).roundToLong() * 1000
            val title = it.args.component2()

            it.respond("Got it, I'll remind you about that in ${timeMilliSecs.convertToTimeString()}")

            Timer().schedule(timeMilliSecs) {
                log.info("${it.author.fullName()} reminded themselves about: $title")
                it.author.sendPrivateMessage("Hi, you asked me to remind you about: $title")
            }
        }
    }
}

