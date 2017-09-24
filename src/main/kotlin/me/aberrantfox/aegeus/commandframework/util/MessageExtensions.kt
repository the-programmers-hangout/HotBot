package me.aberrantfox.aegeus.commandframework.util

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.entities.Message

private val urlRegexes = listOf(
        "[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)",
        "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)"
).map { it.toRegex() }

private val inviteRegex = "(\n|.)*((discord|discordapp).(gg|me|io|com/invite)/)(\n|.)*".toRegex()

fun Message.isDeleted() = channel.getMessageById(id).complete() == null

fun Message.containsInvite() = inviteRegex.matches(rawContent)

fun Message.containsURL() = rawContent.containsURl()

fun Message.isCommandInvocation(config: Configuration) = rawContent.startsWith(config.prefix)

fun Message.isDoubleCommandInvocation(config: Configuration) = this.rawContent.startsWith(config.prefix + config.prefix)

fun Message.deleteIfExists() = if(! (isDeleted()) ) delete().queue() else Unit

fun Message.mentionsSomeone() = (mentionsEveryone() || mentionedUsers.size > 0 || mentionedRoles.size > 0)

fun String.containsURl() = urlRegexes.any { this.replace("\n", "").contains(it) }