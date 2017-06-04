package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class MuteListener(val config: Configuration): ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(config.mutedMembers.contains(event.member.user.id)) {
            event.message.delete().queue()
        }
    }
}
