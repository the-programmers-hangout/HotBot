package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class InviteListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) =
            handlePossibleInviteMessage(event.author, event.message, event.guild, event.channel)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) =
            handlePossibleInviteMessage(event.author, event.message, event.guild, event.channel)

    private fun handlePossibleInviteMessage(author: User, message: Message, guild: Guild, channel: TextChannel) {
        if (author.isBot) return

        if (message.rawContent.matches(Regex("(.|\n)*(https://discord.gg/)+(.|\n)*"))) {
            var messageContent = message.rawContent

            if (messageContent.contains('@')) messageContent.replace("@", "`@`")

            message.delete().queue()
            guild.textChannels.findLast { it.id == config.leaveChannel }
                    ?.sendMessage("Deleted message: ${message.rawContent} " +
                            "by ${author.asMention} " +
                            "in ${channel.asMention}")
                    ?.queue()
        }
    }
}