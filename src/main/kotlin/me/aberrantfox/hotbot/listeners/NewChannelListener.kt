package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent


class NewChannelListener(private val role: Role) {
    @Subscribe
    fun onTextChannelCreate(event: TextChannelCreateEvent) =
        event.channel.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue()
}