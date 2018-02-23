package me.aberrantfox.hotbot.listeners

import me.aberrantfox.hotbot.extensions.jda.descriptor
import me.aberrantfox.hotbot.logging.BotLogger
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class VoiceChannelListener (val log: BotLogger) : ListenerAdapter() {
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) =
        log.voice("**Voice Join** ${event.member.descriptor()} :: ${event.channelJoined.name}")

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) =
        log.voice("**Voice Leave** ${event.member.descriptor()} :: ${event.channelLeft.name}")

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) =
        log.voice("**Voice Move** ${event.member.descriptor()} :: ${event.channelJoined.name}")
}