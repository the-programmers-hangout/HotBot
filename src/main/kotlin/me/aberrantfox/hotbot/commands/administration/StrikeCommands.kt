package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.extensions.createContinuableField
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.*
import me.aberrantfox.kjdautils.internal.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.*
import org.joda.time.format.DateTimeFormat
import java.awt.Color

data class StrikeRequest(val target: Member, val reason: String, val amount: Int, val moderator: User)

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
        when (action.punishment.toInfractionAction()) {
            InfractionActionType.Warn -> {
                target.user.sendPrivateMessage("This is your warning - Do not break the rules again.", log)
            }

            InfractionActionType.Kick -> {
                target.user.sendPrivateMessage("You may return via this: https://discord.gg/BQN6BYE - please be mindful of the rules next time.", log)
                target.guild.kick(target.id, reason).queue()
            }

            InfractionActionType.Mute -> {
                muteService.muteMember(target, action.time!! * 60L * 1000L, "Infraction punishment.", moderator)
            }

            InfractionActionType.Ban -> {
                target.user.sendPrivateMessage("Well... that happened. There may be an appeal system in the future. But for now, you're permanently banned. Sorry about that :) ", log)
                target.guild.ban(target.id, 0, reason).queue()
            }

            InfractionActionType.Error -> {
                println("Error, invalid infraction action detected: ${action.punishment}, muting member instead.")
                muteService.muteMember(target, action.time!! * 60L * 1000L, "Infraction punishment.", moderator)
            }
        }

private fun buildleaveHistoryEmbed(target: User, leaveHistory: List<LeaveHistoryRecord>) = embed {
    title = "${target.fullName()}'s Guild Leave History"
    color = Color.MAGENTA

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

                createContinuableField("Infraction Reasoning Given", record.reason)
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

                createContinuableField("NoteMessage", note.note)
            }
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

            color = Color.RED
        }


private fun expired(boolean: Boolean) = if (boolean) "expired" else "not expired"

enum class InfractionActionType {
    Warn, Mute, Kick, Ban, Error
}

fun String.toInfractionAction() =
    when(this.toLowerCase()) {
    "warn" -> InfractionActionType.Warn
    "mute" -> InfractionActionType.Mute
    "kick" -> InfractionActionType.Kick
    "ban" -> InfractionActionType.Ban
    else -> InfractionActionType.Error
}