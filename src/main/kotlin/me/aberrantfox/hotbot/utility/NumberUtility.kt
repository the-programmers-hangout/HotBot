package me.aberrantfox.hotbot.utility

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

fun timeToString(milliseconds: Long): String{
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60)) % 24
    val days = (milliseconds / (1000 * 60 * 60 * 24))
    return ("$days day(s), $hours hour(s), $minutes minute(s) and $seconds second(s)")
}