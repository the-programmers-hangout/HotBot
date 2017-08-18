package me.aberrantfox.aegeus.commandframework.util

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User


fun String.isUserID(jda: JDA): Boolean = jda.getUserById(this) != null

fun String.idToName(jda: JDA): String = jda.getUserById(this).name

fun String.idToUser(jda: JDA): User = jda.getUserById(this)