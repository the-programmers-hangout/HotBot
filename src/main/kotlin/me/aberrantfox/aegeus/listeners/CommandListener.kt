package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.commands.macroMap
import me.aberrantfox.aegeus.services.CommandRecommender
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method

data class CommandEvent(val args: List<Any>,
                        val config: Configuration,
                        val jda: JDA,
                        val channel: MessageChannel,
                        val author: User,
                        val message: Message,
                        val guild: Guild?)

data class CommandListener(val config: Configuration, val commandMap: Map<String, Method>, val jda: JDA): ListenerAdapter() {

    init {
        CommandRecommender.addAll(commandMap.keys.toList() + macroMap.keys.toList())
    }

    override fun onPrivateMessageReceived(e: PrivateMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author)

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author, e.guild)

    private fun handleInvocation(channel: MessageChannel, message: Message, author: User, guild: Guild? = null) {
        if(!isUsableEvent(message, author.id, channel.id, author.isBot)) return

        val rawMessage = message.rawContent
        val (commandName, actualArgs) = getCommandStruct(rawMessage, config)

        if(commandMap.containsKey(commandName)) {
            respondToCommand(commandName, actualArgs, channel, message, author, guild)
            return
        }

        if (macroMap.containsKey(commandName)) {
            channel.sendMessage(macroMap[commandName]).queue()
            return
        }

        val recommended = CommandRecommender.recommendCommand(commandName)
        channel.sendMessage("I don't know what ${commandName.replace("@", "")} is, perhaps you meant $recommended?").queue()
    }

    private fun respondToCommand(commandName: String, actual: List<String>, channel: MessageChannel, message: Message,
                                 author: User, guild: Guild? = null) {
        val method = commandMap[commandName]?: return
        val userPermissionLevel = getHighestPermissionLevel(guild, config, jda)
        val commandPermissionLevel = config.commandPermissionMap[commandName] ?: return
        val annotation = method.getAnnotation(Command::class.java)

        if( !(isValidCommandInvocation(userPermissionLevel, commandPermissionLevel, annotation, channel, actual, message)) ) return


        val parsedArgs = convertArguments(actual, annotation.expectedArgs, jda)

        if( parsedArgs == null ) {
            channel.sendMessage(":unamused: Yea, you'll need to learn how to use that properly.").queue()
            return
        }

        if (method.parameterCount == 0) {
            method.invoke(null)
        } else {
            method.invoke(null, CommandEvent(parsedArgs, config, jda, channel, author, message, guild))
        }
    }

    private fun isUsableEvent(message: Message, author: String, channel: String, isBot: Boolean): Boolean {
        if(message.rawContent.length > 1500) return false

        if(config.lockDownMode && author != config.ownerID) return false

        if( !(message.rawContent.startsWith(config.prefix)) ) return false

        if(config.ignoredIDs.contains(channel) || config.ignoredIDs.contains(author)) return false

        if(isBot) return false

        return true
    }

    private fun isValidCommandInvocation(userPermission: Permission, commandPermission: Permission, cmd: Command,
                                         channel: MessageChannel, actual: List<String>, message: Message): Boolean {
        if(userPermission < config.mentionFilterLevel
                && (message.mentionsEveryone() || message.mentionedUsers.size > 0 || message.mentionedRoles.size > 0)) {
            channel.sendMessage("Your permission level is below the required level to use a command mention.").queue()
            return false
        }

        if(userPermission < commandPermission) {
            channel.sendMessage(":unamused: Do you really think I would let you do that").queue()
            return false
        }

        if (cmd.expectedArgs.contains(ArgumentType.Joiner)) {
            if(actual.size < cmd.expectedArgs.size) {
                channel.sendMessage("You didn't enter the minimum amount of required arguments.").queue()
                return false
            }
        } else {
            if(actual.size != cmd.expectedArgs.size) {
                if(!cmd.expectedArgs.contains(ArgumentType.Manual)) {
                    channel.sendMessage("This command requires ${cmd.expectedArgs.size} arguments.").queue()
                    return false
                }
            }
        }

        return true
    }
}

