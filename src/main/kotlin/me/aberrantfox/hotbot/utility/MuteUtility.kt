package me.aberrantfox.hotbot.utility

import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.*
import java.awt.Color

data class MuteRecord(val unmuteTime: Long, val reason: String,
                      val moderator: String, val user: String,
                      val guildId: String)

fun permMuteMember(member: Member, reason: String, config: Configuration, log: BotLogger) {
    val guild = member.guild
    val mutedRole = guild.getRolesByName(config.security.mutedRole, true).firstOrNull()!!
    guild.addRoleToMember(member, mutedRole).queue()

    val muteEmbed = buildMuteEmbed(member.asMention, "Indefinite", reason)
    member.user.sendPrivateMessage((muteEmbed), log)
}

fun buildMuteEmbed(userMention: String, timeString: String, reason: String) = embed {
    title = "Mute"
    description = """
                    | $userMention, you have been muted. A muted user cannot speak/post in channels.
                    | If you believe this to be in error, please contact a staff member.
                """.trimMargin()

    field {
        name = "Length"
        value = timeString
        inline = false
    }

    field {
        name = "__Reason__"
        value = reason
        inline = false
    }
    color = Color.RED
}

fun removeMuteRole(member: Member, config: Configuration, log: BotLogger) {

    val embed = embed {
        title = "${member.user.name} - you have been unmuted."
        color = Color.RED
    }

    member.user.sendPrivateMessage(embed, log)

    val guild = member.guild

    val mutedRole = guild.getRoleByName(config.security.mutedRole, true)!!
    guild.removeRoleFromMember(member, mutedRole).queue()
}

fun notifyMuteAction(guild: Guild, user: User, time: String, reason: String, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert)!!.sendMessage("User ${user.asMention} has been muted for $time, with reason: $reason")
}
