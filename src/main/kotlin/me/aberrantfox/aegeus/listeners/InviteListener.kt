package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.getHighestPermissionLevel
import me.aberrantfox.aegeus.commandframework.util.containsInvite
import me.aberrantfox.aegeus.commandframework.util.deleteIfExists
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class InviteListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    private fun handlePossibleInviteMessage(author: Member, message: Message, guild: Guild, channel: TextChannel,
                                            isBot: Boolean, jda: JDA) {
        if (isBot) return

        val maxPermissionLevel = getHighestPermissionLevel(guild, config, jda, author.user.id)

        if(maxPermissionLevel >= config.invitePermissionLevel) return

        if (message.containsInvite()) {
            var messageContent = message.rawContent

            if (messageContent.contains('@')) messageContent = messageContent.replace("@", "`@`")

            message.deleteIfExists()
            guild.textChannels.findLast { it.id == config.leaveChannel }
                    ?.sendMessage("Deleted message: $messageContent " +
                            "by ${author.asMention} " +
                            "in ${channel.asMention}")
                    ?.queue()
        }
    }
}