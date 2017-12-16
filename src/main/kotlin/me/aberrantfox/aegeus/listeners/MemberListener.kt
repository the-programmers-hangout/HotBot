package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.MessageService
import me.aberrantfox.aegeus.services.MessageType
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color



class MemberListener(val configuration: Configuration) : ListenerAdapter() {

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val target = event.guild.textChannels.findLast { it.id == configuration.welcomeChannel }
        val response = MessageService.getMessage(MessageType.Join).replace("%name%", event.user.asMention)
        val userImage = event.user.avatarUrl ?: "http://i.imgur.com/HYkhEFO.jpg"

        target?.sendMessage(buildJoinMessage(response, userImage))?.queue()
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        event.guild.textChannels.findLast { it.id == configuration.logChannel }
                ?.sendMessage(configuration.leaveMessage.replace("%name%", event.user.asMention))
                ?.queue()
    }
}

private fun buildJoinMessage(response: String, image: String) =
        EmbedBuilder()
                .setTitle("Player Get!")
                .setDescription(response)
                .setColor(Color.red)
                .setThumbnail(image)
                .addField("How do I start?",
                         "Take a read of #faq, alongside #rules-and-info. When you are done, scan up and down the " +
                                 "channel set. Don't forget to say hi ;).", false)
                .build()

