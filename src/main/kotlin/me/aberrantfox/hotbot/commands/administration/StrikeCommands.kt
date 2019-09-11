package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.LowerMemberArg
import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.listeners.UserID
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.InfractionAction
import me.aberrantfox.hotbot.services.MuteService
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.formatJdaDate
import me.aberrantfox.kjdautils.extensions.stdlib.limit
import me.aberrantfox.kjdautils.internal.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import org.joda.time.format.DateTimeFormat
import java.awt.Color

data class StrikeRequest(val target: Member, val reason: String, val amount: Int, val moderator: User)

object StrikeRequests {
    val map = HashMap<UserID, StrikeRequest>()
}

@CommandSet("infractions")
fun strikeCommands(config: Configuration, log: BotLogger, muteService: MuteService) =
    commands {
        command("warn") {
            description = "Warn a member, giving them a 0 strike infraction with the given reason."
            requiresGuild = true
            expect(LowerMemberArg, SentenceArg("Warn Reason"))
            execute {
                val target = it.args.component1() as Member
                val reason = it.args.component2() as String

                infract(StrikeRequest(target, reason, 0, it.author), config, log, muteService)

                it.respond("User ${target.descriptor()} has been warned, with reason:\n$reason")
            }
        }

        command("strike") {
            description = "Give a member a weighted infraction for the given reason, defaulting to a weight of 1."
            requiresGuild = true
            expect(arg(LowerMemberArg),
                   arg(IntegerArg("Weight"), optional = true, default = 1),
                   arg(SentenceArg("Infraction Reason")))
            execute {
                val target = it.args.component1() as Member
                val weight = it.args.component2() as Int
                val reason = it.args.component3() as String

                // Not an IntegerRangeArg, because otherwise an invalid value is just absorbed by SentenceArg
                // and so "strike @person 4 blah blah" would default to a strike weight of 1 with message "4 blah blah"z
                val weightRange = 0..config.security.strikeCeil
                if (weight !in weightRange) return@execute it.respond("The weight must be in the range $weightRange")

                infract(StrikeRequest(target, reason, weight, it.author), config, log, muteService)

                it.respond("User ${target.descriptor()} has been infracted with weight: $weight, with reason:\n$reason")
            }
        }

        command("strikerequest") {
            description = "Like the strike command, except another moderator reviews it before it is accepted."
            requiresGuild = true
            expect(LowerMemberArg,
                   IntegerArg("Weight"),
                   SentenceArg("Infraction Reason"))
            execute {
                val target = it.args.component1() as Member
                val weight = it.args.component2() as Int
                val reason = it.args.component3() as String

                val weightRange = 0..config.security.strikeCeil
                if (weight !in weightRange) return@execute it.respond("The weight must be in the range $weightRange")

                val request = StrikeRequest(target, reason, weight, it.author)

                if (StrikeRequests.map.containsKey(target.id)) {
                    it.respond("There already exists a strike request for this user. Use viewrequest to see it.")
                    return@execute
                }

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

                val request = StrikeRequests.map[user.id]
                        ?: return@execute it.respond("That user does not currently have a strike request.")

                it.respond(embed {
                    title = "${request.moderator.fullName()}'s request"

                    field {
                        name = "Target"
                        value = "${request.target.asMention}(${request.target.fullName()})"
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
            requiresGuild = true
            expect(LowerUserArg("User Receiving Infraction"))
            execute {
                val user = it.args.component1() as User

                val request = StrikeRequests.map[user.id]
                        ?: return@execute it.respond("That member does not currently have a strike request.")

                if (user.toMember(it.guild!!) == null) {
                    return@execute it.respond("User is not a guild member. Use (delete/decline)request to remove the request if necessary.")
                }

                infract(request, config, log, muteService)

                StrikeRequests.map.remove(user.id)
                it.respond("Strike request on ${user.descriptor()} was accepted.")
            }
        }

        command("declinerequest") {
            description = "Reject a request for a strike on the given user."
            expect(LowerUserArg("User Receiving Infraction"))
            execute {
                val user = it.args.component1() as User

                StrikeRequests.map.remove(user.id) ?: return@execute it.respond("No request exists for that user.")

                it.respond("Strike request on ${user.descriptor()} was declined.")
            }
        }

        command("deleteRequest") {
            description = "Delete a strike request made by yourself on the given user."
            expect(LowerUserArg("User Receiving Infraction"))
            execute {
                val user = it.args.component1() as User

                val request = StrikeRequests.map[user.id]
                        ?: return@execute it.respond("That user does not currently have a strike request.")

                val byInvoker = request.moderator.id == it.author.id

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
                    .map { "${it.target.descriptor() }, requested by ${it.moderator.fullName()}" }
                    .reduce {a, b -> "$a \n$b" }

                it.respond(response)
            }
        }

        command("history") {
            description = "Display a user's infraction history."
            requiresGuild = true
            expect(UserArg)
            execute {
                val target = it.args[0] as User

                incrementOrSetHistoryCount(target.id)

                it.respond(buildHistoryEmbed(target, true, getHistory(target.id),
                        getHistoryCount(target.id), getNotesByUser(target.id), it, it.guild!!, config))

                val leaveHistory = getLeaveHistory(target.id, it.guild!!.id)
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
                it.respond("Infractions for ${user.descriptor()} have been wiped. Total removed: $amount")
            }
        }

        command("selfhistory") {
            description = "See your own infraction history."
            requiresGuild = true
            execute {
                val target = it.author

                target.sendPrivateMessage(buildHistoryEmbed(target, false, getHistory(target.id),
                        getHistoryCount(target.id), null, it, it.guild!!, config), log)
            }
        }
    }

private fun infract(strike: StrikeRequest, config: Configuration, log: BotLogger, muteService: MuteService) {
    insertInfraction(strike)

    val (target, reason, weight, moderator) = strike
    val totalStrikes = getMaxStrikes(target.id)

    val punishmentLevel =
            if (totalStrikes > config.security.strikeCeil) {
                config.security.strikeCeil
            } else {
                totalStrikes
            }

    val infractionEmbed = buildInfractionEmbed(target, reason, weight, punishmentLevel, config)
    target.user.sendPrivateMessage(infractionEmbed, log)

    val action = config.security.infractionActionMap[punishmentLevel] ?: return

    administerPunishment(action, reason, target, moderator, log, muteService)
}

private fun administerPunishment(action: InfractionAction, reason: String, target: Member, moderator: User, log: BotLogger, muteService: MuteService) =
        when (action) {
            is InfractionAction.Warn -> {
                target.user.sendPrivateMessage("This is your warning - Do not break the rules again.", log)
            }

            is InfractionAction.Kick -> {
                target.user.sendPrivateMessage("You may return via this: https://discord.gg/BQN6BYE - please be mindful of the rules next time.", log)
                target.guild.kick(target.id, reason).queue()
            }

            is InfractionAction.Mute -> {
                muteService.muteMember(target, action.duration * 60L * 1000L, "Infraction punishment.", moderator)
            }

            is InfractionAction.Ban -> {
                target.user.sendPrivateMessage("Well... that happened. There may be an appeal system in the future. But for now, you're permanently banned. Sorry about that :) ", log)
                target.guild.ban(target.id, 0, reason).queue()
            }
        }

private fun buildleaveHistoryEmbed(target: User, leaveHistory: List<LeaveHistoryRecord>) = embed {
    setTitle("${target.fullName()}'s Guild Leave History")
    setColor(Color.MAGENTA)

    leaveHistory.forEachIndexed { num, record ->
        field {
            name = "Record"
            inline = true
            value = "#${num + 1}"
        }
        field {
            name = "Joined"
            inline = true
            value = record.joinDate.toString("yyyy-MM-dd")
        }

        field {
            name = if (record.ban) "Banned" else "Left"
            inline = true
            value = record.leaveDate.toString("yyyy-MM-dd")
        }
    }
}

private fun buildHistoryEmbed(target: User, includeModerator: Boolean, records: List<StrikeRecord>,
                              historyCount: Int, notes: List<NoteRecord>?, it: CommandEvent, guild: Guild,
                              config: Configuration) =
        embed {
            title = "${target.fullName()}'s Record"
            color = Color.MAGENTA
            thumbnail = target.effectiveAvatarUrl

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
                        "\nCreation date: **${target.timeCreated.toString().formatJdaDate()}**"
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
                        value += "\nIssued by **${it.discord.jda.retrieveUserById(record.moderator).complete().name}** on **${record.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
                    }
                }

                produceFields("Infraction Reasoning Given", record.reason).forEach { addField(it.name, it.value, it.isInline) }
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
                val moderator = it.discord.jda.retrieveUserById(note.moderator).complete().name

                field {
                    name = "ID :: __${note.id}__ :: Staff :: __${moderator}__"
                    value = "Noted by **$moderator** on **${note.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy"))}**"
                    inline = false
                }

                produceFields("NoteMessage", note.note).forEach { addField(it.name, it.value, it.isInline) }
            }

        }

fun produceFields(title: String, message: String) = message.chunked(1024).mapIndexed { index, chunk ->
    MessageEmbed.Field("$title -- $index", chunk, false)
}

private fun buildInfractionEmbed(member: Member, reason: String, strikeQuantity: Int, punishmentLevel: Int, config: Configuration) =
        embed {
            title = "Infraction"
            description = "${member.user.name}, you have been infracted.\nInfractions are formal warnings from staff members on TPH.\n" +
                          "If you think your infraction is undoubtedly unjustified, please **do not** post about it in a public channel but take it up with an administrator."

            field {
                name = "Strike Quantity"
                inline = true
                value = "$strikeQuantity"
            }

            field {
                name = "Strike Count"
                inline = true
                value = "${getMaxStrikes(member.id)} / ${config.security.strikeCeil}"
            }

            field {
                name = "Punishment"
                inline = true
                value = config.security.infractionActionMap[punishmentLevel]?.toString() ?: "None"
            }

            field {
                name = "__Reason__"
                value = reason.limit(1024)
                inline = false
            }

            setColor(Color.RED)
        }


private fun expired(boolean: Boolean) = if (boolean) "expired" else "not expired"
