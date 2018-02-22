package me.aberrantfox.hotbot.listeners

import me.aberrantfox.hotbot.extensions.fullName
import me.aberrantfox.hotbot.extensions.randomListItem
import me.aberrantfox.hotbot.logging.BotLogger
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MService
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule


class MemberListener(val configuration: Configuration, val logger: BotLogger, val mService: MService) : ListenerAdapter() {

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val target = event.guild.textChannels.findLast { it.id == configuration.messageChannels.welcomeChannel }
        val response = mService.messages.onJoin.randomListItem().replace("%name%", "${event.user.asMention}(${event.user.fullName()})")
        val userImage = event.user.effectiveAvatarUrl

        target?.sendMessage(buildJoinMessage(response, userImage))?.queue { msg->
            msg.addReaction("\uD83D\uDC4B").queue {
                WelcomeMessages.map.put(event.user.id, msg.id)
                Timer().schedule(1000 * 60 * 60) {
                    WelcomeMessages.map.takeIf { it.containsKey(event.user.id) }?.remove(event.user.id)
                }
            }
        }
    }

    override fun onGuildMemberLeave(e: GuildMemberLeaveEvent) = logger.info("${e.user.asMention} left the server")

    private fun buildJoinMessage(response: String, image: String) =
        EmbedBuilder()
            .setTitle("Player Get!")
            .setDescription(response)
            .setColor(Color.red)
            .setThumbnail(image)
            .addField("How do I start?", mService.messages.welcomeDescription, false)
            .build()
}



