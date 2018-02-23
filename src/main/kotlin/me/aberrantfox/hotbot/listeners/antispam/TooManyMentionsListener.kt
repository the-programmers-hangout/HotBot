package me.aberrantfox.hotbot.listeners.antispam

import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.logging.BotLogger
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class TooManyMentionsListener(val log: BotLogger, val mutedRole: Role) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.member?.roles?.isNotEmpty() != false) return // either has roles or is null (WebHookMessage)

        if(event.message.mentionedUsers.size >= 7) {
            event.message.delete().queue()
            log.alert("${event.author.fullName()} sent a message with ${event.message.mentionedUsers.size} mentions, and it was deleted.")
            event.member.guild.controller.addRolesToMember(event.member, mutedRole).queue()
        }
    }
}