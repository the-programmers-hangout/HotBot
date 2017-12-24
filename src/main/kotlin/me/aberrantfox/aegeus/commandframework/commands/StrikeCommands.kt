package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.permissions.CommandEvent
import me.aberrantfox.aegeus.commandframework.commands.dsl.commands
import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.services.*
import me.aberrantfox.aegeus.services.database.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color

fun strikeCommands() =
    commands {
        command("warn") {
            expect(ArgumentType.UserID, ArgumentType.Joiner)
            execute {
                strike(CommandEvent(listOf(it.args[0], 0, it.args[1]),
                    it.config, it.jda, it.channel,
                    it.author, it.message, it.guild))
            }
        }

        command("strike") {
            expect(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Joiner)
            execute {
                if(it.guild == null) return@execute

                val args = it.args
                val target = args[0] as String
                val strikeQuantity = args[1] as Int
                val reason = args[2] as String

                if(strikeQuantity < 0 || strikeQuantity > 3) {
                    it.respond("Strike weight should be between 0 and 3")
                    return@execute
                }

                if( !(it.guild.members.map { it.user.id }.contains(target)) ) {
                    it.respond("Cannot find the member by the id: $target")
                    return@execute
                }

                insertInfraction(target, it.author.id, strikeQuantity, reason)

                it.author.openPrivateChannel().queue {
                    it.sendMessage("User ${target.idToUser(it.jda).asMention} has been infracted with weight: $strikeQuantity," +
                        " with reason $reason.").queue()
                }

                var totalStrikes = getMaxStrikes(target)

                if(totalStrikes > it.config.strikeCeil) totalStrikes = it.config.strikeCeil

                administerPunishment(it.config, target.idToUser(it.jda), strikeQuantity, reason, it.guild, it.author, totalStrikes)
            }
        }

        command("history") {
            expect(ArgumentType.UserID)
            execute {
                val target = it.args[0] as String
                val records = getHistory(target)
                val builder = EmbedBuilder()
                    .setTitle("${target.idToName(it.jda)}'s Record")
                    .setColor(Color.MAGENTA)
                    .setThumbnail(target.idToUser(it.jda).avatarUrl)

                records.forEach { record ->
                    builder.addField("Strike ID: ${record.id}",
                        "**Acting moderator**: ${record.moderator.idToName(it.jda)}" +
                            "\n**Reason**: ${record.reason}" +
                            "\n**Weight**: ${record.strikes}" +
                            "\n**Date**: ${record.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}," +
                            "\n**Expired:** ${record.isExpired}",
                        false)
                }

                if(builder.fields.size == 0) {
                    builder.addField("Strikes",
                        "Clean as a whistle sir",
                        false)
                }

                it.respond(builder.build())
            }
        }

        command("removestrike") {
            expect(ArgumentType.Integer)
            execute {
                val strikeID = it.args[0] as Int
                val amountRemoved = removeInfraction(strikeID)

                it.respond("Deleted $amountRemoved strike records.")
            }
        }

        command("cleanse") {
            execute {
                val userId = it.args[0] as String
                val amount = removeAllInfractions(userId)

                it.respond("Infractions for ${userId.idToUser(it.jda).asMention} have been wiped. Total removed: $amount")
            }
        }
    }


private fun handleInfraction(event: CommandEvent) {
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

private fun strike(event: CommandEvent) {
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