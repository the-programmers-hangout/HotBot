package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.listeners.UserID
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.InfractionAction
import me.aberrantfox.hotbot.services.MuteService
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.getMemberJoinString
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.jda.toMember
import me.aberrantfox.kjdautils.extensions.stdlib.formatJdaDate
import me.aberrantfox.kjdautils.extensions.stdlib.limit
import me.aberrantfox.kjdautils.internal.command.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.UserArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color

data class StrikeRequest(val user: User, val reason: String, val amount: Int, val moderator: User)

object StrikeRequests {
    val map = HashMap<UserID, StrikeRequest>()
}

@CommandSet("infractions")
fun strikeCommands(config: Configuration, log: BotLogger, muteService: MuteService) =
    commands {
        command("warn") {
            description = "Warn a member, giving them a 0 strike infraction with the given reason."
            expect(LowerUserArg, SentenceArg("Warn Reason"))
            execute {
                val newArgs = listOf(it.args[0], 0, it.args[1])
                val e = it.copy(args=newArgs)
                val guild = it.jda.getGuildById(config.serverInformation.guildid)

                infract(e, guild, config, log, muteService)
            }
        }

        command("strike") {
            description = "Give a member a weighted infraction for the given reason, defaulting to a weight of 1."
            expect(arg(LowerUserArg),
                   arg(IntegerArg("Weight"), optional = true, default = 1),
                   arg(SentenceArg("Infraction Reason")))
            execute {
                val guild = it.jda.getGuildById(config.serverInformation.guildid)
                infract(it, guild, config, log, muteService)
            }
        }

        command("strikerequest") {
            description = "Like the strike command, except another moderator reviews it before it is accepted."
            expect(LowerUserArg, IntegerArg("Weight"), SentenceArg("Infraction Reason"))
            execute {
                val target = it.args.component1() as User
                val amount = it.args.component2() as Int
                val reason = it.args.component3() as String

                if(amount > config.security.strikeCeil) {
                    it.respond("Error, strike quantity above strike ceiling. ")
                    return@execute
                }

                val request = StrikeRequest(target, reason, amount, it.author)

                StrikeRequests.map[target.id] = request
                it.respond("This has been logged and will be accepted or declined, thank you.")
                log.info("${it.author.fullName()} has a new strike request. Use viewRequest ${target.asMention} to see it.")
            }
        }

        command("viewRequest") {
            description = "View the current strike request, if any, on the given user."
            expect(LowerUserArg("User Receiving Infraction"))
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
            description = "Accept a request for striking the given user by another moderator."
            expect(LowerUserArg("User Receiving Infraction"))
            execute {
                val user = it.args.component1() as User

                if( !(strikeAgainst(user, it)) ) return@execute

                val request = StrikeRequests.map[user.id]!!
                val newArgs = listOf(request.user, request.amount, request.reason)
                val guild = it.jda.getGuildById(config.serverInformation.guildid)
                infract(it.copy(args = newArgs), guild, config, log, muteService)

                StrikeRequests.map.remove(user.id)
                it.respond("Strike request on ${user.asMention} was accepted.")
            }
        }

        command("declinerequest") {
            description = "Reject a request for a strike on the given user."
            expect(LowerUserArg("User Receiving Infraction"))
            execute {
                val user = it.args.component1() as User

                if( !(strikeAgainst(user, it)) ) return@execute

                StrikeRequests.map.remove(user.id)
                it.respond("Strike request on ${user.asMention} was declined.")
            }
        }

        command("deleteRequest") {
            description = "Delete a strike request made by yourself on the given user."
            expect(LowerUserArg("User Receiving Infraction"))
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
            description = "List all current strike requests"
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
            description = "Display a user's infraction history."
            expect(UserArg)
            execute {
                val target = it.args[0] as User
                val guild = it.jda.getGuildById(config.serverInformation.guildid)

                incrementOrSetHistoryCount(target.id)

                it.respond(buildHistoryEmbed(target, true, getHistory(target.id),
                        getHistoryCount(target.id), getNotesByUser(target.id), it, guild, config))

                val leaveHistory = getLeaveHistory(target.id, guild.id)
                if (leaveHistory.isNotEmpty())
                    it.respond(buildleaveHistoryEmbed(target, leaveHistory))
            }
        }

        command("removestrike") {
            description = "Delete a particular strike by ID (listed in the history)."
            expect(IntegerArg("Infraction ID"))
            execute {
                val strikeID = it.args[0] as Int
                val amountRemoved = removeInfraction(strikeID)

                it.respond("Deleted $amountRemoved strike records.")
            }
        }

        command("cleanse") {
            description = "Completely wipe a user of all infractions. (Permanently deletes them)"
            expect(LowerUserArg)
            execute {
                val user = it.args[0] as User
                val amount = removeAllInfractions(user.id)

                resetHistoryCount(user.id)
                it.respond("Infractions for ${user.asMention} have been wiped. Total removed: $amount")
            }
        }

        command("selfhistory") {
            description = "See your own infraction history."
            execute {
                val target = it.author
                val guild = it.jda.getGuildById(config.serverInformation.guildid)

                target.sendPrivateMessage(buildHistoryEmbed(target, false, getHistory(target.id),
                        getHistoryCount(target.id), null, it, guild, config), log)
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

private fun infract(event: CommandEvent, guild: Guild, config: Configuration, log: BotLogger, muteService: MuteService) {
    val args = event.args
    val target = args[0] as User
    val strikeQuantity = args[1] as Int
    val reason = args[2] as String

    if (strikeQuantity < 0 || strikeQuantity > 3) {
        event.respond("Strike weight should be between 0 and 3")
        return
    }

    if (!(guild.isMember(target))) {
        event.respond("Cannot find the member by the id: ${target.id}")
        return
    }

    insertInfraction(target.id, event.author.id, strikeQuantity, reason)

    event.respond("User ${target.asMention} has been infracted with weight: $strikeQuantity, with reason:\n$reason")

    var totalStrikes = getMaxStrikes(target.id)

    if (totalStrikes > config.security.strikeCeil) totalStrikes = config.security.strikeCeil

    administerPunishment(config, log, target, strikeQuantity, reason, guild, event.author, totalStrikes, muteService)
}

private fun administerPunishment(config: Configuration, log: BotLogger, user: User, strikeQuantity: Int,
                                 reason: String, guild: Guild, moderator: User, totalStrikes: Int, muteService: MuteService) {

    val punishmentAction = config.security.infractionActionMap[totalStrikes]?.toString() ?: "None"
    val infractionEmbed = buildInfractionEmbed(user.asMention, reason, strikeQuantity,
            totalStrikes, config.security.strikeCeil, punishmentAction)
    val action = config.security.infractionActionMap[totalStrikes]

    user.sendPrivateMessage(infractionEmbed, log)
    when (action) {
        is InfractionAction.Warn -> {
            user.sendPrivateMessage("This is your warning - Do not break the rules again.", log)
        }

        is InfractionAction.Kick -> {
            user.sendPrivateMessage("You may return via this: https://discord.gg/BQN6BYE - please be mindful of the rules next time.", log)
            guild.controller.kick(user.id, reason).queue()
        }

        is InfractionAction.Mute -> {
            muteService.muteMember(user.toMember(guild), action.duration * 60L * 1000L, "Infraction punishment.", moderator)
        }

        is InfractionAction.Ban -> {
            user.sendPrivateMessage("Well... that happened. There may be an appeal system in the future. But for now, you're" +
                    " permanently banned. Sorry about that :) ", log)
            guild.controller.ban(user.id, 0, reason).queue()
        }
    }
}

private fun buildleaveHistoryEmbed(target: User, leaveHistory: List<LeaveHistoryRecord>) = embed {
    setTitle("${target.fullName()}'s Guild Leave History")
    setColor(Color.MAGENTA)

    leaveHistory.forEachIndexed { num, record ->
        ifield {
            name = "Record"
            value = "#${num + 1}"
        }
        ifield {
            name = "Joined"
            value = record.joinDate.toString("yyyy-MM-dd")
        }

        ifield {
            name = if (record.ban) "Banned" else "Left"
            value = record.leaveDate.toString("yyyy-MM-dd")
        }
    }
}

private fun buildHistoryEmbed(target: User, includeModerator: Boolean, records: List<StrikeRecord>,
                              historyCount: Int, notes: List<NoteRecord>?, it: CommandEvent, guild: Guild,
                              config: Configuration) =
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
                        "\nCurrent strike value of **${getMaxStrikes(target.id)}/${config.security.strikeCeil}**" +
                        "\nJoin date: **${guild.getMemberJoinString(target)}**" +
                        "\nCreation date: **${target.creationTime.toString().formatJdaDate()}**"
                inline = false
                if(includeModerator){
                    value +="\nHistory has been invoked **$historyCount** times."
                }
            }


            field {
                name = ""
                value = "__**Infractions**__"
                inline = false
            }

            records.forEach { record ->
                field {
                    name = "ID :: __${record.id}__ :: Weight :: __${record.strikes}__"
                    value = "This infraction is **${expired(record.isExpired)}**."
                    inline = false

                    if(includeModerator) {
                        value += "\nIssued by **${it.jda.retrieveUserById(record.moderator).complete().name}** on **${record.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
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
                val moderator = it.jda.retrieveUserById(note.moderator).complete().name

                field {
                    name = "ID :: __${note.id}__ :: Staff :: __${moderator}__"
                    value = "Noted by **$moderator** on **${note.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
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
