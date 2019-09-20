package me.aberrantfox.hotbot.listeners.moderation

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.MuteService
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent

class NewChannelMuteOverrideListener(private val muteService: MuteService) {
    @Subscribe
    fun onTextChannelCreate(e: TextChannelCreateEvent) =
            e.channel.createPermissionOverride(muteService.getMutedRole(e.guild)).setDeny(Permission.MESSAGE_WRITE).queue()
}
