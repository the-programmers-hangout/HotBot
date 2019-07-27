package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.commands.administration.sendWelcome
import me.aberrantfox.hotbot.database.hasLeaveHistory
import me.aberrantfox.hotbot.database.insertLeave
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MessageService
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
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import me.aberrantfox.hotbot.utility.handleReJoinMute

typealias MessageID = String
typealias UserID = String

class MemberListener(val configuration: Configuration, private val logger: BotLogger, private val messageService: MessageService) {
    private val welcomeMessages = ConcurrentHashMap<UserID, MessageID>()

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {

        val secsSinceCreation = (System.currentTimeMillis() / 1000) - event.user.creationTime.toEpochSecond()
        val numOfDays = TimeUnit.DAYS.convert(secsSinceCreation, TimeUnit.SECONDS).toInt()
        val user = "${event.user.fullName()} :: ${event.user.asMention}"
        val date = event.user.creationTime.toString().formatJdaDate()
        val rejoin = hasLeaveHistory(event.user.id, event.guild.id)
        val newUserThreshold = 5

        logger.info("$user created $numOfDays days ago ($date) -- ${if (rejoin) "re" else ""}joined the server")

        if (numOfDays <= newUserThreshold)
            logger.alert("$user has joined the server but the account has only existed for $numOfDays day${if (numOfDays == 1) "" else "s"}. Potential action required.")
        if(sendWelcome){
            //Build welcome message
            val target = event.guild.textChannels.findLast { it.id == configuration.messageChannels.welcomeChannel }
            val response = messageService.messages.onJoin.randomListItem().replace("%name%", "${event.user.asMention}(${event.user.fullName()})")
            val userImage = event.user.effectiveAvatarUrl

            target?.sendMessage(buildJoinMessage(response, userImage, if (rejoin) "Player Resumes!" else "Player Get!"))?.queue { msg ->
                msg.addReaction("\uD83D\uDC4B").queue {
                    welcomeMessages[event.user.id] = msg.id
                    Timer().schedule(1000 * 60 * 60) {
                        welcomeMessages.takeIf { it.containsKey(event.user.id) }?.remove(event.user.id)
                    }
                }
            }
        }

        handleReJoinMute(event.guild, event.user, configuration, logger)
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
        insertLeave(e.user.id, DateTime(1000 * e.member.joinDate.toEpochSecond()), now, e.guild.id)
    }

    private fun buildJoinMessage(response: String, image: String, title: String) =
        EmbedBuilder()
            .setTitle(title)
            .setDescription(response)
            .setColor(Color.red)
            .setThumbnail(image)
            .addField("How do I start?", messageService.messages.welcomeDescription, false)
            .build()
}



