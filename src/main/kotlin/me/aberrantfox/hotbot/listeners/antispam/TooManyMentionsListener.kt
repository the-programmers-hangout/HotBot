package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.MuteService
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent


class TooManyMentionsListener(val log: BotLogger, private val muteService: MuteService) {
    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val member = event.member ?: return // Message is a WebhookMessage if null

        if (member.roles.isNotEmpty()) return

        if (event.message.mentionedUsers.size >= 7) {
            event.message.delete().queue()
            log.alert("${event.author.fullName()} sent a message with ${event.message.mentionedUsers.size} mentions, and it was deleted.")
            member.guild.addRoleToMember(member, muteService.getMutedRole(event.guild)).queue()
        }
    }
}