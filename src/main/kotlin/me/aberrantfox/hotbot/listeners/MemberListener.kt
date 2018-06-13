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
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule
import me.aberrantfox.hotbot.services.UserID
import net.dv8tion.jda.core.audit.ActionType

typealias MessageID = String

class MemberListener(val configuration: Configuration, val logger: BotLogger, val mService: MService) {
    private val welcomeMessages = ConcurrentHashMap<UserID, MessageID>()

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val target = event.guild.textChannels.findLast { it.id == configuration.messageChannels.welcomeChannel }
        val response = mService.messages.onJoin.randomListItem().replace("%name%", "${event.user.asMention}(${event.user.fullName()})")
        val userImage = event.user.effectiveAvatarUrl

        val rejoin = hasLeaveHistory(event.user.id, event.guild.id)
        logger.info("${event.user.fullName()} :: ${event.user.asMention} created on ${event.user.creationTime.toString().formatJdaDate()} -- ${if (rejoin) "re" else ""}joined the server")

        target?.sendMessage(buildJoinMessage(response, userImage, if (rejoin) "Player Resumes!" else "Player Get!"))?.queue { msg ->
            msg.addReaction("\uD83D\uDC4B").queue {
                welcomeMessages.put(event.user.id, msg.id)
                Timer().schedule(1000 * 60 * 60) {
                    welcomeMessages.takeIf { it.containsKey(event.user.id) }?.remove(event.user.id)
                }
            }
        }
    }

    @Subscribe
    fun onGuildMemberLeave(e: GuildMemberLeaveEvent) {
        logger.info("${e.user.fullName()} :: ${e.user.asMention} left the server")

        if(configuration.serverInformation.deleteWelcomeOnLeave && welcomeMessages.containsKey(e.user.id)) {
            val messageID = welcomeMessages[e.user.id]
            e.jda.getTextChannelById(configuration.messageChannels.welcomeChannel).getMessageById(messageID).queue {
                it.delete().queue()
            }
        }

        val now = DateTime.now()
        Timer().schedule(500) {
            e.guild.auditLogs.type(ActionType.BAN).limit(3).queue { logs ->
                val ban = logs.any { it.targetId == e.user.id && it.creationTime.toEpochSecond() - now.millis / 1000 in -2..2 }
                insertLeave(e.user.id, DateTime(1000 * e.member.joinDate.toEpochSecond()), now, e.guild.id, ban)
            }
        }
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



