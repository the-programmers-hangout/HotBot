package me.aberrantfox.aegeus.commandframework.util

import net.dv8tion.jda.core.JDA


fun String.isUserID(jda: JDA): Boolean = jda.getUserById(this) != null
