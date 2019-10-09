package me.aberrantfox.hotbot.listeners.moderation

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.LoggingService
import me.aberrantfox.hotbot.services.MuteService
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent

class RejoinMuteListener(val muteService: MuteService, val loggingService: LoggingService) {
    @Subscribe
    fun handleReJoinMute(event: GuildMemberJoinEvent) {
        val member = event.member
        val user = event.user
        val guild = event.guild

        if (muteService.checkMuteState(member) == MuteService.MuteState.TrackedMute) {
            loggingService.logInstance.alert("${user.fullName()} :: ${user.asMention} rejoined with a mute withstanding")
            guild.addRoleToMember(member, muteService.getMutedRole(guild)).queue()
        }
    }
}