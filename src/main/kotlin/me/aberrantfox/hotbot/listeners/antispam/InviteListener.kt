package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.RecentInviteService
import me.aberrantfox.hotbot.utility.types.PersistentSet
import me.aberrantfox.hotbot.services.WeightTracker
import me.aberrantfox.kjdautils.api.annotation.Data
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.containsInvite
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.guild.*

@Data("config/invite-whitelist.json")
data class InviteWhitelist(val set: HashSet<String> = HashSet())

class InviteListener(val config: Configuration,
                     val inviteWhitelist: InviteWhitelist,
                     val recentInviteService: RecentInviteService,
                     val manager: PermissionService,
                     private val logger: BotLogger) {
    @Subscribe
    fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    private fun handlePossibleInviteMessage(author: Member?, message: Message, guild: Guild, channel: TextChannel,
                                            isBot: Boolean, jda: JDA) {
        if (isBot || author == null) return

        val id = author.user.id

        if(manager.canPerformAction(author.user, config.permissionedActions.sendInvite)) return

        if (recentInviteService.trimmedMessage(message.contentRaw).containsInvite()) {
            var messageContent = message.contentRaw

            if (messageContent.contains('@')) messageContent = messageContent.replace("@", "`@`")

            recentInviteService.cache.addOrUpdate(id)
            if(recentInviteService.value(id) >= 5) {
                guild.ban(author, 0, "You've been automatically banned for linking invitations. Advertising is not allowed, sorry.").queue {
                    logger.alert("Banned user: ${author.fullName()} ($id for advertising automatically.")
                }

                logger.alert("Banned: ${author.fullName()} for ${recentInviteService.value(id)} invites.")
                recentInviteService.cache.map.remove(id)
            }

            message.deleteIfExists {
                logger.alert("Deleted message: $messageContent by ${author.asMention} in ${channel.asMention}")
            }
        }
    }
}
