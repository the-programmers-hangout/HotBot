package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.getCommandStruct
import me.aberrantfox.aegeus.commandframework.getHighestPermissionLevel
import me.aberrantfox.aegeus.commandframework.convertArguments
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method

data class CommandListener(val config: Configuration,
                           val commandMap: Map<String, Method>): ListenerAdapter() {

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.author.isBot) {
            return
        }

        val rawMessage = event.message.rawContent
        val (commandName, actualArgs) = getCommandStruct(rawMessage, config)

        if(commandMap.containsKey(commandName)) {
            val method = commandMap[commandName]?: return
            val userPermissionLevel = getHighestPermissionLevel(event.member.roles, config)
            val commandPermissionLevel = config.commandPermissionMap[commandName] ?: return
            val annotation = method.getAnnotation(Command::class.java)

            if(userPermissionLevel < commandPermissionLevel) {
                event.channel.sendMessage(":wrong_again_idiot: Do you really think I would let you do that").queue()
                return
            }

            val convertedArguments = convertArguments(actualArgs, annotation.expectedArgs)

            if( convertedArguments == null ) {
                event.channel.sendMessage(":wrong_again_idiot: Yea, you'll need to learn how to use that properly.").queue()
                return
            }

            when (method.parameterCount) {
                0 -> method.invoke(null)
                1 -> method.invoke(null, event)
                2 -> method.invoke(null, event, convertedArguments)
                3 -> method.invoke(null, event, convertedArguments, config)
            }

        } else {
            event.channel.sendMessage(":wrong_again_idiot: I don't know what that command is").queue()
        }

    }
}
