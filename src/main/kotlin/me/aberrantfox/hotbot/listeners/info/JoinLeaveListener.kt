package me.aberrantfox.hotbot.listeners.info

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.commands.administration.sendWelcome
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.DatabaseService
import me.aberrantfox.hotbot.services.LoggingService
import me.aberrantfox.hotbot.services.Messages
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.stdlib.formatJdaDate
import me.aberrantfox.kjdautils.extensions.stdlib.randomListItem
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import org.joda.time.DateTime
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

typealias MessageID = String
typealias UserID = String

class MemberListener(val configuration: Configuration,
                     val loggingService: LoggingService,
                     val databaseService: DatabaseService,
                     val messages: Messages) {
    private val welcomeMessages = ConcurrentHashMap<UserID, MessageID>()

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {

        val secsSinceCreation = (System.currentTimeMillis() / 1000) - event.user.timeCreated.toEpochSecond()
        val numOfDays = TimeUnit.DAYS.convert(secsSinceCreation, TimeUnit.SECONDS).toInt()
        val user = "${event.user.fullName()} :: ${event.user.asMention}"
        val date = event.user.timeCreated.toString().formatJdaDate()
        val rejoin = databaseService.guildLeaves.hasLeaveHistory(event.user.id, event.guild.id)
        val newUserThreshold = 5

        loggingService.logInstance.info("$user created $numOfDays days ago ($date) -- ${if (rejoin) "re" else ""}joined the server")

        if (numOfDays <= newUserThreshold)
            loggingService.logInstance.alert("$user has joined the server but the account has only existed for $numOfDays day${if (numOfDays == 1) "" else "s"}. Potential action required.")
        if(sendWelcome){
            //Build welcome message
            val target = event.guild.textChannels.findLast { it.id == configuration.messageChannels.welcomeChannel }
            val response = messages.onJoin.randomListItem().replace("%name%", "${event.user.asMention}(${event.user.fullName()})")
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

    }
    @Subscribe
    fun onGuildMemberLeave(e: GuildMemberLeaveEvent) {
        loggingService.logInstance.info("${e.user.fullName()} :: ${e.user.asMention} left the server")

        val welcomeMessageID = welcomeMessages[e.user.id]

        if(configuration.serverInformation.deleteWelcomeOnLeave && welcomeMessageID != null) {
            e.jda.getTextChannelById(configuration.messageChannels.welcomeChannel)?.retrieveMessageById(welcomeMessageID)?.queue {
                it.delete().queue()
            } ?: loggingService.logInstance.error("Couldn't retrieve welcome channel to delete welcome embed because of member leave.")
        }

        val now = DateTime.now()
        databaseService.guildLeaves.insertLeave(e.user.id, DateTime(1000 * e.member.timeJoined.toEpochSecond()), now, e.guild.id)
    }

    private fun buildJoinMessage(response: String, image: String, title: String) =
        EmbedBuilder()
            .setTitle(title)
            .setDescription(response)
            .setColor(Color.red)
            .setThumbnail(image)
            .addField("How do I start?", messages.welcomeDescription, false)
            .build()
}


