package me.aberrantfox.hotbot.commandframework

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import me.aberrantfox.hotbot.commandframework.parsing.cleanCommandMessage
import me.aberrantfox.hotbot.commandframework.commands.utility.macroMap
import me.aberrantfox.hotbot.commandframework.parsing.convertArguments
import me.aberrantfox.hotbot.commandframework.parsing.getArgCountError
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.extensions.jda.*
import me.aberrantfox.hotbot.logging.BotLogger
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.CommandRecommender
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MService
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class CommandExecutor(val config: Configuration,
                      val container: CommandsContainer,
                      val jda: JDA,
                      val log: BotLogger,
                      val manager: PermissionManager,
                      val mService: MService) : ListenerAdapter() {


    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        handleInvocation(e.channel, e.message, e.author, true)
    }

    override fun onPrivateMessageReceived(e: PrivateMessageReceivedEvent) {
        handleInvocation(e.channel, e.message, e.author, false)
    }

    private fun handleInvocation(channel: MessageChannel, message: Message, author: User, invokedInGuild: Boolean) =
            launch(CommonPool) {
                if (!(isUsableCommand(message, channel.id, author))) return@launch

                val (commandName, actualArgs) = cleanCommandMessage(message.contentRaw, config)

                if (!(canPerformCommand(channel, message, author))) return@launch

                val command = container[commandName]

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

        if( !(manager.canUseCommand(author, name)) ) {
            channel.sendMessage("Did you really think I would let you do that? :thinking:").queue()
            return
        }

        getArgCountError(actual, command)?.let {
            channel.sendMessage(it).queue()
            return
        }


        val event = CommandEvent(config, jda, channel, author, message, jda.getGuildById(config.serverInformation.guildid), manager, container, mService, actual)

        val (convertedArgs, conversionError) = convertArguments(actual, command.expectedArgs.toList(), event, config.serverInformation.prefix)
        if (conversionError != null || convertedArgs == null) {
            event.safeRespond(conversionError.toString())
            return
        }

        event.args = convertedArgs.requireNoNulls()

        executeCommand(command, event, invokedInGuild)
    }

    private fun executeCommand(command: Command, event: CommandEvent, invokedInGuild: Boolean) {
        if(isDoubleInvocation(event.message, event.config.serverInformation.prefix)) {
            event.message.addReaction("\uD83D\uDC40").queue()
        }

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

    private fun isUsableCommand(message: Message, channel: String, author: User): Boolean {
        if (message.contentRaw.length > 1500) return false

        if (config.security.lockDownMode && author.id != config.serverInformation.ownerID) return false

        if (!(message.isCommandInvocation(config))) return false

        if (config.security.ignoredIDs.contains(channel) || config.security.ignoredIDs.contains(author.id)) return false

        if (author.isBot) return false

        return true
    }

    private fun canPerformCommand(channel: MessageChannel, message: Message, user: User): Boolean {
        if (!manager.canPerformAction(user, config.permissionedActions.commandMention) && message.mentionsSomeone()) {
            channel.sendMessage("Your permission level is below the required level to use a command mention.").queue()
            return false
        }

        if (!manager.canPerformAction(user, config.permissionedActions.sendInvite) && message.containsInvite()) {
            channel.sendMessage("Ayyy lmao. Nice try, try that again. I dare you. :rllynow:").queue()
            return false
        }

        if (!manager.canPerformAction(user, config.permissionedActions.sendURL) && message.containsURL()) {
            channel.sendMessage("Your permission level is below the required level to use a URL in a command.").queue()
            return false
        }

        return true
    }

    private fun handleDelete(message: Message, prefix: String) =
            if (!isDoubleInvocation(message, prefix)) {
                message.deleteIfExists()
            } else Unit

    private fun isDoubleInvocation(message: Message, prefix: String) = message.contentRaw.startsWith(prefix + prefix)
}

