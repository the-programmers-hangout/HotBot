package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class BanReasonReminderListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildBan(event: GuildBanEvent) {
        event.guild.auditLogs.queue {
            val entry = it.first { it.type == ActionType.BAN }

            if(entry?.reason.isNullOrEmpty()) {
                event.guild.getTextChannelById(config.logChannel)
                    .sendMessage("${ entry.user.asMention } -- The reason for ${event.user.fullName()}'s ban was null or empty, add a reason using ${config.prefix}setbanreason")
                    .queue()
            }
        }
    }
}