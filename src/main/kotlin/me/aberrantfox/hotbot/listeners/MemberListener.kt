package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.database.hasLeaveHistory
import me.aberrantfox.hotbot.database.insertLeave
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.stdlib.formatJdaDate
import me.aberrantfox.kjdautils.extensions.stdlib.randomListItem
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import org.joda.time.DateTime
import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule


class MemberListener(val configuration: Configuration, val logger: BotLogger, val mService: MService) {

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val target = event.guild.textChannels.findLast { it.id == configuration.messageChannels.welcomeChannel }
        val response = mService.messages.onJoin.randomListItem().replace("%name%", "${event.user.asMention}(${event.user.fullName()})")
        val userImage = event.user.effectiveAvatarUrl

        val rejoin = hasLeaveHistory(event.user.id, event.guild.id)
        logger.info("${event.user.fullName()} :: ${event.user.asMention} created on ${event.user.creationTime.toString().formatJdaDate()} -- ${if (rejoin) "re" else ""}joined the server")

        target?.sendMessage(buildJoinMessage(response, userImage, if (rejoin) "Player Resumes!" else "Player Get!"))?.queue { msg->
            msg.addReaction("\uD83D\uDC4B").queue {
                WelcomeMessages.map.put(event.user.id, msg.id)
                Timer().schedule(1000 * 60 * 60) {
                    WelcomeMessages.map.takeIf { it.containsKey(event.user.id) }?.remove(event.user.id)
                }
            }
        }
    }

    @Subscribe
    fun onGuildMemberLeave(e: GuildMemberLeaveEvent) {
        logger.info("${e.user.fullName()} :: ${e.user.asMention} left the server")

        if(configuration.serverInformation.deleteWelcomeOnLeave && WelcomeMessages.map.containsKey(e.user.id)) {
            val messageID = WelcomeMessages.map[e.user.id]
            e.jda.getTextChannelById(configuration.messageChannels.welcomeChannel).getMessageById(messageID).queue {
                it.delete().queue()
            }
        }

        insertLeave(e.user.id, DateTime(e.member.joinDate.toEpochSecond() * 1000), e.guild.id, !e.guild.isMember(e.user))
    }

    private fun buildJoinMessage(response: String, image: String, title: String) =
        EmbedBuilder()
            .setTitle(title)
            .setDescription(response)
            .setColor(Color.red)
            .setThumbnail(image)
            .addField("How do I start?", mService.messages.welcomeDescription, false)
            .build()
}



