package me.aberrantfox.aegeus.commandframework.util

import net.dv8tion.jda.core.entities.Message

fun Message.isDeleted(): Boolean = channel.getMessageById(id).complete() == null
