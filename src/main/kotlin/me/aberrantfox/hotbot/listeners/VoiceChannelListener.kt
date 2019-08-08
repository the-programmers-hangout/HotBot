package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.kjdautils.extensions.jda.descriptor
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent


class VoiceChannelListener (val log: BotLogger) {
    @Subscribe
    fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) =
        log.voice("**Voice Join** ${event.member.descriptor()} :: ${event.channelJoined.name}")

    @Subscribe
    fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) =
        log.voice("**Voice Leave** ${event.member.descriptor()} :: ${event.channelLeft.name}")

    @Subscribe
    fun onGuildVoiceMove(event: GuildVoiceMoveEvent) =
        log.voice("**Voice Move** ${event.member.descriptor()} :: ${event.channelJoined.name}")
}