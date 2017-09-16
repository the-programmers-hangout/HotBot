package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.VoiceMovement
import me.aberrantfox.aegeus.services.VoiceMovements
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class VoiceChannelListener : ListenerAdapter() {
    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) =
        VoiceMovements.queue.add(Pair(VoiceMovement.Join, "${event.member.asMention} :: ${event.channelJoined.name}"))

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) =
        VoiceMovements.queue.add(Pair(VoiceMovement.Leave, "${event.member.asMention} :: ${event.channelLeft.name}"))

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        VoiceMovements.queue.add(Pair(VoiceMovement.Switch, "${event.member.asMention} :: ${event.channelJoined.name}"))
    }
}