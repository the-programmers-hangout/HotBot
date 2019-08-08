package me.aberrantfox.hotbot.utility

import me.aberrantfox.hotbot.database.deleteMutedMember
import me.aberrantfox.hotbot.database.insertMutedMember
import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.util.*

data class MuteRecord(val unmuteTime: Long, val reason: String,
                      val moderator: String, val user: String,
                      val guildId: String)

fun permMuteMember(guild: Guild, user: User, reason: String, config: Configuration, log: BotLogger) {
    val mutedRole = guild.getRolesByName(config.security.mutedRole, true).firstOrNull()!!
    guild.addRoleToMember(guild.getMemberById(user.id)!!, mutedRole).queue()

    val muteEmbed = buildMuteEmbed(user.asMention, "Indefinite", reason)
    user.sendPrivateMessage((muteEmbed), log)
}

fun muteVoiceChannel(guild: Guild, voiceChannel: VoiceChannel,
                     config: Configuration, manager: PermissionService) {
    voiceChannel.members
            .filter { !(manager.canPerformAction(it.user, config.permissionedActions.voiceChannelMuteThreshold)) }
            .forEach { guild.mute(it, true).queue() }

    notifyVoiceMuteAction(guild, voiceChannel, config)
}

fun unmuteVoiceChannel(guild: Guild, voiceChannel: VoiceChannel, config: Configuration) {

    voiceChannel.members.filterNot(Member::isOwner).forEach {
        guild.mute(it, false).queue()
    }

    notifyVoiceUnmuteAction(guild, voiceChannel, config)
}

fun buildMuteEmbed(userMention: String, timeString: String, reason: String) = embed {
    title("Mute")
    description("""
                    | $userMention, you have been muted. A muted user cannot speak/post in channels.
                    | If you believe this to be in error, please contact a staff member.
                """.trimMargin())

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


fun removeMuteRole(guild: Guild, user: User, config: Configuration, log: BotLogger) {

    val embed = embed {
        setTitle("${user.name} - you have been unmuted.")
        setColor(Color.RED)
    }
    user.sendPrivateMessage(embed, log)
    val mutedRole = guild.getRolesByName(config.security.mutedRole, true).firstOrNull()!!
    guild.removeRoleFromMember(guild.getMemberById(user.id)!!, mutedRole).queue()
}

fun notifyMuteAction(guild: Guild, user: User, time: String, reason: String, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert)!!.sendMessage("User ${user.asMention} has been muted for $time, with reason: $reason")
}

fun notifyVoiceMuteAction(guild: Guild, voiceChannel: VoiceChannel, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert)!!.sendMessage("All non-moderators in voice channel **${voiceChannel.name}** have been muted.").queue()
}

fun notifyVoiceUnmuteAction(guild: Guild, voiceChannel: VoiceChannel, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert)!!.sendMessage("All members in voice channel **${voiceChannel.name}** have been un-muted.").queue()
}
