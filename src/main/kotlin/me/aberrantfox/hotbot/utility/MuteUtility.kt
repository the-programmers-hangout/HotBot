package me.aberrantfox.hotbot.utility

import me.aberrantfox.hotbot.database.deleteMutedMember
import me.aberrantfox.hotbot.database.insertMutedMember
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.VoiceChannel
import java.awt.Color
import java.util.*


data class MuteRecord(val unmuteTime: Long, val reason: String,
                      val moderator: String, val user: String,
                      val guildId: String)

fun permMuteMember(guild: Guild, user: User, reason: String, config: Configuration) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id),
            guild.getRolesByName(config.security.mutedRole, true)).queue()

    user.openPrivateChannel().queue {
        val muteEmbed = buildMuteEmbed(user.asMention, "Indefinite", reason)

        it.sendMessage(muteEmbed).queue()
    }
}

fun muteMember(guild: Guild, user: User, time: Long, reason: String,
               config: Configuration, moderator: User) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id),
            guild.getRolesByName(config.security.mutedRole, true)).queue()
    val timeString = time.convertToTimeString()

    user.openPrivateChannel().queue {
        val timeToUnmute = futureTime(time)
        val record = MuteRecord(timeToUnmute, reason, moderator.id, user.id,
                guild.id)

        val muteEmbed = buildMuteEmbed(user.asMention, timeString, reason)
        it.sendMessage(muteEmbed).queue()

        insertMutedMember(record)
        scheduleUnmute(guild, user, config, time, record)
    }

    moderator.openPrivateChannel().queue {
        it.sendMessage(
                "User ${user.asMention} has been muted for $timeString, with reason:\n\n$reason")
                .queue()
    }
}

fun muteVoiceChannel(guild: Guild, voiceChannel: VoiceChannel,
                     moderator: User, config: Configuration, manager: PermissionManager) {
    voiceChannel.members
            .filter { !(manager.canPerformAction(it.user, config.permissionedActions.voiceChannelMuteThreshold)) }
            .forEach { guild.controller.setMute(it, true).queue() }

    moderator.openPrivateChannel().queue {
        it.sendMessage("All non-moderators in voice channel **${voiceChannel.name}** have been muted.").queue()
    }
}

fun unmuteVoiceChannel(guild: Guild, voiceChannel: VoiceChannel, moderator: User) {

    voiceChannel.members.filterNot(Member::isOwner).forEach {
        guild.controller.setMute(it, false).queue()
    }

    moderator.openPrivateChannel().queue {
        it.sendMessage("All members in voice channel **${voiceChannel.name}** have been un-muted.").queue()
    }
}

private fun buildMuteEmbed(userMention: String, timeString: String,
                           reason: String) = embed {
    title("Mute")
    description(
            "$userMention, you have been muted.\nA muted user cannot speak, post in channels, or react to messages.")

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


    setColor(Color.RED)
}

fun scheduleUnmute(guild: Guild, user: User, config: Configuration, time: Long,
                   muteRecord: MuteRecord) {
    if (time <= 0) {
        removeMuteRole(guild, user, config, muteRecord)
        return
    }

    Timer().schedule(object : TimerTask() {
        override fun run() {
            removeMuteRole(guild, user, config, muteRecord)
        }
    }, time)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration,
                   record: MuteRecord) {
    if (user.mutualGuilds.isEmpty()) {
        deleteMutedMember(record)
        return
    }

    deleteMutedMember(record)
    removeMuteRole(guild, user, config)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration) =
        user.openPrivateChannel().queue {
            it.sendMessage(
                    "${user.name} - you have been unmuted. Please respect our rules to prevent further infractions.")
                    .queue {
                        guild.controller.removeRolesFromMember(
                                guild.getMemberById(user.id),
                                guild.getRolesByName(config.security.mutedRole,
                                        true)).queue()
                    }
        }
