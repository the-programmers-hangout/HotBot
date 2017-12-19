package me.aberrantfox.aegeus.extensions

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import java.util.*

data class MuteRecord(val unmuteTime: Long, val reason: String, val moderator: String, val user: String, val guildId: String)

fun String.isUserID(jda: JDA): Boolean =
    try {
        jda.getUserById(this.trimToID()) != null
    } catch (e: NumberFormatException) {
        false
    }

fun String.idToName(jda: JDA): String = jda.getUserById(this).name

fun String.idToUser(jda: JDA): User = jda.getUserById(this.trimToID())

fun Guild.hasRole(roleName: String): Boolean = this.roles.any { it.name.toLowerCase() == roleName }

fun JDA.isRole(role: String) = this.getRolesByName(role, true).size == 1

fun Long.convertToTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return "$days days, ${hours % 24} hours, ${minutes % 60} minutes, ${seconds % 60} seconds"
}

fun permMuteMember(guild: Guild, user: User, reason: String, config: Configuration, moderator: User) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id), guild.getRolesByName(config.mutedRole, true)).queue()

    user.openPrivateChannel().queue {
        it.sendMessage("You have been muted indefinitely, for reason: $reason").queue()
    }
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

fun removeMuteRole(guild: Guild, user: User, config: Configuration, record: MuteRecord) {
    if (user.mutualGuilds.isEmpty()) {
        config.mutedMembers.remove(record)
        return
    }

    config.mutedMembers.remove(record)
    removeMuteRole(guild, user, config, record)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration) =
    user.openPrivateChannel().queue {
        it.sendMessage("${user.name} - you have been unmuted. Please respect our rules to prevent" +
            " further infractions.").queue {
            guild.controller.removeRolesFromMember(guild.getMemberById(user.id), guild.getRolesByName(
                config.mutedRole, true)).queue()
        }

    }


fun User.sendPrivateMessage(msg: MessageEmbed) =
    openPrivateChannel().queue {
        it.sendMessage(msg).queue()
    }


fun User.sendPrivateMessage(msg: String) =
    openPrivateChannel().queue {
        it.sendMessage(msg).queue()
    }

fun List<String>.isUserIDList(jda: JDA) = this.all { it.isUserID(jda) }

fun JDA.performActionIfIsID(id: String, action: (User) -> Unit) =
    retrieveUserById(id).queue {
        action(it)
    }

private fun String.trimToID(): String =
    if (this.startsWith("<@") && this.endsWith(">")) {
        this.substring(2, this.length - 1)
    } else {
        this
    }