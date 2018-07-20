package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.database.addKarma
import me.aberrantfox.hotbot.database.removeKarma
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.KarmaService
import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.hotbot.services.Positive
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class KarmaListener(val mService: MService, val log: BotLogger, val config: Configuration) {
    private val karmaService = KarmaService()
    private val waitingUsers = ConcurrentHashMap.newKeySet<String>()

    @Subscribe
    fun onGuildMessageReceivedEvent(event: GuildMessageReceivedEvent) {
        if(event.author.isBot) return

        if(waitingUsers.contains(event.author.id)) return

        if(config.security.ignoredIDs.contains(event.author.id)) return

        val message = event.message
        val karmaResult = karmaService.isKarmaMessage(message)


        if(karmaResult is Positive) {
            addKarma(karmaResult.member.user, 1)
            log.info("${message.author.fullName()} gave ${karmaResult.member.fullName()} 1 karma")

            event.channel.sendMessage(mService.messages.karmaMessage.replace("%mention%", karmaResult.member.asMention)).queue()
            waitingUsers.add(event.member.user.id)

            Timer().schedule(object : TimerTask(){
                override fun run() {
                    waitingUsers.remove(event.member.user.id)
                }
            }, config.serverInformation.karmaGiveDelay.toLong())
        }
    }

    @Subscribe
    fun leaveEvent(event: GuildMemberLeaveEvent) = removeKarma(event.user)
}