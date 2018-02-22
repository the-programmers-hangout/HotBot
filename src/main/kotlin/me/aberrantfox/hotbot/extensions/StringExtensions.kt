package me.aberrantfox.hotbot.extensions

fun String.formatJdaDate() = this.substring(0, this.indexOf("T"))

fun String.limit(length: Int) = if (this.length > length) substring(length) else this