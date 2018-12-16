package me.aberrantfox.hotbot.utility

import me.aberrantfox.hotbot.database.deleteMutedMember
import me.aberrantfox.hotbot.database.insertMutedMember
import me.aberrantfox.hotbot.database.isMemberMuted
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.VoiceChannel
import java.awt.Color
import java.util.*

data class MuteRecord(val unmuteTime: Long, val reason: String,
                      val moderator: String, val user: String,
                      val guildId: String)

fun permMuteMember(guild: Guild, user: User, reason: String, config: Configuration, log: BotLogger) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id),
            guild.getRolesByName(config.security.mutedRole, true)).queue()

    val muteEmbed = buildMuteEmbed(user.asMention, "Indefinite", reason)
    user.sendPrivateMessage((muteEmbed), log)
}

fun muteMember(guild: Guild, user: User, time: Long, reason: String, config: Configuration, moderator: User, log: BotLogger) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id),
            guild.getRolesByName(config.security.mutedRole, true)).queue()
    val timeString = time.convertToTimeString()
    val timeToUnmute = futureTime(time)
    val record = MuteRecord(timeToUnmute, reason, moderator.id, user.id,
            guild.id)
    val muteEmbed = buildMuteEmbed(user.asMention, timeString, reason)

    user.sendPrivateMessage(muteEmbed, log)
    insertMutedMember(record)
    scheduleUnmute(guild, user, config, log, time, record)
    notifyMuteAction(guild, user, timeString, reason, config)
}

fun muteVoiceChannel(guild: Guild, voiceChannel: VoiceChannel,
                     config: Configuration, manager: PermissionManager) {
    voiceChannel.members
            .filter { !(manager.canPerformAction(it.user, config.permissionedActions.voiceChannelMuteThreshold)) }
            .forEach { guild.controller.setMute(it, true).queue() }

    notifyVoiceMuteAction(guild, voiceChannel, config)
}

fun unmuteVoiceChannel(guild: Guild, voiceChannel: VoiceChannel, config: Configuration) {

    voiceChannel.members.filterNot(Member::isOwner).forEach {
        guild.controller.setMute(it, false).queue()
    }

    notifyVoiceUnmuteAction(guild, voiceChannel, config)
}

private fun buildMuteEmbed(userMention: String, timeString: String, reason: String) = embed {
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

fun scheduleUnmute(guild: Guild, user: User, config: Configuration, log: BotLogger,
                   time: Long, muteRecord: MuteRecord) {
    if (time <= 0) {
        removeMuteRole(guild, user, config, log, muteRecord)
        return
    }

    Timer().schedule(object : TimerTask() {
        override fun run() {
            removeMuteRole(guild, user, config, log, muteRecord)
        }
    }, time)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration,
                   log: BotLogger, record: MuteRecord) {

    if(!isMemberMuted(user.id, guild.id)){
        return
    }

    if (user.mutualGuilds.isEmpty()) {
        deleteMutedMember(record)
        return
    }

    deleteMutedMember(record)
    removeMuteRole(guild, user, config, log)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration, log: BotLogger) {

    val embed = embed {
        setTitle("${user.name} - you have been unmuted.")
        setColor(Color.RED)
    }
    user.sendPrivateMessage(embed, log)
    guild.controller.removeRolesFromMember(
            guild.getMemberById(user.id),
            guild.getRolesByName(config.security.mutedRole,
                    true)).queue()
}

fun handleReJoinMute(guild: Guild, user: User, config: Configuration, log: BotLogger) {
    if (isMemberMuted(user.id, guild.id)) {
        log.alert("${user.fullName()} :: ${user.asMention} rejoined with a mute withstanding")
        guild.controller.addRolesToMember(guild.getMemberById(user.id),
                guild.getRolesByName(config.security.mutedRole, true)).queue()
    }

}

fun notifyMuteAction(guild: Guild, user: User, time: String, reason: String, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert).sendMessage("User ${user.asMention} has been muted for $time, with reason: $reason")
}

fun notifyVoiceMuteAction(guild: Guild, voiceChannel: VoiceChannel, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert).sendMessage("All non-moderators in voice channel **${voiceChannel.name}** have been muted.").queue()
}

fun notifyVoiceUnmuteAction(guild: Guild, voiceChannel: VoiceChannel, config: Configuration) {
    guild.getTextChannelById(config.logChannels.alert).sendMessage("All members in voice channel **${voiceChannel.name}** have been un-muted.").queue()
}
