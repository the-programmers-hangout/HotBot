package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.*
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.dsls.embed.embed
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color

@CommandSet
fun strikeCommands() =
    commands {
        command("warn") {
            expect(ArgumentType.User, ArgumentType.Sentence)
            execute {
                val newArgs = listOf(it.args[0], 0, it.args[1])
                val e = CommandEvent(it.config, it.jda, it.channel, it.author, it.message, it.guild, it.manager, it.container, newArgs)

                infract(e)
            }
        }

        command("strike") {
            expect(ArgumentType.User, ArgumentType.Integer, ArgumentType.Sentence)
            execute {
                infract(it)
            }
        }

        command("history") {
            expect(ArgumentType.User)
            execute {
                val target = it.args[0] as User
                it.respond(buildHistoryEmbed(target, true, getHistory(target.id), it))
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
            expect(ArgumentType.User)
            execute {
                val user = it.args[0] as User
                val amount = removeAllInfractions(user.id)

                it.respond("Infractions for ${user.asMention} have been wiped. Total removed: $amount")
            }
        }

        command("selfhistory") {
            execute {
                val target = it.author.id.idToUser(it.jda)
                target.sendPrivateMessage(buildHistoryEmbed(target, false, getHistory(target.id), it))
            }
        }
    }


private fun buildHistoryEmbed(target: User, includeModerator: Boolean, records: List<StrikeRecord>, it: CommandEvent) =
    embed {
        title("${target.fullName()}'s Record")
        description("${target.fullName()} has **${records.size}** infractions(s). Of these infractions, " +
            "**${records.filter { it.isExpired }.size}** are expired and **${records.filter { !it.isExpired }.size}** are still in effect." +
            "\nCurrent strike value of **${getMaxStrikes(target.id)}/${it.config.security.strikeCeil}**" +
            "\nJoin date: **${it.guild.getMemberJoinString(target)}**" +
            "\nCreation date: **${target.creationTime.toString().formatJdaDate()}**")
        setColor(Color.MAGENTA)
        setThumbnail(target.effectiveAvatarUrl)

        records.forEach { record ->
            field {
                name = "ID :: __${record.id}__ :: Weight :: __${record.strikes}__"
                value = "This infraction is **${expired(record.isExpired)}**."
                inline = false

                if(includeModerator) {
                    value += "\nIssued by **${record.moderator.idToName(it.jda)}** on **${record.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
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



private fun infract(event: CommandEvent) {
    val args = event.args
    val target = args[0] as User
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if (strikeQuantity < 0 || strikeQuantity > 3) {
        event.respond("Strike weight should be between 0 and 3")
        return
    }

    if (!(event.guild.isMember(target))) {
        event.respond("Cannot find the member by the id: ${target.id}")
        return
    }

    insertInfraction(target.id, event.author.id, strikeQuantity, reason)

    event.author.sendPrivateMessage("User ${target.asMention} has been infracted with weight: $strikeQuantity, with reason:\n$reason")

    var totalStrikes = getMaxStrikes(target.id)

    if (totalStrikes > event.config.security.strikeCeil) totalStrikes = event.config.security.strikeCeil

    administerPunishment(event.config, target, strikeQuantity, reason, event.guild, event.author, totalStrikes)
}

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

private fun expired(boolean: Boolean) = if (boolean) "expired" else "not expired"