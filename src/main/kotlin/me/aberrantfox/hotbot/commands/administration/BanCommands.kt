package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.database.getReason
import me.aberrantfox.hotbot.database.updateOrSetReason
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.arguments.UserArg
import net.dv8tion.jda.api.entities.User

private val DeletionDaysArg = arg(IntegerArg("Message Deletion Days [Default: 1]"), true, 1)
private val BanReasonArg = arg(SentenceArg("Ban Reason"))

@CommandSet("BanCommands")
fun createBanCommands() = commands {
    command("Ban") {
        description = "Bans a member for the passed reason, deleting a given number of days messages."
        requiresGuild = true
        expect(arg(LowerUserArg), DeletionDaysArg, BanReasonArg)
        execute {
            val target = it.args.component1() as User
            val deleteMessageDays = it.args.component2() as Int
            val reason = it.args.component3() as String

            updateOrSetReason(target.id, reason, it.author.id)

            it.guild!!.ban(target, deleteMessageDays, reason).queue { _ ->
                it.respond("${target.fullName()} was banned.")
            }
        }
    }

    command("SetBanReason") {
        description = "Set the ban reason of someone logged who does not have a ban reason in the audit log."
        expect(UserArg, SentenceArg("Ban Reason"))
        execute {
            val target = it.args.component1() as User
            val reason = it.args.component2() as String

            updateOrSetReason(target.id, reason, it.author.id)
            it.respond("The ban reason for ${target.fullName()} has been logged")
        }
    }

    command("GetBanReason") {
        description = "Get the ban reason of someone logged who does not have a ban reason in the audit log."
        expect(UserArg)
        execute {
            val target = it.args.component1() as User
            val record = getReason(target.id)

            if (record != null) {
                it.respond("${target.fullName()} was banned by ${it.discord.jda.retrieveUserById(record.mod).complete().fullName()} for reason ${record.reason}")
            } else {
                it.respond("That user does not have a record logged.")
            }
        }
    }
}