package me.aberrantfox.aegeus.extensions

import net.dv8tion.jda.core.entities.Member


fun Member.getHighestRole() = roles.maxBy { it.position }