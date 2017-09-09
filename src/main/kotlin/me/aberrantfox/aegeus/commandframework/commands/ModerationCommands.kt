package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.stringToPermission
import me.aberrantfox.aegeus.commandframework.util.idToUser
import me.aberrantfox.aegeus.commandframework.util.muteMember
import me.aberrantfox.aegeus.commandframework.util.unmute
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.MessageService
import me.aberrantfox.aegeus.services.MessageType
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent


@Command(ArgumentType.Integer)
fun nuke(event: CommandEvent) {
    val (guildEvent, args) = event
    val amount = args[0] as Int

    if (amount <= 0) {
        guildEvent.channel.sendMessage("Yea, what exactly is the point in nuking nothing... ?").queue()
        return
    }

    guildEvent.channel.history.retrievePast(amount + 1).queue({
        it.forEach { it.delete().queue() }
        guildEvent.channel.sendMessage("Be nice. No spam.").queue()
    })
}

@Command(ArgumentType.String)
fun ignore(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val target = args[0] as String

    if (config.ignoredIDs.contains(target)) {
        config.ignoredIDs.remove(target)
        guildEvent.channel.sendMessage("Unignored $target").queue()
    } else {
        config.ignoredIDs.add(target)
        guildEvent.channel.sendMessage("$target? Who? What? Don't know what that is. ;)").queue()
    }
}

@Command(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Joiner)
fun mute(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val user = (args[0] as String).idToUser(guildEvent.jda)
    val time = (args[1] as Int).toLong() * 1000 * 60
    val reason = args[2] as String
    val guild = guildEvent.guild

    muteMember(guild, user, time, reason, config, guildEvent.author)
}

@Command
fun lockdown(event: CommandEvent) {
    val (guildEvent, _, config) = event
    config.lockDownMode = !config.lockDownMode
    guildEvent.channel.sendMessage("Lockdown mode is now set to: ${config.lockDownMode}.").queue()
}

@Command(ArgumentType.String)
fun prefix(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val newPrefix = args[0] as String
    config.prefix = newPrefix
    guildEvent.channel.sendMessage("Prefix is now $newPrefix. Please invoke commands using that prefix in the future. " +
            "To save this configuration, use the saveconfigurations command.").queue()
    guildEvent.jda.presence.setPresence(OnlineStatus.ONLINE, Game.of("${config.prefix}help"))
}

@Command(ArgumentType.String)
fun setFilter(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val desiredLevel = stringToPermission((args[0] as String).toUpperCase())

    if (desiredLevel == null) {
        guildEvent.channel.sendMessage("Don't know that permission level boss... ").queue()
        return
    }

    config.mentionFilterLevel = desiredLevel
    guildEvent.channel.sendMessage("Permission level now set to: ${desiredLevel.name} ; be sure to save configurations.").queue()
}

@Command(ArgumentType.String, ArgumentType.Integer, ArgumentType.String)
fun move(event: CommandEvent) {
    val (guildEvent, args) = event
    val targets = getTargets((args[0] as String))
    val searchSpace = args[1] as Int
    val chan = args[2] as String

    guildEvent.message.delete().queue()

    if (searchSpace < 0) {
        guildEvent.channel.sendMessage("... move what").queue()
        return
    }

    if(searchSpace > 99) {
        guildEvent.channel.sendMessage("Yea buddy, I'm not moving the entire channel into another, 99 messages or less").queue()
        return
    }

    val channel = guildEvent.guild.textChannels.filter { it.id == chan }.first()

    if (channel == null) {
        guildEvent.channel.sendMessage("... to where?").queue()
        return
    }

    guildEvent.channel.history.retrievePast(searchSpace + 1).queue {
        handleResponse(it, channel, targets, guildEvent.channel, guildEvent.author.asMention)
    }
}

@Command(ArgumentType.UserID, ArgumentType.Joiner)
fun badname(event: CommandEvent) {
    val (guildEvent, args) = event
    val target = args[0] as String
    val reason = args[1] as String
    val targetMember = guildEvent.guild.getMemberById(target)

    guildEvent.guild.controller.setNickname(targetMember, MessageService.getMessage(MessageType.Name)).queue {
        targetMember.user.openPrivateChannel().queue {
            it.sendMessage("Your name has been changed forcefully by a member of staff for reason: $reason").queue()
        }
    }
}

private fun handleResponse(past: List<Message>, channel: TextChannel, targets: List<String>, error: TextChannel,
                           source: String) {
    val messages = past.subList(1, past.size).filter { targets.contains(it.author.id) }
    if (messages.isEmpty()) {
        error.sendMessage("No messages found").queue()
        return
    }

    val response = messages
            .map { "${it.author.asMention} said ${it.rawContent}" }
            .reduce { a, b -> "$a\n$b" }

    channel.sendMessage("==Messages moved from ${channel.name} to here by $source\n$response")
            .queue{ messages.forEach { it.delete().queue() }}
}

private fun getTargets(msg: String): List<String> =
        if (msg.contains(",")) {
            msg.split(",")
        } else {
            listOf(msg)
        }
