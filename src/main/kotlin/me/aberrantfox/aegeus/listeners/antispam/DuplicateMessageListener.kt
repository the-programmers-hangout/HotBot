package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.commandframework.commands.SecurityLevelState
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.permMuteMember
import me.aberrantfox.aegeus.services.AccurateMessage
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.MessageTracker
import me.aberrantfox.aegeus.services.PersistentList
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.joda.time.DateTime

object MutedRaiders {
    val list = PersistentList("raiders.json")
}

class DuplicateMessageListener (val config: Configuration, val log: TextChannel, val tracker: MessageTracker) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val time = DateTime.now()

        if(event.member.roles.size > 0) return
        if(event.author.isBot) return

        val id = event.author.id
        val matches = tracker.addMessage(AccurateMessage(time, event.message))

        checkDuplicates(id, event, matches)
        checkSpeed(id, event)
    }

    private fun checkDuplicates(id: String, event: GuildMessageReceivedEvent, matches: Int) {

        if(tracker.count(id) < SecurityLevelState.alertLevel.waitPeriod)  return
        if(matches <  SecurityLevelState.alertLevel.matchCount) return

        MutedRaiders.list.add(id)
        val reason = "Automatic mute for duplicate-spam detection due to security level ${SecurityLevelState.alertLevel.name}"
        punish(event, reason, id)
    }

    private fun checkSpeed(id: String, event: GuildMessageReceivedEvent) {
        if(MutedRaiders.list.contains(id)) return

        val maxAmount = SecurityLevelState.alertLevel.maxAmount

        val amount = tracker.list(id)
            ?.count { it.time.isAfter(DateTime.now().minusSeconds(5)) }
            ?: return

        if(maxAmount <= amount) {
            MutedRaiders.list.add(id)
            val reason = "Automatic mute for repeat-spam detection due to security level ${SecurityLevelState.alertLevel.name}"
            punish(event, reason, id)
        }
    }

    private fun punish(event: GuildMessageReceivedEvent, reason: String, id: String) {
        permMuteMember(event.guild, event.author, reason, config, event.jda.selfUser)

        tracker.list(id)?.forEach { it.message.delete().queue() }

        log.sendMessage("${event.author.fullName()} was muted for $reason").queue()
        tracker.removeUser(id)
    }
}