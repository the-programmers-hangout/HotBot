package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.commands.macroMap
import me.aberrantfox.aegeus.services.CommandRecommender
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method

data class CommandListener(val config: Configuration,
                           val commandMap: Map<String, Method>): ListenerAdapter() {
    init {
        CommandRecommender.addAll(commandMap.keys.toList() + macroMap.keys.toList())
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(!isUsableEvent(event)) return

        val rawMessage = event.message.rawContent
        val (commandName, actualArgs) = getCommandStruct(rawMessage, config)

        if(commandMap.containsKey(commandName)) {
            val method = commandMap[commandName]?: return
            val userPermissionLevel = getHighestPermissionLevel(event.member.roles, config)
            val commandPermissionLevel = config.commandPermissionMap[commandName] ?: return
            val annotation = method.getAnnotation(Command::class.java)

            if(userPermissionLevel < config.mentionFilterLevel
                    && (event.message.mentionsEveryone() || event.message.mentionedUsers.size > 0 || event.message.mentionedRoles.size > 0)) {
                event.channel.sendMessage("Your permission level is below the required level to use a command mention.").queue()
                return
            }

            if(userPermissionLevel < commandPermissionLevel) {
                event.channel.sendMessage(":unamused: Do you really think I would let you do that").queue()
                return
            }

            if (annotation.expectedArgs.contains(ArgumentType.Joiner)) {
                if(actualArgs.size < annotation.expectedArgs.size) {
                    event.channel.sendMessage("You didn't enter the minimum amount of required arguments.").queue()
                    return
                }
            } else {
                if(actualArgs.size != annotation.expectedArgs.size) {
                    if(!annotation.expectedArgs.contains(ArgumentType.Manual)) {
                        event.channel.sendMessage("This command requires ${annotation.expectedArgs.size} arguments.").queue()
                        return
                    }
                }
            }

            val convertedArguments = convertArguments(actualArgs, annotation.expectedArgs, event.jda)

            if( convertedArguments == null ) {
                event.channel.sendMessage(":unamused: Yea, you'll need to learn how to use that properly.").queue()
                return
            }

            when (method.parameterCount) {
                0 -> method.invoke(null)
                1 -> method.invoke(null, event)
                2 -> method.invoke(null, event, convertedArguments)
                3 -> method.invoke(null, event, convertedArguments, config)
            }

            return
        } else if (macroMap.containsKey(commandName)) {
            event.channel.sendMessage(macroMap[commandName]).queue()
            return
        }

        val recommended = CommandRecommender.recommendCommand(commandName)
        event.channel.sendMessage("I don't know what $commandName is, did you mean $recommended").queue()
    }

    private fun isUsableEvent(event: GuildMessageReceivedEvent): Boolean {
        if(config.lockDownMode && event.author.id != config.ownerID) return false

        if( !(event.message.rawContent.startsWith(config.prefix)) ) return false

        if(config.ignoredIDs.contains(event.channel.id) || config.ignoredIDs.contains(event.author.id)) return false

        if(event.author.isBot) return false

        return true
    }
}

