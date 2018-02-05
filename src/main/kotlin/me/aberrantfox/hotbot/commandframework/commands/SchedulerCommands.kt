package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.fullName
import me.aberrantfox.hotbot.extensions.isDigitOrPeriod
import me.aberrantfox.hotbot.extensions.isDouble
import me.aberrantfox.hotbot.extensions.sendPrivateMessage
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

@CommandSet
fun schedulerCommands() = commands {
    command("remindme") {
        expect(ArgumentType.Sentence)
        execute {
            val args = it.args.component1() as String
            val split = args.split(" ")

            if (split.size < 2) {
                it.respond("Error, you must enter both a time directive, and a reminder title.")
                return@execute
            }

            val timeElements = split
                .map(String::toLowerCase)
                .takeWhile { toTimeElement(it) != null }

            if(timeElements.isEmpty()) {
                it.respond("Error, one or more of your time elements didn't parse correctly. Please run the help command and try again.")
                return@execute
            }

            val timeList = timeElements.map(::toTimeElement)
            val timeString = timeElements.joinToString(" ")
            val title = args.substring(timeString.length)

            if(title.length > 1500 || timeString.length > 1500) {
                it.respond(":zzz: That's too long to remember, try something simpler... ")
                return@execute
            }

            val hasMissingQuantifier = timeList.withIndex().any {
                if (it.value is Double) {
                    if (!(timeList.indices.contains(it.index + 1))) {
                        true
                    } else {
                        timeList[it.index + 1] is Double
                    }
                } else {
                    false
                }
            }

            if (hasMissingQuantifier) {
                it.respond("The time string that you supplied is missing a quantifier.")
                return@execute
            }

            val timeInSeconds = timeList.mapIndexed { i, e ->
                when (e) {
                    is Pair<*, *> -> e
                    is Double -> {
                        Pair(e, timeList[i + 1])
                    }
                    else -> null
                }
            }
            .filter { it != null }
            .map { it as Pair<Double, String> }
            .map { it.first * timeStringToSeconds[it.second]!! }
            .reduce { a, b -> a + b }

            it.respond("Got it, I'll remind you about about that in $timeString")

            Timer().schedule((timeInSeconds * 1000).roundToLong()) {
                info("${it.author.fullName()} reminded themselves about: $title")
                it.author.sendPrivateMessage("Hi, you asked me to remind you about: $title")
            }
        }
    }
}

fun toTimeElement(element: String): Any? {
    val both = toBoth(element)

    if(both != null) return both

    val quantifier = toQuantifier(element)

    if (quantifier != null) return quantifier

    val quantity = toQuantity(element)

    if (quantity != null) return quantity

    return null
}

fun toQuantifier(element: String) = if (timeStringToSeconds.containsKey(element)) element else null

fun toQuantity(element: String) = element.toDoubleOrNull()

fun toBoth(element: String): Pair<Double, String>? {
    val quantity = element.toCharArray().takeWhile { it.isDigitOrPeriod() }.joinToString("")
    val quantifier = element.substring(quantity.length)

    if (!(timeStringToSeconds.containsKey(quantifier))) return null

    if (!(quantity.isDouble())) return null

    return Pair(quantity.toDouble(), quantifier)
}


private val timeStringToSeconds = mapOf(
    "seconds" to 1,
    "s" to 1,
    "sec" to 1,
    "h" to 3600,
    "hour" to 3600,
    "hours" to 3600,
    "minute" to 60,
    "minutes" to 60,
    "m" to 60,
    "mins" to 60,
    "min" to 60,
    "days" to 86400,
    "d" to 86400,
    "day" to 86400,
    "weeks" to 604800,
    "w" to 604800,
    "week" to 604800
)
