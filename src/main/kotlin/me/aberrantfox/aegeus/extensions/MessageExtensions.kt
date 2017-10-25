package me.aberrantfox.aegeus.extensions

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.entities.Message

private val urlRegexes = listOf(
    "[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)",
    "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)"
).map { it.toRegex() }

private val inviteRegex = "(\n|.)*((discord|discordapp).(gg|me|io|com/invite)/)(\n|.)*".toRegex()

fun Message.containsInvite() = inviteRegex.matches(rawContent)

fun Message.containsURL() = rawContent.containsURl()

fun Message.isCommandInvocation(config: Configuration) = rawContent.startsWith(config.prefix)

fun Message.deleteIfExists(runnable: () -> Unit = {}) = channel.getMessageById(id).queue { it?.delete()?.queue { runnable() } }

fun Message.mentionsSomeone() = (mentionsEveryone() || mentionedUsers.size > 0 || mentionedRoles.size > 0)

fun String.containsURl() = urlRegexes.any { this.replace("\n", "").contains(it) }