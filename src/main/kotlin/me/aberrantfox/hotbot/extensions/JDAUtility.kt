package me.aberrantfox.hotbot.extensions

import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.awt.Color
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

fun Guild.hasRole(roleName: String): Boolean = this.roles.any { it.name.toLowerCase() == roleName.toLowerCase() }

fun JDA.isRole(role: String) = this.getRolesByName(role, true).size == 1

fun User.toMember(guild: Guild) = guild.getMemberById(this.id)

fun Role.isEqualOrHigherThan(other: Role?) = if(other == null) false else this.position >= other.position

fun String.toRole(guild: Guild): Role? = guild.getRoleById(this)

fun String.sanitiseMentions() = this.replace("@", "")

fun Long.convertToTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return "$days days, ${hours % 24} hours, ${minutes % 60} minutes, ${seconds % 60} seconds"
}

fun permMuteMember(guild: Guild, user: User, reason: String, config: Configuration, moderator: User) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id), guild.getRolesByName(config.security.mutedRole, true)).queue()

    user.openPrivateChannel().queue {
        val muteEmbed = buildMuteEmbed("Indefinite", reason)

        it.sendMessage(muteEmbed).queue()
    }
}

fun muteMember(guild: Guild, user: User, time: Long, reason: String, config: Configuration, moderator: User) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id), guild.getRolesByName(config.security.mutedRole, true)).queue()
    val timeString = time.convertToTimeString()

    user.openPrivateChannel().queue {
        val timeToUnmute = futureTime(time)
        val record = MuteRecord(timeToUnmute, reason, moderator.id, user.id, guild.id)

        val muteEmbed = buildMuteEmbed(timeString, reason)
        it.sendMessage(muteEmbed).queue()

        config.security.mutedMembers.add(record)
        unmute(guild, user, config, time, record)
    }

    moderator.openPrivateChannel().queue {
        it.sendMessage("User ${user.asMention} has been muted for $timeString, with reason:\n\n$reason").queue()
    }
}

private fun buildMuteEmbed(timeString: String, reason: String) =
        embed {
            title("Mute")
            description("You have been muted for:")

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
        config.security.mutedMembers.remove(record)
        return
    }

    config.security.mutedMembers.remove(record)
    removeMuteRole(guild, user, config)
}

fun removeMuteRole(guild: Guild, user: User, config: Configuration) =
    user.openPrivateChannel().queue {
        it.sendMessage("${user.name} - you have been unmuted. Please respect our rules to prevent further infractions.").queue {
            guild.controller.removeRolesFromMember(guild.getMemberById(user.id), guild.getRolesByName(config.security.mutedRole, true)).queue()
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