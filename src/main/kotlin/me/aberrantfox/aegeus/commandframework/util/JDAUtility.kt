package me.aberrantfox.aegeus.commandframework.util

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import java.util.*

data class MuteRecord(val unmuteTime: Long, val reason: String, val moderator: String, val user: String, val guildId: String)

fun String.isUserID(jda: JDA): Boolean = jda.getUserById(this) != null

fun String.idToName(jda: JDA): String = jda.getUserById(this).name

fun String.idToUser(jda: JDA): User = jda.getUserById(this)

fun Guild.hasRole(roleName: String): Boolean = this.roles.any { it.name.toLowerCase() == roleName }

fun Long.convertToTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return "$days days, ${hours % 24} hours, ${minutes % 60} minutes, ${seconds % 60} seconds"
}

fun muteMember(guild: Guild, user: User, time: Long, reason: String, config: Configuration, moderator: User) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id), guild.getRolesByName(config.mutedRole, true)).queue()
    val timeString = time.convertToTimeString()

    user.openPrivateChannel().queue {
        val timeToUnmute = futureTime(time)
        val record = MuteRecord(timeToUnmute, reason, moderator.id, user.id, guild.id)

        it.sendMessage("You have been muted for $timeString, reason: $reason").queue()

        config.mutedMembers.add(record)
        unmute(guild, user, config, time, record)
    }

    moderator.openPrivateChannel().queue {
        it.sendMessage("User ${user.asMention} has been muted for $timeString.").queue()
    }

}

fun unmute(guild: Guild, user: User, config: Configuration, time: Long, muteRecord: MuteRecord) {
    if(time <= 0) {
        removeMuteRole(guild, user, config, muteRecord)
        return
    }

    Timer().schedule(object : TimerTask() {
        override fun run() {
            removeMuteRole(guild, user, config, muteRecord)
        }
    }, time)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration, record: MuteRecord) {
    if(user.mutualGuilds.isEmpty()) {
        config.mutedMembers.remove(record)
        return
    }

    user.openPrivateChannel().queue {
        it.sendMessage("${user.name} - you have been unmuted. Please respect our rules to prevent" +
                " further infractions.").queue {
            guild.controller.removeRolesFromMember(guild.getMemberById(user.id), guild.getRolesByName(
                    config.mutedRole, true)).queue()
            config.mutedMembers.remove(record)
        }

    }
}

