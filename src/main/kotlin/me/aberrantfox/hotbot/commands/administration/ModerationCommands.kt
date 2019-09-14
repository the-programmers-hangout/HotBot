package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.randomListItem
import me.aberrantfox.kjdautils.internal.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

class ModerationCommands

@CommandSet("moderation")
fun moderationCommands(kConfig: KConfiguration,
                       config: Configuration,
                       messageService: MessageService,
                       manager: PermissionService,
                       logger: BotLogger,
                       muteService: MuteService) = commands {
    command("ban") {
        description = "Bans a member for the passed reason, deleting a given number of days messages."
        requiresGuild = true
        expect(arg(LowerUserArg),
                arg(IntegerArg("Message Deletion Days [Default: 1]"), true, 1),
                arg(SentenceArg("Ban Reason")))
        execute {
            val target = it.args.component1() as User
            val deleteMessageDays = it.args.component2() as Int
            val reason = it.args.component3() as String

            it.guild!!.ban(target, deleteMessageDays, reason).queue { _ ->
                it.respond("${target.fullName()} was banned.")
            }
        }
    }

    command("nuke") {
        description = "Delete 2 - 99 past messages in the given channel (default is the invoked channel). If users are given, only messages from those will be deleted."
        expect(arg(TextChannelArg, optional = true, default = { it.channel }),
                arg(MultipleArg(UserArg), optional = true),
                arg(IntegerArg))
        execute {
            val channel = it.args.component1() as TextChannel
            val users = (it.args.component2() as List<User>).map { it.id }
            val amount = it.args.component3() as Int

            if (amount !in 2..99) {
                it.respond("You can only nuke between 2 and 99 messages")
                return@execute
            }

            val sameChannel = it.channel.id == channel.id
            val singlePrefixInvocationDeleted = it.stealthInvocation

            channel.history.retrievePast(amount + if (sameChannel) 1 else 0).queue { past ->

                val noSinglePrefixMsg = past.drop(if (sameChannel && singlePrefixInvocationDeleted) 1 else 0)

                val userFiltered =
                        if (!users.isEmpty()) {
                            noSinglePrefixMsg.filter { it.author.id in users }
                        } else {
                            noSinglePrefixMsg
                        }

                safeDeleteMessages(channel, userFiltered)

                channel.sendMessage("Be nice. No spam.").queue()

                if (!sameChannel) it.respond("$amount messages deleted.")
            }
        }
    }

    command("ignore") {
        description = "Drop and don't respond to anything from the given id (user or channel)"
        expect(UserArg or TextChannelArg)
        execute {
            val targetArg = it.args.component1() as Either<User, TextChannel>
            val (target, name) = when (targetArg) {
                is Either.Left -> targetArg.left.id to targetArg.left.name
                is Either.Right -> targetArg.right.id to targetArg.right.name
            }

            val removed = config.security.ignoredIDs.remove(target)
            if (removed) {
                deleteIgnoredID(target)
                it.respond("Unignored $name")
            } else {
                config.security.ignoredIDs.add(target)
                insertIgnoredID(target)
                it.respond("$name? Who? What? Don't know what that is. ;)")
            }
        }
    }

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

            muteService.muteMember(member, 5 * 1000 * 60, messageService.messages.gagResponse, it.author)
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
                MuteService.MuteState.UntrackedMute -> removeMuteRole(member, config, logger)
                MuteService.MuteState.None -> {
                    it.respond("${member.descriptor()} isn't muted")
                    return@execute
                }
            }

            it.respond("${member.descriptor()} has been unmuted")
        }
    }

    command("lockdown") {
        description = "Force the bot to ignore anyone who isn't the owner."
        category = "management"
        execute {
            config.security.lockDownMode = !config.security.lockDownMode
            it.respond("Lockdown mode is now set to: ${config.security.lockDownMode}.")
        }
    }

    command("verboselogging") {
        description = "Begins logging more detailed information about the actions of users."
        execute {
            config.security.verboseLogging = !config.security.verboseLogging
            it.respond("Verbose logging mode is now set to: ${config.security.verboseLogging}.")
        }
    }

    command("prefix") {
        description = "Set the bot prefix to the specified string (Cannot be a space)"
        category = "management"
        expect(WordArg("New Prefix"))
        execute {
            val newPrefix = it.args[0] as String
            config.serverInformation.prefix = newPrefix
            kConfig.prefix = newPrefix
            it.respond("Prefix is now $newPrefix. Please invoke commands using that prefix in the future." +
                "To save this configuration, use the saveconfigurations command.")
        }
    }

    command("setfilter") {
        description = "Set the permission level required to run a command which contains a mention (used for mention sanitation)"
        category = "management"
        expect(PermissionLevelArg)
        execute {
            val level = it.args.component1() as PermissionLevel
            config.permissionedActions.commandMention = level
            it.respond("Permission level now set to: ${level.name} ; be sure to save configurations.")
        }
    }

    command("move") {
        description = "Move messages sent by the users passed found within the last given number of messages to the specified channel."
        expect(MultipleArg(UserArg),
                IntegerArg("Number of Messages to Search Back"),
                TextChannelArg)
        execute {
            val targets = it.args.component1() as List<User>
            val searchSpace = it.args.component2() as Int
            val channel = it.args.component3() as TextChannel

            if (searchSpace < 0) {
                it.respond("... move what")
                return@execute
            }

            if (searchSpace > 99) {
                it.respond("Yea buddy, I'm not moving the entire channel into another, 99 messages or less")
                return@execute
            }

            if (it.commandStruct.doubleInvocation)
                it.message.delete().queue()

            it.channel.history.retrievePast(searchSpace + 1).queue { past ->
                handleResponse(past, channel, targets, it.channel as TextChannel, it.author.asMention, kConfig, logger)
            }
        }
    }

    command("badname") {
        description = "Auto-nick a user with a bad name, for the given reason."
        requiresGuild = true
        expect(LowerMemberArg,
                SentenceArg("Rename Reason"))
        execute {
            val targetMember = it.args[0] as Member
            val reason = it.args[1] as String

            it.guild!!.modifyNickname(targetMember, messageService.messages.names.randomListItem()).queue {
                targetMember.user.sendPrivateMessage("Your name has been changed forcefully by a member of staff for reason: $reason", logger)
            }
        }
    }

    command("joindate") {
        description = "See when a member joined the guild."
        requiresGuild = true
        expect(MemberArg)
        execute {
            val member = it.args[0] as Member

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val joinDateParsed = dateFormat.parse(member.timeJoined.toString())
            val joinDate = dateFormat.format(joinDateParsed)

            it.respond("${member.fullName()}'s join date: $joinDate")
        }
    }

    command("restart") {
        description = "Restart the bot."
        execute {
            val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            val currentJar = File(ModerationCommands::class.java.protectionDomain.codeSource.location.toURI())



            if (!currentJar.name.endsWith(".jar")) return@execute

            ProcessBuilder(arrayListOf(javaBin, "-jar", currentJar.path)).start()
            System.exit(0)
        }
    }

    command("setbanreason") {
        description = "Set the ban reason of someone logged who does not have a ban reason in the audit log."
        expect(UserArg, SentenceArg("Ban Reason"))
        execute {
            val target = it.args.component1() as User
            val reason = it.args.component2() as String

            updateOrSetReason(target.id, reason, it.author.id)
            it.respond("The ban reason for ${target.fullName()} has been logged")
        }
    }

    command("getbanreason") {
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

    command("note") {
        description = "Add a note to a user's history"
        expect(LowerUserArg, SentenceArg("Note Content"))
        execute {
            val target = it.args.component1() as User
            val note = it.args.component2() as String
            val noteId = insertNote(target.id, it.author.id, note)

            it.respond("Note added. ID: $noteId")
        }
    }

    command("removenote") {
        description = "Removes a note by ID (listed in history)"
        expect(IntegerArg("Note ID"))
        execute {
            val noteId = it.args.component1() as Int
            val notesRemoved = removeNoteById(noteId)

            it.respond("Number of notes removed: $notesRemoved")
        }
    }

    command("cleansenotes") {
        description = "Wipes all notes from a user (permanently deleted)."
        expect(LowerUserArg)
        execute {
            val target = it.args.component1() as User
            val amount = removeAllNotesByUser(target.id)

            it.respond("Notes for ${target.descriptor()} have been wiped. Total removed: $amount")
        }
    }

    command("editnote") {
        description = "Edits a note by ID (listed in history), replacing the content with the given text"
        expect(IntegerArg("Note ID"), SentenceArg("New Note Content"))
        execute {
            //get user id that note is placed on, use that in insertNote part. If possible, try to replace note at ID with a different note, rather than a different ID.
            val noteId = it.args.component1() as Int
            val note = it.args.component2() as String
            replaceNote(noteId, note, it.author.id)
            it.respond("Note $noteId has been updated.")
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
                    member.user.sendPrivateMessage("Hi, since you failed to change your profile picture, you are being banned.", logger)
                    Timer().schedule(delay = 1000 * 10) {
                        it.guild!!.ban(member, 1, "Having a bad profile picture and refusing to change it.").queue()
                    }
                } else {
                    member.user.sendPrivateMessage("Thank you for changing your avatar. You will not be banned.", logger)
                }
            }
        }
    }

    command("mutevoicechannel") {
        description = "Mute all non-moderators in a voice channel."
        requiresGuild = true
        expect(VoiceChannelArg)
        execute {
            val voiceChannel = it.args.component1() as VoiceChannel
            muteVoiceChannel(it.guild!!, voiceChannel, config, manager)
        }
    }

    command("unmutevoicechannel") {
        description = "Unmute all users in a voice channel."
        requiresGuild = true
        expect(VoiceChannelArg)
        execute {
            val voiceChannel = it.args.component1() as VoiceChannel
            unmuteVoiceChannel(it.guild!!, voiceChannel, config)
        }
    }

    command("nick"){
        description = "Nickname a user. If no name is specified, reset the nickname."
        requiresGuild = true
        expect(arg(LowerMemberArg),arg(SentenceArg("New Nickname"),true,""))
        execute{
            val member = it.args.component1() as Member
            var nickname = it.args.component2() as String

            if (nickname.length>32){
                it.respond("Please enter a nickname no more than 32 characters.")
                return@execute
            }

            if (nickname.isEmpty()) nickname = member.user.name

            it.guild!!.modifyNickname(member, nickname).queue()
        }
    }

}

private fun handleResponse(past: List<Message>, channel: TextChannel, targets: List<User>, sourceChannel: TextChannel,
                           source: String, kConfig: KConfiguration, logger: BotLogger) {


    val targetIDs = targets.map { it.id }
    val messages = if (past.firstOrNull()?.isCommandInvocation(kConfig) == true)
                       // Without ++move command invocation message
                       past.subList(1, past.size).filter { it.author.id in targetIDs }
                   else
                       /*
                       Without extra message that could've been the ++move message but wasn't.
                       Side effect of having to search searchSpace+1 because of queue/API request timings
                       causing the possibility but not guarantee of the ++move command invocation being
                       included in the past List.
                       */
                       past.subList(0, past.size - 1).filter { it.author.id in targetIDs }

    if (messages.isEmpty()) {
        sourceChannel.sendMessage("No messages found").queue()
        return
    }

    val responseEmbed = buildResponseEmbed(sourceChannel, source, messages)

    channel.sendMessage(responseEmbed).queue {
        if (messages.size == 1) {
            messages.first().delete().queue()
        } else {
            safeDeleteMessages(sourceChannel, messages)
        }

        targets.forEach {
            it.sendPrivateMessage("Your messages have been moved to ${channel.asMention}", logger)
        }
    }
}

private fun buildResponseEmbed(orig: MessageChannel, sourceMod: String, messages: List<Message>) =
        embed {
            title = "__Moved Messages__"

            field {
                name = "Source Channel"
                inline = true
                value = "<#${orig.id}>"
            }

            field {
                name = "By Staff"
                inline = true
                value = sourceMod
            }

            messages.reversed().forEach {
                val content = "${it.contentRaw}\n${it.attachments.joinToString("\n") { it.url }}"
                field {
                    name = "Message"
                    value = "${it.author.asMention}: $content" // Can't mention in 'name'
                    inline = false
                }
            }

            color = Color.CYAN
        }

fun safeDeleteMessages(channel: TextChannel,
                       messages: List<Message>) =
        try {
            channel.deleteMessages(messages).queue()
        } catch (e: IllegalArgumentException) { // some messages older than 2 weeks => can't mass delete
            messages.forEach { it.delete().queue() }
        }

fun safeDeleteMessagesByIds(channel: TextChannel,
                            messageIDs: List<String>,
                            messages: List<Message>) =
        try {
            channel.deleteMessagesByIds(messageIDs).queue()
        } catch (e: IllegalArgumentException) { // some messages older than 2 weeks => can't mass delete
            messages.forEach { it.delete().queue() }
        }
