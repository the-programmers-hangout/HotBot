package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.MuteService
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent


class NewChannelListener(val muteService: MuteService) {
    @Subscribe
    fun onTextChannelCreate(e: TextChannelCreateEvent) =
        e.channel.createPermissionOverride(muteService.getMutedRole(e.guild)).setDeny(Permission.MESSAGE_WRITE).queue()
}