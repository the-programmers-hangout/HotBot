package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.commands.administration.SecurityLevelState
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.permMuteMember
import me.aberrantfox.hotbot.utility.types.PersistentSet
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.joda.time.DateTime

object MutedRaiders {
    val set = PersistentSet(configPath("raiders.json"))
}

class DuplicateMessageListener (val config: Configuration,
                                val log: BotLogger,
                                private val tracker: MessageTracker) {

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.message.isImagePost()) return

        val member = event.member ?: return

        val time = DateTime.now()

        if(member.roles.size > 0) return
        if(event.author.isBot) return

        val id = event.author.id
        val matches = tracker.addMessage(AccurateMessage(time, event.message))

        checkDuplicates(id, member, matches)
        checkSpeed(id, member)
    }

    private fun checkDuplicates(id: String, member: Member, matches: Int) {

        if(tracker.count(id) < SecurityLevelState.alertLevel.waitPeriod)  return
        if(matches <  SecurityLevelState.alertLevel.matchCount) return

        MutedRaiders.set.add(id)
        val reason = "Automatic mute for duplicate-spam detection due to security level ${SecurityLevelState.alertLevel.name}"
        punish(member, reason, id)
    }

    private fun checkSpeed(id: String, member: Member) {
        if(MutedRaiders.set.contains(id)) return

        val maxAmount = SecurityLevelState.alertLevel.maxAmount

        val amount = tracker.list(id)
            ?.count { it.time.isAfter(DateTime.now().minusSeconds(5)) }
            ?: return

        if(maxAmount <= amount) {
            MutedRaiders.set.add(id)
            val reason = "Automatic mute for repeat-spam detection due to security level ${SecurityLevelState.alertLevel.name}"
            punish(member, reason, id)
        }
    }

    private fun punish(member: Member, reason: String, id: String) {
        permMuteMember(member, reason, config, log)

        tracker.list(id)?.forEach { it.message.delete().queue() }

        log.alert("${member.descriptor()} was muted for $reason")
        tracker.removeUser(id)
    }
}
