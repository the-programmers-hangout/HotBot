package me.aberrantfox.hotbot.listeners.antispam

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.permMuteMember
import me.aberrantfox.kjdautils.api.annotation.Data
import me.aberrantfox.kjdautils.extensions.jda.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.joda.time.DateTime


@Data("config/raiders.json")
data class Raiders(val set: HashSet<String> = HashSet())

object SecuritySettings {
    var matchCount = 6
    var waitPeriod = 10
    var maxAmount = 5
}

class DuplicateMessageListener (val config: Configuration,
                                val loggingService: LoggingService,
                                val raiders: Raiders,
                                private val tracker: MessageCache) {
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

        if(tracker.count(id) < SecuritySettings.waitPeriod)  return
        if(matches < SecuritySettings.matchCount) return

        raiders.set.add(id)
        val reason = "Automatic mute for duplicate-spam detection."
        punish(member, reason, id)
    }

    private fun checkSpeed(id: String, member: Member) {
        if(raiders.set.contains(id)) return

        val amount = tracker.list(id)
            ?.count { it.time.isAfter(DateTime.now().minusSeconds(5)) }
            ?: return

        if(SecuritySettings.maxAmount <= amount) {
            raiders.set.add(id)
            val reason = "Automatic mute for repeat-spam detection."
            punish(member, reason, id)
        }
    }

    private fun punish(member: Member, reason: String, id: String) {
        permMuteMember(member, reason, config, loggingService.logInstance)

        tracker.list(id)?.forEach { it.message.delete().queue() }

        loggingService.logInstance.alert("${member.descriptor()} was muted for $reason")
        tracker.removeUser(id)
    }
}
