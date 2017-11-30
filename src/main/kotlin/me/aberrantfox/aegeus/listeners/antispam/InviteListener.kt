package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.commandframework.getHighestPermissionLevel
import me.aberrantfox.aegeus.extensions.containsInvite
import me.aberrantfox.aegeus.extensions.deleteIfExists
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.idToName
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object RecentInvites {
    val cache = IdTracker<Int>(6)

    fun addOrUpdate(id: String) {
        cache.map.putIfAbsent(id, 0)
        val get = cache.map[id]!!

        cache.map.put(id, get + 1)

    }

    fun value(id: String) = cache.map[id]!!
}

class InviteListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) =
            handlePossibleInviteMessage(event.member, event.message, event.guild, event.channel, event.author.isBot, event.jda)

    private fun handlePossibleInviteMessage(author: Member, message: Message, guild: Guild, channel: TextChannel,
                                            isBot: Boolean, jda: JDA) {
        if (isBot) return

        val id = author.user.id
        val maxPermissionLevel = getHighestPermissionLevel(guild, config, jda, id)

        if(maxPermissionLevel >= config.invitePermissionLevel) return

        if (message.containsInvite()) {
            var messageContent = message.rawContent

            if (messageContent.contains('@')) messageContent = messageContent.replace("@", "`@`")

            RecentInvites.addOrUpdate(id)
            val logChannel = guild.textChannels.findLast { it.id == config.logChannel }

            println(RecentInvites.value(id))

            if(RecentInvites.value(id) >= 3) {
                guild.controller.ban(author, 0, "You've been automatically banned for linking invitations. Advertising is not allowed, sorry.").queue{
                    logChannel?.sendMessage("Banned user: ${author.fullName()} ($id for advertising automatically.")?.queue()
                }
                logChannel?.sendMessage("Banned: ${id.idToName(jda)} for ${RecentInvites.value(id)} invites.")?.queue()
                RecentInvites.cache.map.remove(id)
            }

            message.deleteIfExists {
                logChannel
                    ?.sendMessage("Deleted message: $messageContent " +
                        "by ${author.asMention} " +
                        "in ${channel.asMention}")
                    ?.queue()
            }
        }
    }
}