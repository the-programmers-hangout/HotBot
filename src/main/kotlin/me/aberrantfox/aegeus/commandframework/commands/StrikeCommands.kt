package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.util.*
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.*
import me.aberrantfox.aegeus.services.database.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.joda.time.format.DateTimeFormat
import java.awt.Color


@Command(ArgumentType.UserID, ArgumentType.Joiner)
fun warn(event: CommandEvent) =
        strike(CommandEvent(event.guildEvent, listOf(event.args[0], 0, event.args[1]), event.config))

@Command(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Joiner)
fun strike(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val target = args[0] as String
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if(strikeQuantity < 0 || strikeQuantity > 3) {
        guildEvent.channel.sendMessage("Strike weight should be between 0 and 3").queue()
        return
    }

    if( !(guildEvent.guild.members.map { it.user.id }.contains(target)) ) {
        guildEvent.channel.sendMessage("Cannot find the member by the id: $target").queue()
        return
    }

    insertInfraction(target, guildEvent.author.id, strikeQuantity, reason)

    guildEvent.author.openPrivateChannel().queue {
        it.sendMessage("User ${target.idToUser(guildEvent.jda).asMention} has been infracted with weight: $strikeQuantity," +
                " with reason $reason.").queue()
    }

    var totalStrikes = getMaxStrikes(target)

    if(totalStrikes > config.strikeCeil) totalStrikes = config.strikeCeil

    administerPunishment(config, target.idToUser(guildEvent.jda), strikeQuantity, reason, guildEvent, guildEvent.author, totalStrikes)
    guildEvent.message.delete().queue()
}

@Command(ArgumentType.UserID)
fun history(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val target = args[0] as String
    val records = getHistory(target)
    val builder = EmbedBuilder()
            .setTitle("${target.idToName(guildEvent.jda)}'s Record")
            .setColor(Color.MAGENTA)
            .setThumbnail(target.idToUser(guildEvent.jda).avatarUrl)

    records.forEach {
        builder.addField("Strike ID: ${it.id}",
                "**Acting moderator**: ${it.moderator.idToName(guildEvent.jda)}" +
                        "\n**Reason**: ${it.reason}" +
                        "\n**Weight**: ${it.strikes}" +
                        "\n**Date**: ${it.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}," +
                        "\n**Expired:** ${it.isExpired}",
                false)
    }

    if(builder.fields.size == 0) {
        builder.addField("Strikes",
                "Clean as a whistle sir",
                false)
    }

    guildEvent.channel.sendMessage(builder.build()).queue()
    guildEvent.message.delete().queue()
}

@Command(ArgumentType.Integer)
fun removeStrike(event: CommandEvent) {
    val (guildEvent, args) = event
    val strikeID = args[0] as Int
    val amountRemoved = removeInfraction(strikeID)

    guildEvent.channel.sendMessage("Deleted $amountRemoved strike records.").queue()
}

@Command(ArgumentType.UserID)
fun cleanse(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val userId = args[0] as String
    val amount = removeAllInfractions(userId)

    guildEvent.channel.sendMessage("Infractions for ${userId.idToUser(guildEvent.jda).asMention} have been wiped. Total removed: $amount").queue()
}

private fun administerPunishment(config: Configuration, user: User, strikeQuantity: Int, reason: String,
                                 event: GuildMessageReceivedEvent, moderator: User, totalStrikes: Int) {
    user.openPrivateChannel().queue { chan ->
        val punishmentAction = config.infractionActionMap[totalStrikes]
        chan.sendMessage("${chan.user.asMention}, you have been infracted. Infractions are formal warnings from staff members" +
                " on TPH. The infraction you just received was a $strikeQuantity strike infraction," +
                " and you received it for reason: $reason\n" +
                " Your current strike count is $totalStrikes/${config.strikeCeil}.\n" +
                "The assigned punishment for this infraction is: $punishmentAction").queue {

            when (config.infractionActionMap[totalStrikes]) {
                InfractionAction.Warn -> {
                    chan.sendMessage("This is your warning - Do not break the rules again.").queue()
                }
                InfractionAction.Kick -> {
                    chan.sendMessage("You may return via this: https://discord.gg/BQN6BYE - please be mindful of the rules next time.")
                            .queue {
                                event.guild.controller.kick(user.id, reason).queue()
                            }
                }
                InfractionAction.Mute -> {
                    muteMember(event.guild, user, 1000 * 60 * 60 * 24, reason, config, moderator)
                }
                InfractionAction.Ban -> {
                    chan.sendMessage("Well... that happened. There may be an appeal system in the future. But for now, you're" +
                            " permanently banned. Sorry about that :) ").queue {
                        event.guild.controller.ban(user.id, 0, reason).queue()
                    }
                }
            }
        }
    }
}