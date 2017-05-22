package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.businessobjects.Configuration
import me.aberrantfox.aegeus.commandframework.getHighestPermissionLevel
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method

data class CommandListener(val jda: JDA,
                           val config: Configuration,
                           val commandMap: Map<String, Method>): ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if(event != null && event.message != null && event.message.rawContent.startsWith(config.prefix)) {
            val rawMessage = event.message.rawContent
            val commandName = getCommandName(rawMessage).toLowerCase()

            if(commandMap.containsKey(commandName)) {
                val method = commandMap[commandName]
                val userPermissionLevel = getHighestPermissionLevel(event.member.roles, config)
                val commandPermissionLevel = config.commandPermissionMap[commandName]

                if(commandPermissionLevel == null) {
                    return
                }

                if(userPermissionLevel < commandPermissionLevel) {
                    event.channel.sendMessage(":wrong_again_idiot: Do you really think I would let you do that").queue()
                    return
                }

                when (method?.parameterCount) {
                    1 -> method?.invoke(null, event)
                    2 -> method?.invoke(null, event, config)
                }

            } else {
                event.channel.sendMessage(":wrong_again_idiot:").queue()
            }
        }
    }

    fun getCommandName(message: String): String {
        val trimmedMessage = message.substring(config.prefix.length)

        if(! (message.contains(" ")) ) {
            return trimmedMessage
        }

        return message.substring(0, message.indexOf(" "))
    }
}
