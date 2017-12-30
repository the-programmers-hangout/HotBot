package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.commandframework.commands.dsl.Command
import me.aberrantfox.aegeus.commandframework.commands.dsl.CommandEvent
import me.aberrantfox.aegeus.commandframework.commands.dsl.CommandsContainer
import me.aberrantfox.aegeus.commandframework.commands.dsl.arg
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.commands.macroMap
import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.permissions.Permission
import me.aberrantfox.aegeus.permissions.getHighestPermissionLevel
import me.aberrantfox.aegeus.services.CommandRecommender
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

data class CommandListener(val config: Configuration,
                           val container: CommandsContainer,
                           val jda: JDA,
                           val logChannel: MessageChannel,
                           val guild: Guild) : ListenerAdapter() {
    init {
        CommandRecommender.addAll(container.commands.keys.toList() + macroMap.keys.toList())
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author, true)

    override fun onPrivateMessageReceived(e: PrivateMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author, false)

    private fun handleInvocation(channel: MessageChannel, message: Message, author: User, invokedInGuild: Boolean) {
        if ( !(isUsableEvent(message, channel.id, author)) ) return

        val (commandName, actualArgs) = getCommandStruct(message.contentRaw, config)
        val permission = getHighestPermissionLevel(guild, config, jda, author.id)

        if (!(isValidCommand(permission, channel, message))) return

        val command = container.get(commandName)

        when {
            command != null -> {
                invokeCommand(command, commandName, actualArgs, message, author, permission, invokedInGuild)
                logChannel.sendMessage("${author.descriptor()} -- invoked $commandName in ${channel.name}").queue()
            }
            macroMap.containsKey(commandName) -> {
                channel.sendMessage(macroMap[commandName]).queue()
                logChannel.sendMessage("${author.descriptor()} -- invoked $commandName in ${channel.name}").queue()
            }
            else -> {
                val recommended = CommandRecommender.recommendCommand(commandName)
                channel.sendMessage("I don't know what ${commandName.replace("@", "")} is, perhaps you meant $recommended?").queue()
            }
        }

        if (invokedInGuild) handleDelete(message, config.prefix)
    }

    private fun invokeCommand(command: Command, name: String, actual: List<String>, message: Message, author: User,
                              permission: Permission, invokedInGuild: Boolean) {
        val channel = message.channel
        val commandPermissionLevel = config.commandPermissionMap[name] ?: return

        if (permission < commandPermissionLevel) {
            channel.sendMessage(":unamused: Do you really think I would let you do that").queue()
            return
        }

        if (!(argsMatch(actual, command, channel))) return

        val parsedArgs = convertArguments(actual, command.expectedArgs.map { it.type }.toList(), jda)

        if (parsedArgs == null) {
            channel.sendMessage(":unamused: Yea, you'll need to learn how to use that properly.").queue()
            return
        }

        val event = CommandEvent(actual, config, jda, channel, author, message, guild)

        if (command.parameterCount == 0) {
            command.execute(event)
            return
        }

        if (command.requiresGuild && !invokedInGuild) {
            channel.sendMessage("This command must be invoked in a guild channel, and not through PM").queue()
        } else {
            command.execute(event)
        }
    }

    private fun isUsableEvent(message: Message, channel: String, author: User): Boolean {
        if (message.contentRaw.length > 1500) return false

        if (config.lockDownMode && author.id != config.ownerID) return false

        if (!(message.isCommandInvocation(config))) return false

        if (config.ignoredIDs.contains(channel) || config.ignoredIDs.contains(author.id)) return false

        if (author.isBot) return false

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

    private fun handleDelete(message: Message, prefix: String) =
        if (!message.rawContent.startsWith(prefix + prefix)) {
            message.deleteIfExists()
        } else {
            Unit
        }

    private fun argsMatch(actual: List<String>, cmd: Command, channel: MessageChannel): Boolean {
        if (cmd.expectedArgs.contains(arg(ArgumentType.Joiner)) || cmd.expectedArgs.contains(arg(ArgumentType.Splitter))) {
            if (actual.size < cmd.expectedArgs.size) {
                channel.sendMessage("You didn't enter the minimum amount of required arguments.").queue()
                return false
            }
        } else {
            if (actual.size != cmd.expectedArgs.size) {
                if (!cmd.expectedArgs.contains(arg(ArgumentType.Manual))) {
                    channel.sendMessage("This command requires ${cmd.expectedArgs.size} arguments.").queue()
                    return false
                }
            }
        }

        return true
    }
}

