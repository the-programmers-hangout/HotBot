package me.aberrantfox.hotbot.listeners

import me.aberrantfox.hotbot.commandframework.*
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.commandframework.commands.macroMap
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.extensions.*
import me.aberrantfox.hotbot.logging.BotLogger
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.CommandRecommender
import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.hotbot.services.UserID
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

data class CommandListener(val config: Configuration,
                           val container: CommandsContainer,
                           val jda: JDA,
                           val log: BotLogger,
                           val guild: Guild,
                           val manager: PermissionManager,
                           val mService: MService) : ListenerAdapter() {
    init {
        CommandRecommender.addAll(container.commands.keys.toList() + macroMap.keys.toList())
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author, true)

    override fun onPrivateMessageReceived(e: PrivateMessageReceivedEvent) = handleInvocation(e.channel, e.message, e.author, false)

    private fun handleInvocation(channel: MessageChannel, message: Message, author: User, invokedInGuild: Boolean) {
        if ( !(isUsableEvent(message, channel.id, author)) ) return

        val (commandName, actualArgs) = getCommandStruct(message.contentRaw, config)

        if (!(isValidCommand(channel, message, author.id))) return

        val command = container.get(commandName)

        when {
            command != null -> {
                invokeCommand(command, commandName, actualArgs, message, author, invokedInGuild)
                log.cmd("${author.descriptor()} -- invoked $commandName in ${channel.name}")
            }
            macroMap.containsKey(commandName) -> {
                channel.sendMessage(macroMap[commandName]).queue()
                log.cmd("${author.descriptor()} -- invoked $commandName in ${channel.name}")
            }
            else -> {
                val recommended = CommandRecommender.recommendCommand(commandName)
                channel.sendMessage("I don't know what ${commandName.replace("@", "")} is, perhaps you meant $recommended?").queue()
            }
        }

        if (invokedInGuild) handleDelete(message, config.serverInformation.prefix)
    }

    private fun invokeCommand(command: Command, name: String, actual: List<String>, message: Message, author: User, invokedInGuild: Boolean) {
        val channel = message.channel

        if( !(manager.canUseCommand(author.id, name)) ) {
            channel.sendMessage("Did you really think I would let you do that? :thinking:").queue()
            return
        }

        if (!(argsMatch(actual, command, channel))) return

        val event = CommandEvent(config, jda, channel, author, message, guild, manager, container, mService, actual)
        convertAndQueue(actual, command.expectedArgs.map { it.type }.toList(), this, event, invokedInGuild, command, config)
    }

    fun executeEvent(command: Command, event: CommandEvent, invokedInGuild: Boolean) {
        if (command.parameterCount == 0) {
            command.execute(event)
            return
        }

        if (command.requiresGuild && !invokedInGuild) {
            event.respond("This command must be invoked in a guild channel, and not through PM")
        } else {
            command.execute(event)
        }
    }

    private fun isUsableEvent(message: Message, channel: String, author: User): Boolean {
        if (message.contentRaw.length > 1500) return false

        if (config.security.lockDownMode && author.id != config.serverInformation.ownerID) return false

        if (!(message.isCommandInvocation(config))) return false

        if (config.security.ignoredIDs.contains(channel) || config.security.ignoredIDs.contains(author.id)) return false

        if (author.isBot) return false

        return true
    }

    private fun isValidCommand(channel: MessageChannel, message: Message, userID: UserID): Boolean {
        if (!manager.canPerformAction(userID, config.permissionedActions.commandMention) && message.mentionsSomeone()) {
            channel.sendMessage("Your permission level is below the required level to use a command mention.").queue()
            return false
        }

        if (!manager.canPerformAction(userID, config.permissionedActions.sendInvite) && message.containsInvite()) {
            channel.sendMessage("Ayyy lmao. Nice try, try that again. I dare you. :rllynow:").queue()
            return false
        }

        if (!manager.canPerformAction(userID, config.permissionedActions.sendURL) && message.containsURL()) {
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
        val optionalCount = cmd.expectedArgs.filter { it.optional }.size

        if (cmd.expectedArgs.contains(arg(ArgumentType.Sentence)) || cmd.expectedArgs.contains(arg(ArgumentType.Splitter))) {
            if (actual.size < cmd.expectedArgs.size - optionalCount) {
                channel.sendMessage("You didn't enter the minimum number of required arguments: ${cmd.expectedArgs.size - optionalCount}.").queue()
                return false
            }
        } else {
            if(!(actual.size >= (cmd.expectedArgs.size - optionalCount)
                            && (actual.size <= cmd.expectedArgs.size))) {
                if (!cmd.expectedArgs.contains(arg(ArgumentType.Manual))) {
                    channel.sendMessage("This command requires at least ${cmd.expectedArgs.size - optionalCount} and a maximum of ${cmd.expectedArgs.size} arguments.").queue()
                    return false
                }
            }
        }

        return true
    }
}

