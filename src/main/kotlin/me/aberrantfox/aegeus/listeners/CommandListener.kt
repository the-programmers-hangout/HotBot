package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.commands.macroMap
import me.aberrantfox.aegeus.services.CommandRecommender
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method

data class CommandEvent(val guildEvent: GuildMessageReceivedEvent,
                        val args: List<Any>,
                        val config: Configuration,
                        val jda: JDA,
                        val channel: MessageChannel,
                        val author: User,
                        val message: Message)

data class CommandListener(val config: Configuration, val commandMap: Map<String, Method>): ListenerAdapter() {
    init {
        CommandRecommender.addAll(commandMap.keys.toList() + macroMap.keys.toList())
    }

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        if(!isUsableEvent(event.message, event.author.id, event.channel.id, event.author.isBot)) return
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(!isUsableEvent(event.message, event.author.id, event.channel.id, event.author.isBot)) return

        val rawMessage = event.message.rawContent
        val (commandName, actualArgs) = getCommandStruct(rawMessage, config)

        if(commandMap.containsKey(commandName)) {
            respondToCommand(commandName, actualArgs, event)
            return
        }

        if (macroMap.containsKey(commandName)) {
            event.channel.sendMessage(macroMap[commandName]).queue()
            return
        }

        val recommended = CommandRecommender.recommendCommand(commandName)
        event.channel.sendMessage("I don't know what ${commandName.replace("@", "")} is, perhaps you meant $recommended?").queue()
    }

    private fun respondToCommand(commandName: String, actual: List<String>, event: GuildMessageReceivedEvent) {
        val method = commandMap[commandName]?: return
        val userPermissionLevel = getHighestPermissionLevel(event.member.roles, config)
        val commandPermissionLevel = config.commandPermissionMap[commandName] ?: return
        val annotation = method.getAnnotation(Command::class.java)

        if( !(isValidCommandInvocation(userPermissionLevel, commandPermissionLevel, annotation, event, actual)) ) return

        val parsedArgs = convertArguments(actual, annotation.expectedArgs, event.jda)

        if( parsedArgs == null ) {
            event.channel.sendMessage(":unamused: Yea, you'll need to learn how to use that properly.").queue()
            return
        }

        if (method.parameterCount == 0) {
            method.invoke(null)
        } else {
            method.invoke(null, CommandEvent(event, parsedArgs, config, event.jda, event.channel, event.author, event.message))
        }
    }

    private fun isValidCommandInvocation(userPermission: Permission, commandPermission: Permission, cmd: Command,
                                         event: GuildMessageReceivedEvent, actual: List<String>): Boolean {
        if(userPermission < config.mentionFilterLevel
                && (event.message.mentionsEveryone() || event.message.mentionedUsers.size > 0 || event.message.mentionedRoles.size > 0)) {
            event.channel.sendMessage("Your permission level is below the required level to use a command mention.").queue()
            return false
        }

        if(userPermission < commandPermission) {
            event.channel.sendMessage(":unamused: Do you really think I would let you do that").queue()
            return false
        }

        if (cmd.expectedArgs.contains(ArgumentType.Joiner)) {
            if(actual.size < cmd.expectedArgs.size) {
                event.channel.sendMessage("You didn't enter the minimum amount of required arguments.").queue()
                return false
            }
        } else {
            if(actual.size != cmd.expectedArgs.size) {
                if(!cmd.expectedArgs.contains(ArgumentType.Manual)) {
                    event.channel.sendMessage("This command requires ${cmd.expectedArgs.size} arguments.").queue()
                    return false
                }
            }
        }

        return true
    }

    private fun isUsableEvent(message: Message, author: String, channel: String, isBot: Boolean): Boolean {
        if(message.rawContent.length > 1500) return false

        if(config.lockDownMode && author != config.ownerID) return false

        if( !(message.rawContent.startsWith(config.prefix)) ) return false

        if(config.ignoredIDs.contains(channel) || config.ignoredIDs.contains(author)) return false

        if(isBot) return false

        return true
    }
}

