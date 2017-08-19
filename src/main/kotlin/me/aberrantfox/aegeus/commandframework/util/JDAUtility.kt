package me.aberrantfox.aegeus.commandframework.util

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import java.util.*


fun String.isUserID(jda: JDA): Boolean = jda.getUserById(this) != null

fun String.idToName(jda: JDA): String = jda.getUserById(this).name

fun String.idToUser(jda: JDA): User = jda.getUserById(this)

fun Guild.hasRole(roleName: String): Boolean = this.roles.any { it.name.toLowerCase() == roleName }

fun Long.convertToTimeString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return "$days:${hours % 24}:${minutes % 60}:${seconds % 60}"
}

fun muteMember(guild: Guild, user: User, time: Long, reason: String, config: Configuration) {
    guild.controller.addRolesToMember(guild.getMemberById(user.id), guild.getRolesByName(config.mutedRole, true)).queue()

    user.openPrivateChannel().queue {
        it.sendMessage("You have been muted for ${time.convertToTimeString()} minutes, reason: $reason").queue()
        unmute(guild, user, config, time)
    }
}

fun unmute(guild: Guild, user: User, config: Configuration, time: Long) =
        Timer().schedule(object : TimerTask() {
            override fun run() {
                guild.controller.removeRolesFromMember(guild.getMemberById(user.id), guild.getRolesByName(config.mutedRole, true)).queue()
                user.openPrivateChannel().queue {
                    it.sendMessage("${user.name} - you have been unmuted. Please respect our rules to prevent" +
                        " further infractions.").queue()
                }
            }
        }, time)
