package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.commandframework.commands.SecurityLevelState
import me.aberrantfox.aegeus.services.MessageTracker
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class RepeatMessageListener : ListenerAdapter() {
    private val tracker = MessageTracker(1)

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if(event.author.isBot) return

        val id = event.author.id
        val matches = tracker.addMessage(id, event.message.rawContent)

        if(tracker.count(id) > SecurityLevelState.alertLevel.waitPeriod) {
            if(matches >= SecurityLevelState.alertLevel.matchCount) {
                event.channel.sendMessage("${SecurityLevelState.alertLevel.matchCount} or more found").queue()
            }
        }
    }
}