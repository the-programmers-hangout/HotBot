package me.aberrantfox.hotbot.extensions

import me.aberrantfox.hotbot.dsls.command.CommandEvent
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User


fun Guild.isMember(id: String) = this.isMember(id.idToUser(this.jda))

fun Guild.getMemberJoinString(target: User) =
        if(this.isMember(target)) {
            target.toMember(this).joinDate.toString().formatJdaDate()
        } else {
            "This user is not currently in this guild"
        }