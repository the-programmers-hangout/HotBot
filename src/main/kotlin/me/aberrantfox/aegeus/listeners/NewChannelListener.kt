package me.aberrantfox.aegeus.listeners

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class NewChannelListener(private val role: Role) : ListenerAdapter() {
    override fun onTextChannelCreate(event: TextChannelCreateEvent) =
        event.channel.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue()
}