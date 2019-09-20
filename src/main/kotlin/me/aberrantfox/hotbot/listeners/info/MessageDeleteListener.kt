package me.aberrantfox.hotbot.listeners.info

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.extensions.createContinuableField
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.types.LimitedList
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.guild.*
import java.awt.Color

class MessageDeleteListener(private val logger: BotLogger, val manager: PermissionService, val config: Configuration) {
    val list = LimitedList<Message>(5000)

    @Subscribe
    fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if(event.author.isBot) return

        if(shouldBeLogged(event.author)) return

        if(event.message.contentRaw.startsWith(config.serverInformation.prefix)) return

        val found = list.find { it == event.message } ?: return

        logger.history(embed {
            title = "Message Edited"
            description = "${event.author.asMention}(${event.author.fullName()}) in ${event.channel.asMention}"
            color = Color.ORANGE

            createContinuableField("Old", found.contentRaw)
            createContinuableField("New", event.message.contentRaw)
        })

        list.remove(found)
        list.add(event.message)
    }

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.author.isBot) return

        if(shouldBeLogged(event.author)) return

        list.add(event.message)
    }

    @Subscribe
    fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        val found = list.find { it.id == event.messageId }

        if(found != null) {
            logger.history(embed {
                title = "Message Deleted"
                description = "${found.author.asMention}(${found.author.fullName()}) in ${event.channel.asMention}"
                color = Color.ORANGE

                createContinuableField("Deleted Message", found.contentRaw)
            })
            list.remove(found)
        }
    }

    private fun shouldBeLogged(user: User) = manager.canPerformAction(user, config.permissionedActions.ignoreLogging)
}