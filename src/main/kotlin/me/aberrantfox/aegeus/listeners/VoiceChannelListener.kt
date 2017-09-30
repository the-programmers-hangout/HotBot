package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.extensions.descriptor
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class VoiceChannelListener (val channel: MessageChannel) : ListenerAdapter() {
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) =
        channel.sendMessage("**Voice Join** ${event.member.descriptor()} :: ${event.channelJoined.name}").queue()

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) =
        channel.sendMessage("**Voice Leave** ${event.member.descriptor()} :: ${event.channelLeft.name}").queue()

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) =
        channel.sendMessage("**Voice Move** ${event.member.descriptor()} :: ${event.channelJoined.name}").queue()
}