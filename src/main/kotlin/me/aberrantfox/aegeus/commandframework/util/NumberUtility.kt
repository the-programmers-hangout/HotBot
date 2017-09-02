package me.aberrantfox.aegeus.commandframework.util

import org.joda.time.DateTime
import java.security.SecureRandom

fun futureTime(timeToAdd: Long): Long {
    val now = DateTime.now()
    return now.plus(timeToAdd).millis
}

fun timeToDifference(time: Long): Long {
    val now = DateTime.now()
    return time - now.millis
}

fun randomInt(min: Int, max: Int): Int {
    val random = SecureRandom()
    return random.nextInt(max + 1 - min) + min
}

fun <T> randomListItem(list: List<T>) = list[randomInt(0, list.size - 1)]

fun String.isInteger(): Boolean =
        try {
            this.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }

fun String.isDouble(): Boolean =
        try {
            this.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }


fun String.isBooleanValue(): Boolean =
        when(this.toLowerCase()) {
            "true" -> true
            "false" -> true
            "t" -> true
            "f" -> true
            else -> false
        }

fun String.toBooleanValue(): Boolean =
        when(this.toLowerCase()) {
            "true" -> true
            "t" -> true
            else -> false
        }