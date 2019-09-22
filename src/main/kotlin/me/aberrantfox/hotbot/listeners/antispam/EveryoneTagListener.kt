package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.LoggingService
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent


class EveryoneTagListener(val loggingService: LoggingService) {
    @Subscribe fun onMessageReceived(event: GuildMessageReceivedEvent) =
            handleEveryoneTag(event.member, event.message, event.channel)

    @Subscribe fun onMessageUpdate(event: GuildMessageUpdateEvent) =
            handleEveryoneTag(event.member, event.message, event.channel)

    private fun handleEveryoneTag(member: Member?, message: Message, channel: TextChannel) {
        if (member?.roles?.isNotEmpty() != false) return

        if (member.user.isBot) return

        // mentionsEveryone only works if the message actually pings, so search for the tag manually
        if (listOf("@here", "@everyone").any { message.contentRaw.contains(it) }) {
            loggingService.logInstance.alert("everyone mention in ${channel.asMention} by ${message.author.asMention}")

            message.delete().queue()
            val response = "Your message has been deleted. ${message.author.asMention}\n" +
                           "Please be more considerate and try not to ping everyone whatever be your purpose."
            channel.sendMessage(response).queue()
        }
    }

}

