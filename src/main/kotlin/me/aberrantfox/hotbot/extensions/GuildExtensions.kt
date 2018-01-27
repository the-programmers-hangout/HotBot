package me.aberrantfox.hotbot.extensions

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User


fun Guild.hasMember(user: User) = this.hasMember(user.id)

fun Guild.hasMember(id: String) = members.any { it.user.id == id }
