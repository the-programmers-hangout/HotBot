package me.aberrantfox.hotbot.extensions

import net.dv8tion.jda.core.entities.Member


fun Member.getHighestRole() =
    if(roles.isNotEmpty()) {
        roles.maxBy { it.position }
    } else {
        guild.roles.minBy { it.position }
    }