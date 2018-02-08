package me.aberrantfox.hotbot.extensions

import me.aberrantfox.hotbot.dsls.command.CommandEvent
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User


fun Guild.isMember(id: String) = this.isMember(id.idToUser(this.jda))

fun Guild.getMemberJoinString(target: User) =
        if(this.isMember(target)) {
            target.toMember(this).joinDate.toString().formatJdaDate()
        } else {
            "This user is not currently in this guild"
        }

fun Guild.getRoleByName(name: String, ignoreCase: Boolean = true) = getRolesByName(name, ignoreCase).firstOrNull()

fun Guild.getRoleByIdOrName(idOrName: String): Role? {
    if(idOrName.toLowerCase() == "everyone") {
        return this.publicRole
    }
    val name = getRoleByName(idOrName, true)

    if(name != null) return name

    return try {
        getRoleById(idOrName)
    } catch(e: NumberFormatException) {
        null
    }
}