package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.LowerMemberArg
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.removeMuteRole
import me.aberrantfox.hotbot.utility.timeToString
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.descriptor
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.jda.toMember
import me.aberrantfox.kjdautils.internal.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.arguments.TimeStringArg
import net.dv8tion.jda.api.entities.Member
import org.joda.time.DateTime
import java.awt.Color
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

@CommandSet("MuteCommands")
fun createMuteCommands(config: Configuration,
                       loggingService: LoggingService,
                       muteService: MuteService,
                       messages: Messages,
                       databaseService: DatabaseService) = commands {
    command("gag") {
        description = "Temporarily mute a user for 5 minutes so that you can deal with something."
        requiresGuild = true
        expect(LowerMemberArg)
        execute {
            val member = it.args.component1() as Member

            if(member.id == it.discord.jda.selfUser.id) {
                it.respond("Nice try but I'm not going to gag myself.")
                return@execute
            }

            muteService.muteMember(member, 5 * 1000 * 60, messages.gagResponse, it.author)
        }
    }

    command("mute") {
        description = "Mute a member for a specified amount of time with the given reason."
        requiresGuild = true
        expect(LowerMemberArg,
                TimeStringArg,
                SentenceArg("Mute Reason"))
        execute {
            val member = it.args.component1() as Member
            val time = (it.args.component2() as Double).roundToLong() * 1000
            val reason = it.args.component3() as String

            if (member.id == it.discord.jda.selfUser.id) {
                it.respond("Nice try but I'm not going to mute myself.")
                return@execute
            }

            if (time <= 0) {
                it.respond("Sorry, the laws of physics disallow muting for non-positive durations.")
                return@execute
            }

            val alreadyMuted = muteService.checkMuteState(member)

            muteService.muteMember(member, time, reason, it.author)

            it.respond(embed {
                color = Color.RED
                title = "${member.descriptor()} has been muted"
                if (alreadyMuted != MuteService.MuteState.None) {
                    description = "User was already muted, overriding previous mute."
                }
            })
        }
    }

    command("unmute") {
        description = "Unmute a previously muted member"
        requiresGuild = true
        expect(LowerMemberArg)
        execute {
            val member = it.args.component1() as Member

            when (muteService.checkMuteState(member)) {
                MuteService.MuteState.TrackedMute -> muteService.cancelMute(member)
                MuteService.MuteState.UntrackedMute -> removeMuteRole(member, config, loggingService.logInstance)
                MuteService.MuteState.None -> {
                    it.respond("${member.descriptor()} isn't muted")
                    return@execute
                }
            }

            it.respond("${member.descriptor()} has been unmuted")
        }
    }

    command("badpfp") {
        description = "Notifies the user that they should change their profile pic. If they don't do that within 30 minutes, they will be automatically banned."
        requiresGuild = true
        expect(LowerMemberArg)
        execute {
            val member = it.args.component1() as Member
            val avatar = member.user.effectiveAvatarUrl
            val minutesUntilBan = 30L
            val timeLimit = 1000 * 60 * minutesUntilBan

            val warningMessage = "We have flagged your profile picture as inappropriate. " +
                    "Please change it within the next $minutesUntilBan minutes or you will be banned."

            muteService.muteMember(member, timeLimit, warningMessage, it.author)
            it.respond("${member.asMention} has been flagged for having a bad pfp and muted for $minutesUntilBan minutes.")

            Timer().schedule(timeLimit) {
                if(avatar == it.discord.jda.retrieveUserById(member.id).complete().effectiveAvatarUrl) {
                    member.user.sendPrivateMessage("Hi, since you failed to change your profile picture, you are being banned.", loggingService.logInstance)
                    Timer().schedule(delay = 1000 * 10) {
                        it.guild!!.ban(member, 1, "Having a bad profile picture and refusing to change it.").queue()
                    }
                } else {
                    member.user.sendPrivateMessage("Thank you for changing your avatar. You will not be banned.", loggingService.logInstance)
                }
            }
        }
    }

    command("selfmute") {
        description = "Need to study and want no distractions? Mute yourself! (Length defaults to 1 hour)"
        requiresGuild = true
        expect(arg(TimeStringArg, true, 3600.0))
        execute {
            val time = (it.args.component1() as Double).roundToLong() * 1000
            val member = it.author.toMember(it.guild!!)!!

            if(muteService.checkMuteState(member) != MuteService.MuteState.None) {
                it.respond("Nice try but you're already muted")
                return@execute
            }

            if(time > 1000*60*config.serverInformation.maxSelfmuteMinutes) {
                it.respond("Sorry but you can't mute yourself for that long")
                return@execute
            } else if (time <= 0) {
                it.respond("Sorry, the laws of physics disallow muting for non-positive durations.")
                return@execute
            }

            muteService.muteMember(member, time, "No distractions for a while? Got it", it.author)
        }
    }

    command("remainingmute") {
        description="Return the remaining time of a mute"
        execute {
            try{
                val unmuteTime = databaseService.mutes.getUnmuteRecord(it.author.id, config.serverInformation.guildid) - DateTime().millis
                it.respond(timeToString(unmuteTime))
            }catch (e: NoSuchElementException){
                it.respond("You aren't currently muted...")
            }
        }
    }
}