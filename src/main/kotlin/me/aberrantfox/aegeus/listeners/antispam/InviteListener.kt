package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.logging.BotLogger
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.PersistentSet
import me.aberrantfox.aegeus.services.WeightTracker
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object RecentInvites {
    val cache = WeightTracker(6)
    val ignore = PersistentSet("invite-whitelist.json")

    fun value(id: String) = cache.map[id]!!

    fun trimmedMessage(data: String): String {
        var str = data
        ignore.forEach { str = str.replace(it, "") }

        return str
    }
}

class InviteListener(val config: Configuration, val logger: BotLogger) : ListenerAdapter() {
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    private fun handlePossibleInviteMessage(author: Member, message: Message, guild: Guild, channel: TextChannel,
                                            isBot: Boolean, jda: JDA) {
        if (isBot) return

        val id = author.user.id
        val highestRole = author.user.id.idToUser(message.jda).toMember(guild).getHighestRole()

        if(config.permissionedActions.sendInvite.toRole(guild)?.isEqualOrHigherThan(highestRole) == true) return

        if (RecentInvites.trimmedMessage(message.contentRaw).containsInvite()) {
            var messageContent = message.contentRaw

            if (messageContent.contains('@')) messageContent = messageContent.replace("@", "`@`")

            RecentInvites.cache.addOrUpdate(id)
            if(RecentInvites.value(id) >= 5) {
                guild.controller.ban(author, 0, "You've been automatically banned for linking invitations. Advertising is not allowed, sorry.").queue {
                    logger.alert("Banned user: ${author.fullName()} ($id for advertising automatically.")
                }
                logger.alert("Banned: ${id.idToName(jda)} for ${RecentInvites.value(id)} invites.")
                RecentInvites.cache.map.remove(id)
            }

            message.deleteIfExists {
                logger.alert("Deleted message: $messageContent by ${author.asMention} in ${channel.asMention}")
            }
        }
    }
}