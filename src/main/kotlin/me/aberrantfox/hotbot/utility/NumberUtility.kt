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