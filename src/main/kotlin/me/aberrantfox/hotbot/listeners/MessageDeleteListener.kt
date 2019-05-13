package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.utility.types.LimitedList
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import java.awt.Color


class MessageDeleteListener(val logger: BotLogger, val manager: PermissionService, val config: Configuration) {
    val list = LimitedList<Message>(5000)

    @Subscribe
    fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if(event.author.isBot) return

        if(shouldBeLogged(event.author)) return

        if(event.message.contentRaw.startsWith(config.serverInformation.prefix)) return

        val found = list.find { it == event.message }

        if(found != null) {
            logger.history(embed {
                title("Message Edited")
                description("${event.author.asMention}(${event.author.fullName()}) in ${event.channel.asMention}")
                setColor(Color.ORANGE)

                field {
                    name = "Old"
                    value = found.contentRaw
                    inline = false
                }

                field {
                    name = "New"
                    value = event.message.contentRaw
                    inline = false
                }
            })

            list.remove(found)
            list.add(event.message)
        }
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
                title("Message Deleted")
                description("${found.author.asMention}(${found.author.fullName()}) in ${event.channel.asMention}")
                setColor(Color.ORANGE)

                field {
                    name = "Deleted Message"
                    value = found.contentRaw
                    inline = false
                }
            })
            list.remove(found)
        }
    }

    private fun shouldBeLogged(user: User) = manager.canPerformAction(user, config.permissionedActions.ignoreLogging)
}