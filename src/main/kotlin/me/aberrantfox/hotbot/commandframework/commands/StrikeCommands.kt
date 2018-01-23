package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.*
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.dsls.embed.embed
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color

@CommandSet
fun strikeCommands() =
    commands {
        command("warn") {
            expect(ArgumentType.UserID, ArgumentType.Sentence)
            execute {
                strike(CommandEvent(listOf(it.args[0], 0, it.args[1]),
                    it.config, it.jda, it.channel,
                    it.author, it.message, it.guild, it.manager, it.container))
            }
        }

        command("strike") {
            expect(ArgumentType.UserID, ArgumentType.Integer, ArgumentType.Sentence)
            execute {
                val args = it.args
                val target = args[0] as String
                val strikeQuantity = args[1] as Int
                val reason = args[2] as String

                if (strikeQuantity < 0 || strikeQuantity > 3) {
                    it.respond("Strike weight should be between 0 and 3")
                    return@execute
                }

                if (!(it.guild.members.map { it.user.id }.contains(target))) {
                    it.respond("Cannot find the member by the id: $target")
                    return@execute
                }

                insertInfraction(target, it.author.id, strikeQuantity, reason)

                it.author.openPrivateChannel().queue {
                    it.sendMessage("User ${target.idToUser(it.jda).asMention} has been infracted with weight: $strikeQuantity," +
                        " with reason:\n\n$reason.").queue()
                }

                var totalStrikes = getMaxStrikes(target)

                if (totalStrikes > it.config.security.strikeCeil) totalStrikes = it.config.security.strikeCeil

                administerPunishment(it.config, target.idToUser(it.jda), strikeQuantity, reason, it.guild, it.author, totalStrikes)
            }
        }

        command("history") {
            expect(ArgumentType.UserID)
            execute {
                val target = it.args[0] as String
                it.respond(buildHistoryEmbed(target, true, getHistory(target), it))
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
            expect(ArgumentType.UserID)
            execute {
                val userId = it.args[0] as String
                val amount = removeAllInfractions(userId)

                it.respond("Infractions for ${userId.idToUser(it.jda).asMention} have been wiped. Total removed: $amount")
            }
        }

        command("selfhistory") {
            execute {
                val target = it.author.id
                target.idToUser(it.jda).sendPrivateMessage(buildHistoryEmbed(target, false, getHistory(target), it))
            }
        }
    }


private fun buildHistoryEmbed(target: String, includeModerator: Boolean, records: List<StrikeRecord>, it: CommandEvent) =
    embed {
        val targetUser = target.idToUser(it.jda)

        title("${target.idToName(it.jda)}'s Record")
        description("${target.idToName(it.jda)} has **${records.size}** infractions(s). Of these infractions, " +
            "**${records.filter { it.isExpired }.size}** are expired and **${records.filter { !it.isExpired }.size}** are still in effect." +
            "\nCurrent strike value of **${getMaxStrikes(target)}/${it.config.security.strikeCeil}**" +
            "\nJoin date: **${targetUser.toMember(it.guild).joinDate.toString().formatJdaDate()}**" +
            "\nCreation date: **${targetUser.creationTime.toString().formatJdaDate()}**")
        setColor(Color.MAGENTA)
        setThumbnail(targetUser.effectiveAvatarUrl)

        records.forEach { record ->
            field {
                name = "ID :: __${record.id}__ :: Weight :: __${record.strikes}__"
                value = "Issued by **${record.moderator.idToName(it.jda)}** on **${record.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
                inline = false

                if(includeModerator) {
                    value += "\nThis infraction is **${expired(record.isExpired)}**."
                }
            }

            field {
                name = "Infraction Reasoning Given"
                value = record.reason
            }

            addBlankField(false)
        }

        if (this.fields.isEmpty()) {
            ifield {
                name = "Strikes"
                value = "Clean as a whistle, sir."
            }
        }
    }

private fun handleInfraction(event: CommandEvent) {
    if (event.guild == null) return

    val args = event.args
    val target = args[0] as String
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if (strikeQuantity < 0 || strikeQuantity > 3) {
        event.respond("Strike weight should be between 0 and 3")
        return
    }

    if (!(event.guild.members.map { it.user.id }.contains(target))) {
        event.respond("Cannot find the member by the id: $target")
        return
    }

    insertInfraction(target, event.author.id, strikeQuantity, reason)

    event.author.openPrivateChannel().queue {
        it.sendMessage("User ${target.idToUser(event.jda).asMention} has been infracted with weight: $strikeQuantity," +
            " with reason:\n\n$reason.").queue()
    }

    var totalStrikes = getMaxStrikes(target)

    if (totalStrikes > event.config.security.strikeCeil) totalStrikes = event.config.security.strikeCeil

    administerPunishment(event.config, target.idToUser(event.jda), strikeQuantity, reason, event.guild, event.author, totalStrikes)
}

private fun strike(event: CommandEvent) {
    if (event.guild == null) return

    val args = event.args
    val target = args[0] as String
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if (strikeQuantity < 0 || strikeQuantity > 3) {
        event.respond("Strike weight should be between 0 and 3")
        return
    }

    if (!(event.guild.members.map { it.user.id }.contains(target))) {
        event.respond("Cannot find the member by the id: $target")
        return
    }

    insertInfraction(target, event.author.id, strikeQuantity, reason)

    event.author.openPrivateChannel().queue {
        it.sendMessage("User ${target.idToUser(event.jda).asMention} has been infracted with weight: $strikeQuantity," +
            " with reason:\n\n$reason").queue()
    }

    var totalStrikes = getMaxStrikes(target)

    if (totalStrikes > event.config.security.strikeCeil) totalStrikes = event.config.security.strikeCeil

    administerPunishment(event.config, target.idToUser(event.jda), strikeQuantity, reason, event.guild, event.author, totalStrikes)
}

private fun expired(boolean: Boolean) = if (boolean) "expired" else "not expired"

private fun administerPunishment(config: Configuration, user: User, strikeQuantity: Int, reason: String,
                                 guild: Guild, moderator: User, totalStrikes: Int) {
    user.openPrivateChannel().queue { chan ->
        val punishmentAction: String = config.security.infractionActionMap[totalStrikes]?.toString() ?: "None"
        val infractionEmbed = buildInfractionEmbed(chan.user.asMention, reason, strikeQuantity,
                totalStrikes, config.security.strikeCeil, punishmentAction)

        chan.sendMessage(infractionEmbed).queue {
            when (config.security.infractionActionMap[totalStrikes]) {
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
                    muteMember(guild, user, 1000 * 60 * 60 * 24, "Infraction punishment.", config, moderator)
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

private fun buildInfractionEmbed(userMention: String, reason: String, strikeQuantity: Int, totalStrikes: Int,
                                 strikeCeil: Int, punishmentAction: String) =
        embed {
            title("Infraction")
            description("$userMention, you have been infracted.\nInfractions are formal warnings from staff members on TPH.\n" +
                        "If you think your infraction is undoubtedly unjustified, please **do not** post about it in a public channel but take it up with an administrator.")

            ifield {
                name = "Strike Quantity"
                value = "$strikeQuantity"
            }

            ifield {
                name = "Strike Count"
                value = "$totalStrikes / $strikeCeil"
            }

            ifield {
                name = "Punishment"
                value = punishmentAction
            }

            field {
                name = "__Reason__"
                value = reason
                inline = false
            }

            setColor(Color.RED)
        }