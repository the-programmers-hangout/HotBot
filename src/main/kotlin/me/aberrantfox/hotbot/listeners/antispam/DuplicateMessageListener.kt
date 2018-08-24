package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.commands.administration.SecurityLevelState
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.permMuteMember
import me.aberrantfox.kjdautils.extensions.jda.descriptor
import me.aberrantfox.kjdautils.extensions.jda.isImagePost
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.joda.time.DateTime

object MutedRaiders {
    val set = PersistentSet(configPath("raiders.json"))
}

class DuplicateMessageListener (val config: Configuration,
                                val log: BotLogger,
                                val tracker: MessageTracker) {

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.message.isImagePost()) return

        val time = DateTime.now()

        if((event.member?.roles?.size ?: 0) > 0) return
        if(event.author.isBot) return

        val id = event.author.id
        val matches = tracker.addMessage(AccurateMessage(time, event.message))

        checkDuplicates(id, event, matches)
        checkSpeed(id, event)
    }

    private fun checkDuplicates(id: String, event: GuildMessageReceivedEvent, matches: Int) {

        if(tracker.count(id) < SecurityLevelState.alertLevel.waitPeriod)  return
        if(matches <  SecurityLevelState.alertLevel.matchCount) return

        MutedRaiders.set.add(id)
        val reason = "Automatic mute for duplicate-spam detection due to security level ${SecurityLevelState.alertLevel.name}"
        punish(event, reason, id)
    }

    private fun checkSpeed(id: String, event: GuildMessageReceivedEvent) {
        if(MutedRaiders.set.contains(id)) return

        val maxAmount = SecurityLevelState.alertLevel.maxAmount

        val amount = tracker.list(id)
            ?.count { it.time.isAfter(DateTime.now().minusSeconds(5)) }
            ?: return

        if(maxAmount <= amount) {
            MutedRaiders.set.add(id)
            val reason = "Automatic mute for repeat-spam detection due to security level ${SecurityLevelState.alertLevel.name}"
            punish(event, reason, id)
        }
    }

    private fun punish(event: GuildMessageReceivedEvent, reason: String, id: String) {
        permMuteMember(event.guild, event.author, reason, config, log)

        tracker.list(id)?.forEach { it.message.delete().queue() }

        log.alert("${event.author.descriptor()} was muted for $reason")
        tracker.removeUser(id)
    }
}
