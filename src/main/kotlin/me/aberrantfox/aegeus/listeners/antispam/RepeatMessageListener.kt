package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.commandframework.commands.SecurityLevelState
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.permMuteMember
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.MessageTracker
import me.aberrantfox.aegeus.services.PersistentList
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object MutedRaiders {
    val list = PersistentList("raiders.json")
}

class RepeatMessageListener (val config: Configuration, val log: TextChannel) : ListenerAdapter() {
    private val tracker = MessageTracker(1)

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.member.roles.size > 0) return
        if(event.author.isBot) return

        val id = event.author.id
        val matches = tracker.addMessage(event.message)

        if(tracker.count(id) > SecurityLevelState.alertLevel.waitPeriod) {
            if(matches >= SecurityLevelState.alertLevel.matchCount) {
                MutedRaiders.list.add(id)
                val reason = "Automatic mute for spam detection due to security level ${SecurityLevelState.alertLevel.name}"
                permMuteMember(event.guild, event.author, reason, config, event.jda.selfUser)

                tracker.list(id)?.forEach { it.delete().queue() }

                log.sendMessage("${event.author.fullName()} was muted for $reason").queue()
                tracker.removeUser(id)
            }
        }
    }
}