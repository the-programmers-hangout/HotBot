package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.jda.getMemberJoinString
import me.aberrantfox.hotbot.extensions.jda.sendPrivateMessage
import me.aberrantfox.hotbot.extensions.stdlib.formatJdaDate
import me.aberrantfox.hotbot.extensions.stdlib.idToName
import me.aberrantfox.hotbot.extensions.stdlib.idToUser
import me.aberrantfox.hotbot.extensions.stdlib.limit
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.InfractionAction
import me.aberrantfox.hotbot.services.UserID
import me.aberrantfox.hotbot.utility.muteMember
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color

data class StrikeRequest(val user: User, val reason: String, val amount: Int, val moderator: User)

object StrikeRequests {
    val map = HashMap<UserID, StrikeRequest>()
}

@CommandSet
fun strikeCommands() =
    commands {
        command("warn") {
            expect(ArgumentType.User, ArgumentType.Sentence)
            execute {
                val newArgs = listOf(it.args[0], 0, it.args[1])
                val e = it.copy(args=newArgs)

                infract(e)
            }
        }

        command("strike") {
            expect(arg(ArgumentType.User),
                   arg(ArgumentType.Integer, optional = true, default = 1),
                   arg(ArgumentType.Sentence))
            execute {
                infract(it)
            }
        }

        command("strikerequest") {
            expect(ArgumentType.User, ArgumentType.Integer, ArgumentType.Sentence)
            execute {
                val target = it.args.component1() as User
                val amount = it.args.component2() as Int
                val reason = it.args.component3() as String

                if(amount > it.config.security.strikeCeil) {
                    it.respond("Error, strike quantity above strike ceiling. ")
                    return@execute
                }

                val request = StrikeRequest(target, reason, amount, it.author)

                StrikeRequests.map.put(target.id, request)
                it.respond("This has been logged and will be accepted or declined, thank you.")
                info("${it.author.fullName()} has a new strike request. Use viewRequest ${target.asMention} to see it.")
            }
        }

        command("viewRequest") {
            expect(ArgumentType.User)
            execute {
                val user = it.args.component1() as User

                if( !(strikeAgainst(user, it)) ) return@execute

                val request = StrikeRequests.map[user.id]!!

                it.respond(embed {
                    title("${request.moderator.fullName()}'s request")

                    field {
                        name = "Target"
                        value = "${request.user.asMention}(${request.user.fullName()})"
                        inline = false
                    }

                    field {
                        name = "Reasoning"
                        value = request.reason
                        inline = false
                    }

                    field {
                        name =  "Amount"
                        value = "${request.amount}"
                        inline = false
                    }
                })
            }
        }

        command("acceptrequest") {
            expect(ArgumentType.User)
            execute {
                val user = it.args.component1() as User

                if( !(strikeAgainst(user, it)) ) return@execute

                val request = StrikeRequests.map[user.id]!!
                val newArgs = listOf(request.user, request.amount, request.reason)
                infract(it.copy(args=newArgs))

                StrikeRequests.map.remove(user.id)
                user.sendPrivateMessage("Strike request was accepted. Thanks a bunch!" )
            }
        }

        command("declinerequest") {
            expect(ArgumentType.User)
            execute {
                val user = it.args.component1() as User

                if( !(strikeAgainst(user, it)) ) return@execute

                StrikeRequests.map.remove(user.id)
                user.sendPrivateMessage("Strike request was declined, better lucky next time :)")
            }
        }

        command("deleteRequest") {
            expect(ArgumentType.User)
            execute {
                val user = it.args.component1() as User

                if( !(strikeAgainst(user, it)) ) return@execute

                val byInvoker = StrikeRequests.map[user.id]!!.moderator.id == it.author.id

                if(byInvoker) {
                    StrikeRequests.map.remove(user.id)
                    it.respond("Request removed.")
                } else {
                    it.respond("You did not make that request and as such cannot delete it.")
                }
            }
        }

        command("listrequests") {
            execute {
                if(StrikeRequests.map.isEmpty()) {
                    it.respond("No requests currently in place.")
                    return@execute
                }
                val response = StrikeRequests.map.values
                    .map { "${it.user.asMention }, requested by ${it.moderator.fullName()}" }
                    .reduce {a, b -> "$a \n$b" }

                it.respond(response)
            }
        }

        command("history") {
            expect(ArgumentType.User)
            execute {
                val target = it.args[0] as User
                it.respond(buildHistoryEmbed(target, true, getHistory(target.id),
                        getNotesByUser(target.id), it))
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

                target.sendPrivateMessage(buildHistoryEmbed(target, false, getHistory(target.id),
                        null, it))
            }
        }
    }

private fun strikeAgainst(user: User, event: CommandEvent) =
    if( !(StrikeRequests.map.containsKey(user.id)) ) {
        event.respond("That user does not currently have a strike request.")
        false
    } else {
        true
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

        val punishmentAction = config.security.infractionActionMap[totalStrikes]?.toString() ?: "None"
        val infractionEmbed = buildInfractionEmbed(user.asMention, reason, strikeQuantity,
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

private fun buildHistoryEmbed(target: User, includeModerator: Boolean, records: List<StrikeRecord>,
                              notes: List<NoteRecord>?, it: CommandEvent) =
        embed {
            title("${target.fullName()}'s Record")
            setColor(Color.MAGENTA)
            setThumbnail(target.effectiveAvatarUrl)

            field {
                name = ""
                value = "__**Summary**__"
                inline = false
            }

            field {
                name = "Information"
                value = "${target.fullName()} has **${records.size}** infractions(s).\nOf these infractions, " +
                        "**${records.filter { it.isExpired }.size}** are expired and **${records.filter { !it.isExpired }.size}** are still in effect." +
                        "\nCurrent strike value of **${getMaxStrikes(target.id)}/${it.config.security.strikeCeil}**" +
                        "\nJoin date: **${it.guild.getMemberJoinString(target)}**" +
                        "\nCreation date: **${target.creationTime.toString().formatJdaDate()}**"
                inline = false
            }

            field {
                name = ""
                value = "__**Infractions**__"
                inline = false
            }

            records.forEach { record ->
                field {
                    name = "ID :: ${record.id} :: Weight :: ${record.strikes}"
                    value = "This infraction is **${expired(record.isExpired)}**."
                    inline = false

                    if(includeModerator) {
                        value += "\nIssued by **${record.moderator.idToName(it.jda)}** on **${record.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
                    }
                }

                produceFields("Infraction Reasoning Given", record.reason).forEach { addField(it) }
            }

            if (records.isEmpty()) {
                field {
                    name = "No Infractions"
                    value = "Clean as a whistle, sir."
                    inline = false
                }
            }

            if (!includeModerator || notes == null) {
                return@embed
            }

            field {
                name = ""
                value = "__**Notes**__"
                inline = false
            }

            if(notes.isEmpty()) {
                field {
                    name = "No Notes"
                    value = "User has no notes written"
                    inline = false
                }
            }

            notes.forEach { note ->
                field {
                    name = "ID :: __${note.id}__ :: Staff :: __${note.moderator.idToName(it.jda)}__"
                    value = "Noted on **${note.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
                    inline = false
                }

                produceFields("NoteMessage", note.note).forEach { addField(it) }
            }

        }

fun produceFields(title: String, message: String) = message.chunked(1024).mapIndexed { index, chunk ->
    MessageEmbed.Field("$title -- $index", chunk, false)
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
                value = reason.limit(1024)
                inline = false
            }

            setColor(Color.RED)
        }


private fun expired(boolean: Boolean) = if (boolean) "expired" else "not expired"