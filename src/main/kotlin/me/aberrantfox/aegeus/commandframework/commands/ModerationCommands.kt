package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.commandframework.stringToPermission
import me.aberrantfox.aegeus.commandframework.extensions.idToUser
import me.aberrantfox.aegeus.commandframework.extensions.isUserID
import me.aberrantfox.aegeus.commandframework.extensions.isUserIDList
import me.aberrantfox.aegeus.commandframework.extensions.muteMember
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.BanQueue
import me.aberrantfox.aegeus.services.MessageService
import me.aberrantfox.aegeus.services.MessageType
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel

@RequiresGuild(false)
@Command(ArgumentType.Integer)
fun nuke(event: CommandEvent) {
    if(event.guild == null) return

    val amount = event.args[0] as Int

    if (amount <= 0) {
        event.channel.sendMessage("Yea, what exactly is the point in nuking nothing... ?").queue()
        return
    }

    event.channel.history.retrievePast(amount + 1).queue({
        it.forEach { it.delete().queue() }
        event.channel.sendMessage("Be nice. No spam.").queue()
    })
}

@RequiresGuild
@Command(ArgumentType.String)
fun ignore(event: CommandEvent) {
    if(event.guild == null) return

    val config = event.config
    val target = event.args[0] as String

    if (config.ignoredIDs.contains(target)) {
        config.ignoredIDs.remove(target)
        event.channel.sendMessage("Unignored $target").queue()
    } else {
        config.ignoredIDs.add(target)
        event.channel.sendMessage("$target? Who? What? Don't know what that is. ;)").queue()
    }
}

@RequiresGuild
@Command(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Joiner)
fun mute(event: CommandEvent) {
    if(event.guild == null) return

    val args = event.args

    val user = (args[0] as String).idToUser(event.jda)
    val time = (args[1] as Int).toLong() * 1000 * 60
    val reason = args[2] as String

    muteMember(event.guild, user, time, reason, event.config, event.author)
}

@Command
fun lockdown(event: CommandEvent) {
    val config = event.config
    config.lockDownMode = !config.lockDownMode
    event.channel.sendMessage("Lockdown mode is now set to: ${config.lockDownMode}.").queue()
}

@Command(ArgumentType.String)
fun prefix(event: CommandEvent) {
    val newPrefix = event.args[0] as String
    event.config.prefix = newPrefix
    event.channel.sendMessage("Prefix is now $newPrefix. Please invoke commands using that prefix in the future. " +
            "To save this configuration, use the saveconfigurations command.").queue()
    event.jda.presence.setPresence(OnlineStatus.ONLINE, Game.of("${event.config.prefix}help"))
}

@Command(ArgumentType.String)
fun setFilter(event: CommandEvent) {
    val desiredLevel = stringToPermission((event.args[0] as String).toUpperCase())

    if (desiredLevel == null) {
        event.channel.sendMessage("Don't know that permission level boss... ").queue()
        return
    }

    event.config.mentionFilterLevel = desiredLevel
    event.channel.sendMessage("Permission level now set to: ${desiredLevel.name} ; be sure to save configurations.").queue()
}

@RequiresGuild(false)
@Command(ArgumentType.String, ArgumentType.Integer, ArgumentType.String)
fun move(event: CommandEvent) {
    if(event.guild == null) return

    val args = event.args

    val targets = getTargets((args[0] as String))
    val searchSpace = args[1] as Int
    val chan = args[2] as String

    event.message.delete().queue()

    if (searchSpace < 0) {
        event.channel.sendMessage("... move what").queue()
        return
    }

    if(searchSpace > 99) {
        event.channel.sendMessage("Yea buddy, I'm not moving the entire channel into another, 99 messages or less").queue()
        return
    }

    val channel = event.guild.textChannels.filter { it.id == chan }.first()

    if (channel == null) {
        event.channel.sendMessage("... to where?").queue()
        return
    }

    event.channel.history.retrievePast(searchSpace + 1).queue {
        handleResponse(it, channel, targets, event.channel, event.author.asMention)
    }
}

@RequiresGuild
@Command(ArgumentType.UserID, ArgumentType.Joiner)
fun badname(event: CommandEvent) {
    if(event.guild == null) return

    val args = event.args
    val target = args[0] as String
    val reason = args[1] as String

    val targetMember = event.guild.getMemberById(target)

    event.guild.controller.setNickname(targetMember, MessageService.getMessage(MessageType.Name)).queue {
        targetMember.user.openPrivateChannel().queue {
            it.sendMessage("Your name has been changed forcefully by a member of staff for reason: $reason").queue()
        }
    }
}


@Command(ArgumentType.Joiner)
fun setQueueReason(event: CommandEvent) {
    val reason = event.args[0] as String
    BanQueue.setReason(event.author.id, reason)
    event.channel.sendMessage("Set Ban-Raid reason to: $reason").queue()
}

@Command(ArgumentType.Splitter)
fun addQueueTargets(event: CommandEvent) {
    val targets = event.args[0] as List<String>

    if( !(targets.isUserIDList(event.jda)) )  {
        event.channel.sendMessage("One or more UserIDS were invalid.").queue()
        return
    }

    targets.forEach { BanQueue.queueBan(event.author.id, it) }
    event.channel.sendMessage("Attempted to add ${targets.size} -- BanQueue now ${BanQueue.getBans(event.author.id)?.size}").queue()
}

@Command(ArgumentType.Splitter)
fun removeTarget(event: CommandEvent) {
    val targets = event.args[0] as List<String>

    if( !(targets.isUserIDList(event.jda)) ) {
        event.channel.sendMessage("One or more UserIDS were invalid.").queue()
        return
    }

    targets.forEach { BanQueue.removeBan(event.author.id, it) }
    event.channel.sendMessage("${targets.size} removed from the ban queue").queue()
}

@Command
fun clearQueue(event: CommandEvent) {
    BanQueue.clearBans(event.author.id)
    event.channel.sendMessage("Targets cleared.").queue()
}

@RequiresGuild
@Command
fun banRaid(event: CommandEvent) {
    if(event.guild == null) return

    val bans = BanQueue.getBans(event.author.id)
    val reason = BanQueue.getReason(event.author.id)

    if(bans == null || reason == null || bans.isEmpty()) {
        event.channel.sendMessage("You haven't added any bans... ").queue()
        return
    }

    bans.forEach { event.guild.controller.ban(it, 1, reason).queue() }
    BanQueue.clearBans(event.author.id)
    event.channel.sendMessage("Raid cleared.").queue()
}

@Command
fun showRaid(event: CommandEvent) {
    val targets = BanQueue.getBans(event.author.id)

    if(targets == null || targets.isEmpty()) {
        event.channel.sendMessage("You haven't added anyone to your ban queue").queue()
        return
    }

    event.channel.sendMessage("Current targets: ${targets.reduce{a, b -> "$a, $b"}}.").queue()
}

private fun handleResponse(past: List<Message>, channel: MessageChannel, targets: List<String>, error: MessageChannel,
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
