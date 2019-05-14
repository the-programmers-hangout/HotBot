package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.containsInvite
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.*

object RecentInvites {
    val cache = WeightTracker(6)
    val ignore = PersistentSet(configPath("invite-whitelist.json"))

    fun value(id: String) = cache.map[id]!!

    fun trimmedMessage(data: String): String {
        var str = data
        ignore.forEach { str = str.replace(it, "") }

        return str
    }
}

class InviteListener(val config: Configuration, private val logger: BotLogger, val manager: PermissionManager) {

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

        if (RecentInvites.trimmedMessage(message.contentRaw).containsInvite()) {
            var messageContent = message.contentRaw

            if (messageContent.contains('@')) messageContent = messageContent.replace("@", "`@`")

            RecentInvites.cache.addOrUpdate(id)
            if(RecentInvites.value(id) >= 5) {
                guild.controller.ban(author, 0, "You've been automatically banned for linking invitations. Advertising is not allowed, sorry.").queue {
                    logger.alert("Banned user: ${author.fullName()} ($id for advertising automatically.")
                }

                logger.alert("Banned: ${author.fullName()} for ${RecentInvites.value(id)} invites.")
                RecentInvites.cache.map.remove(id)
            }

            message.deleteIfExists {
                logger.alert("Deleted message: $messageContent by ${author.asMention} in ${channel.asMention}")
            }
        }
    }
}
