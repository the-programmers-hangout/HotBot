package me.aberrantfox.hotbot.extensions.stdlib

import me.aberrantfox.hotbot.utility.randomInt

fun <T> ArrayList<T>.randomListItem() = this[randomInt(0, size - 1)]
