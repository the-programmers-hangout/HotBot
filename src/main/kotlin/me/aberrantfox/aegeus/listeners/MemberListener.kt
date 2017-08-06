package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class MemberListener(val configuration: Configuration) : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        event.guild.textChannels.findLast { it.id == configuration.welcomeChannel }
                ?.sendMessage(configuration.welcomeMessage.replace("%name%", event.user.asMention))
                ?.queue()
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        event.guild.textChannels.findLast { it.id == configuration.leaveChannel }
                ?.sendMessage(configuration.leaveMessage.replace("%name%", event.user.asMention))
                ?.queue()
    }
}