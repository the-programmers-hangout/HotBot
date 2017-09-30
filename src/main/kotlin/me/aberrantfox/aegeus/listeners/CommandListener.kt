package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.commands.macroMap
import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.services.CommandRecommender
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Method

data class CommandListener(val config: Configuration, val commandMap: Map<String, Method>, val jda: JDA,
                           val logChannel: MessageChannel) : ListenerAdapter() {
    init {
        CommandRecommender.addAll(commandMap.keys.toList() + macroMap.keys.toList())
    }

    override fun onPrivateMessageReceived(e: PrivateMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author)

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author, e.guild)

    private fun handleInvocation(channel: MessageChannel, message: Message, author: User, guild: Guild? = null) {
        if (!isUsableEvent(message, author.id, channel.id, author.isBot)) return

        val (commandName, actualArgs) = getCommandStruct(message.rawContent, config)
        val userPermissionLevel = getHighestPermissionLevel(guild, config, jda, author.id)

        if (!(isValidCommand(userPermissionLevel, channel, message))) return

        if (commandMap.containsKey(commandName)) {
            respondToCommand(commandName, actualArgs, channel, message, author, guild, userPermissionLevel)
            logChannel.sendMessage("${author.descriptor()} -- invoked $commandName in ${channel.name}").queue()
            if(guild != null) handleDelete(message, config.prefix)
            return
        }

        if (macroMap.containsKey(commandName)) {
            channel.sendMessage(macroMap[commandName]).queue()
            logChannel.sendMessage("${author.descriptor()} -- invoked $commandName in ${channel.name}").queue()
            if(guild != null) handleDelete(message, config.prefix)
            return
        }

        val recommended = CommandRecommender.recommendCommand(commandName)
        channel.sendMessage("I don't know what ${commandName.replace("@", "")} is, perhaps you meant $recommended?").queue()
    }

    private fun respondToCommand(commandName: String, actual: List<String>, channel: MessageChannel, message: Message,
                                 author: User, guild: Guild? = null, userPermissionLevel: Permission) {
        val method = commandMap[commandName] ?: return
        val commandPermissionLevel = config.commandPermissionMap[commandName] ?: return
        val annotation = method.getAnnotation(Command::class.java)
        val requiresGuild = method.getAnnotation(RequiresGuild::class.java)

        if (userPermissionLevel < commandPermissionLevel) {
            channel.sendMessage(":unamused: Do you really think I would let you do that").queue()
            return
        }

        if (!(argsMatch(actual, annotation, channel))) return

        val parsedArgs = convertArguments(actual, annotation.expectedArgs, jda)

        if (parsedArgs == null) {
            channel.sendMessage(":unamused: Yea, you'll need to learn how to use that properly.").queue()
            return
        }

        if (method.parameterCount == 0) {
            method.invoke(null)
            handleDelete(message, config.prefix)
            return
        }

        if (requiresGuild != null && !requiresGuild.useDefault && guild == null) {
            channel.sendMessage("This command must be invoked in a guild channel, and not through PM").queue()
        } else if (requiresGuild != null && requiresGuild.useDefault && guild == null) {
            method.invoke(null, CommandEvent(parsedArgs, config, jda, channel, author, message, jda.getGuildById(config.guildid)))
        } else {
            method.invoke(null, CommandEvent(parsedArgs, config, jda, channel, author, message, guild))
        }
    }

    private fun isUsableEvent(message: Message, author: String, channel: String, isBot: Boolean): Boolean {
        if (message.rawContent.length > 1500) return false

        if (config.lockDownMode && author != config.ownerID) return false

        if (!(message.isCommandInvocation(config))) return false

        if (config.ignoredIDs.contains(channel) || config.ignoredIDs.contains(author)) return false

        if (isBot) return false

        return true
    }

    private fun isValidCommand(userPerm: Permission, channel: MessageChannel, message: Message): Boolean {
        if (userPerm < config.mentionFilterLevel && message.mentionsSomeone()) {
            channel.sendMessage("Your permission level is below the required level to use a command mention.").queue()
            return false
        }

        if (userPerm < config.invitePermissionLevel && message.containsInvite()) {
            channel.sendMessage("Ayyy lmao. Nice try, try that again. I dare you. :rllynow:").queue()
            return false
        }

        if (userPerm < config.urlFilterPermissionLevel && message.containsURL()) {
            channel.sendMessage("Your permission level is below the required level to use a URL in a command.").queue()
            return false
        }

        return true
    }

    private fun argsMatch(actual: List<String>, cmd: Command, channel: MessageChannel): Boolean {
        if (cmd.expectedArgs.contains(ArgumentType.Joiner) || cmd.expectedArgs.contains(ArgumentType.Splitter)) {
            if (actual.size < cmd.expectedArgs.size) {
                channel.sendMessage("You didn't enter the minimum amount of required arguments.").queue()
                return false
            }
        } else {
            if (actual.size != cmd.expectedArgs.size) {
                if (!cmd.expectedArgs.contains(ArgumentType.Manual)) {
                    channel.sendMessage("This command requires ${cmd.expectedArgs.size} arguments.").queue()
                    return false
                }
            }
        }

        return true
    }

    private fun handleDelete(message: Message, prefix: String) =
            if (!message.rawContent.startsWith(prefix + prefix)) {
                message.deleteIfExists()
            } else {
                Unit
            }
}

