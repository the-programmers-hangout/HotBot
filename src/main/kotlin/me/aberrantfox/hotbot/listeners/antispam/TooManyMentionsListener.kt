package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent


class TooManyMentionsListener(val log: BotLogger, val mutedRole: Role) {
    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val member = event.member ?: return // Message is a WebhookMessage if null

        if (member.roles.isNotEmpty()) return

        if (event.message.mentionedUsers.size >= 7) {
            event.message.delete().queue()
            log.alert("${event.author.fullName()} sent a message with ${event.message.mentionedUsers.size} mentions, and it was deleted.")
            member.guild.controller.addRolesToMember(member, mutedRole).queue()
        }
    }
}