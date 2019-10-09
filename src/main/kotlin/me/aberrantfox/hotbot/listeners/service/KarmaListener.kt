package me.aberrantfox.hotbot.listeners.service

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class KarmaListener(val loggingService: LoggingService,
                    val config: Configuration,
                    val messages: Messages,
                    val karmaService: KarmaService,
                    val databaseService: DatabaseService) {
    private val waitingUsers = ConcurrentHashMap.newKeySet<String>()

    @Subscribe
    fun onGuildMessageReceivedEvent(event: GuildMessageReceivedEvent) {
        if(event.author.isBot) return

        if(waitingUsers.contains(event.author.id)) return

        if(config.security.ignoredIDs.contains(event.author.id)) return

        val member = event.member ?: return

        val message = event.message
        val karmaResult = karmaService.isKarmaMessage(message)


        if(karmaResult is Positive) {
            databaseService.karma.addKarma(karmaResult.member.user, 1)
            loggingService.logInstance.info("${message.author.fullName()} gave ${karmaResult.member.fullName()} 1 karma")

            event.channel.sendMessage(messages.karmaMessage.replace("%mention%", karmaResult.member.asMention)).queue()
            waitingUsers.add(member.user.id)

            Timer().schedule(object : TimerTask(){
                override fun run() {
                    waitingUsers.remove(member.user.id)
                }
            }, config.serverInformation.karmaGiveDelay.toLong())
        }
    }

    @Subscribe
    fun leaveEvent(event: GuildMemberLeaveEvent) = databaseService.karma.removeKarma(event.user)
}