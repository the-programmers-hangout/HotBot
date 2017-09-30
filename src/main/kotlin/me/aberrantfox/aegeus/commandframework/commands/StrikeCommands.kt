package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.*
import me.aberrantfox.aegeus.services.database.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color


@Command(ArgumentType.UserID, ArgumentType.Joiner)
fun warn(event: CommandEvent) =
        strike(CommandEvent(listOf(event.args[0], 0, event.args[1]),
                event.config, event.jda, event.channel,
                event.author, event.message, event.guild))

@RequiresGuild
@Command(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Joiner)
fun strike(event: CommandEvent) {
    if(event.guild == null) return

    val args = event.args
    val target = args[0] as String
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if(strikeQuantity < 0 || strikeQuantity > 3) {
        event.respond("Strike weight should be between 0 and 3")
        return
    }

    if( !(event.guild.members.map { it.user.id }.contains(target)) ) {
        event.respond("Cannot find the member by the id: $target")
        return
    }

    insertInfraction(target, event.author.id, strikeQuantity, reason)

    event.author.openPrivateChannel().queue {
        it.sendMessage("User ${target.idToUser(event.jda).asMention} has been infracted with weight: $strikeQuantity," +
                " with reason $reason.").queue()
    }

    var totalStrikes = getMaxStrikes(target)

    if(totalStrikes > event.config.strikeCeil) totalStrikes = event.config.strikeCeil

    administerPunishment(event.config, target.idToUser(event.jda), strikeQuantity, reason, event.guild, event.author, totalStrikes)
}

@Command(ArgumentType.UserID)
fun history(event: CommandEvent) {
    val target = event.args[0] as String
    val records = getHistory(target)
    val builder = EmbedBuilder()
            .setTitle("${target.idToName(event.jda)}'s Record")
            .setColor(Color.MAGENTA)
            .setThumbnail(target.idToUser(event.jda).avatarUrl)

    records.forEach {
        builder.addField("Strike ID: ${it.id}",
                "**Acting moderator**: ${it.moderator.idToName(event.jda)}" +
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

    event.respond(builder.build())
}

@Command(ArgumentType.Integer)
fun removeStrike(event: CommandEvent) {
    val strikeID = event.args[0] as Int
    val amountRemoved = removeInfraction(strikeID)

    event.respond("Deleted $amountRemoved strike records.")
}

@Command(ArgumentType.UserID)
fun cleanse(event: CommandEvent) {
    val userId = event.args[0] as String
    val amount = removeAllInfractions(userId)

    event.respond("Infractions for ${userId.idToUser(event.jda).asMention} have been wiped. Total removed: $amount")
}

private fun administerPunishment(config: Configuration, user: User, strikeQuantity: Int, reason: String,
                                 guild: Guild, moderator: User, totalStrikes: Int) {
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
                                guild.controller.kick(user.id, reason).queue()
                            }
                }
                InfractionAction.Mute -> {
                    muteMember(guild, user, 1000 * 60 * 60 * 24, reason, config, moderator)
                }
                InfractionAction.Ban -> {
                    chan.sendMessage("Well... that happened. There may be an appeal system in the future. But for now, you're" +
                            " permanently banned. Sorry about that :) ").queue {
                        guild.controller.ban(user.id, 0, reason).queue()
                    }
                }
            }
        }
    }
}