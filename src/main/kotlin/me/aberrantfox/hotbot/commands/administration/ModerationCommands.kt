package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.hotbot.utility.muteMember
import me.aberrantfox.hotbot.utility.muteVoiceChannel
import me.aberrantfox.hotbot.utility.unmuteVoiceChannel
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.isCommandInvocation
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.stdlib.randomListItem
import me.aberrantfox.kjdautils.internal.command.arguments.*
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

class ModerationCommands

@CommandSet("moderation")
fun moderationCommands(kConfig: KJDAConfiguration, config: Configuration, mService: MService, manager: PermissionManager) = commands {
    command("ban") {
        description = "Bans a member for the passed reason, deleting a given number of days messages."
        expect(arg(LowerUserArg), arg(IntegerArg, true, 1), arg(SentenceArg))
        execute {
            val target = it.args.component1() as User
            val deleteMessageDays = it.args.component2() as Int
            val reason = it.args.component3() as String

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            guild.controller.ban(target, deleteMessageDays, reason).queue { _ ->
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
            val singlePrefixInvocationDeleted = !it.commandStruct.doubleInvocation && kConfig.deleteOnInvocation

            channel.history.retrievePast(amount + if (sameChannel) 1 else 0).queue { past ->

                val noSinglePrefixMsg = past.drop(if (sameChannel && singlePrefixInvocationDeleted) 1 else 0)

                val userFiltered =
                        if (!users.isEmpty()) {
                            noSinglePrefixMsg.filter { it.author.id in users }
                        } else {
                            noSinglePrefixMsg
                        }

                val messageIDs = userFiltered.map { it.id }

                try {
                    channel.deleteMessagesByIds(messageIDs).queue()
                } catch (e: IllegalArgumentException) { // some messages older than 2 weeks
                    userFiltered.forEach { it.delete().queue() }
                }

                channel.sendMessage("Be nice. No spam.").queue()

                if (!sameChannel) it.respond("$amount messages deleted.")
            }
        }
    }

    command("ignore") {
        description = "Drop and don't respond to anything from the given id (user or channel)"
        expect(WordArg)
        execute {
            val target = it.args.component1() as String

            val removed = config.security.ignoredIDs.remove(target)

            if (removed) {
                deleteIgnoredID(target)
                it.respond("Unignored $target")
            } else {
                config.security.ignoredIDs.add(target)
                insertIgnoredID(target)
                it.respond("$target? Who? What? Don't know what that is. ;)")
            }
        }
    }

    command("gag") {
        description = "Temporarily mute a user for 5 minutes so that you can deal with something."
        expect(LowerUserArg)
        execute {
            val user = it.args.component1() as User
            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            if(user.id == it.jda.selfUser.id) {
                it.respond("Nice try but I'm not going to gag myself.")
                return@execute
            }

            muteMember(guild, user, 5 * 1000 * 60, mService.messages.gagResponse, config, it.author)
        }
    }

    command("mute") {
        description = "Mute a member for a specified amount of time with the given reason."
        expect(LowerUserArg, TimeStringArg, SentenceArg)
        execute {
            val user = it.args.component1() as User
            val time = (it.args.component2() as Double).roundToLong() * 1000
            val reason = it.args.component3() as String

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            if (user.id == it.jda.selfUser.id) {
                it.respond("Nice try but I'm not going to mute myself.")
                return@execute
            }

            muteMember(guild, user, time, reason, config, it.author)
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

    command("prefix") {
        description = "Set the bot prefix to the specified string (Cannot be a space)"
        category = "management"
        expect(WordArg)
        execute {
            val newPrefix = it.args[0] as String
            config.serverInformation.prefix = newPrefix
            it.respond("Prefix is now $newPrefix. Please invoke commands using that prefix in the future." +
                "To save this configuration, use the saveconfigurations command.")
        }
    }

    command("setfilter") {
        description = "Set the permission level required to run a command which contains a mention (used for mention sanitation)"
        category = "management"
        expect(PermissionLevelArg)
        execute {
            val level = it.args.component1() as me.aberrantfox.hotbot.permissions.PermissionLevel
            config.permissionedActions.commandMention = level
            it.respond("Permission level now set to: ${level.name} ; be sure to save configurations.")
        }
    }

    command("move") {
        description = "Move messages sent by the users passed found within the last given number of messages to the specified channel."
        expect(MultipleArg(UserArg), IntegerArg, TextChannelArg)
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
                handleResponse(past, channel, targets, it.channel as TextChannel, it.author.asMention, config)
            }
        }
    }

    command("badname") {
        description = "Auto-nick a user with a bad name."
        expect(LowerUserArg, SentenceArg)
        execute {
            val target = it.args[0] as User
            val reason = it.args[1] as String

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            val targetMember = guild.getMember(target)

            guild.controller.setNickname(targetMember, mService.messages.names.randomListItem()).queue {
                target.sendPrivateMessage("Your name has been changed forcefully by a member of staff for reason: $reason")
            }
        }
    }

    command("joindate") {
        description = "See when a member joined the guild."
        expect(UserArg)
        execute {
            val target = it.args[0] as User
            val guild = it.jda.getGuildById(config.serverInformation.guildid)
            val member = guild.getMember(target)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val joinDateParsed = dateFormat.parse(member.joinDate.toString())
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
        expect(UserArg, SentenceArg)
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
                it.respond("${target.fullName()} was banned by ${it.jda.retrieveUserById(record.mod).complete().fullName()} for reason ${record.reason}")
            } else {
                it.respond("That user does not have a record logged.")
            }
        }
    }

    command("note") {
        description = "Add a note to a user's history"
        expect(LowerUserArg, SentenceArg)
        execute {
            val target = it.args.component1() as User
            val note = it.args.component2() as String

            insertNote(target.id, it.author.id, note)

            it.respond("Note added.")
        }
    }

    command("removenote") {
        description = "Removes a note by ID (listed in history)"
        expect(IntegerArg)
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

            it.respond("Notes for ${target.asMention} have been wiped. Total removed: $amount")
        }
    }

    command("editnote") {
        description = "Edits a note by ID (listed in history)"
        expect(IntegerArg, SentenceArg)
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
        expect(LowerUserArg)
        execute {
            val user = it.args.component1() as User
            val avatar = user.effectiveAvatarUrl

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            user.sendPrivateMessage("We have flagged your profile picture as inappropriate. " +
                "Please change it within the next 30 minutes or you will be banned.")

            Timer().schedule(1000 * 60 * 30) {
                if(avatar == it.jda.retrieveUserById(user.id).complete().effectiveAvatarUrl) {
                    user.sendPrivateMessage("Hi, since you failed to change your profile picture, you are being banned.")
                    Timer().schedule(1000 * 10) {
                        guild.controller.ban(user, 1, "Having a bad profile picture and refusing to change it.").queue()
                    }
                } else {
                    user.sendPrivateMessage("Thank you for changing your avatar. You will not be banned.")
                }
            }
        }
    }

    command("mutevoicechannel") {
        description = "Mute all non-moderators in a voice channel."
        expect(VoiceChannelArg)
        execute {
            val voiceChannel = it.args.component1() as VoiceChannel
            val guild = it.jda.getGuildById(config.serverInformation.guildid)
            muteVoiceChannel(guild, voiceChannel, it.author, config, manager)
        }
    }

    command("unmutevoicechannel") {
        description = "Unmute all users in a voice channel."
        expect(VoiceChannelArg)
        execute {
            val voiceChannel = it.args.component1() as VoiceChannel
            val guild = it.jda.getGuildById(config.serverInformation.guildid)
            unmuteVoiceChannel(guild, voiceChannel, it.author)
        }
    }
}

private fun handleResponse(past: List<Message>, channel: TextChannel, targets: List<User>, sourceChannel: TextChannel,
                           source: String, config: Configuration) {


    val targetIDs = targets.map { it.id }
    val messages = if (past.firstOrNull()?.isCommandInvocation(KJDAConfiguration(prefix=config.serverInformation.prefix)) == true)
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
        if (messages.size == 1)
            messages.first().delete().queue()
        else
            sourceChannel.deleteMessages(messages).queue()

        targets.forEach {
            it.sendPrivateMessage("Your messages have been moved to ${channel.asMention}")
        }
    }
}

private fun buildResponseEmbed(orig: MessageChannel, sourceMod: String, messages: List<Message>) =
        embed {
            title("__Moved Messages__")

            ifield {
                name = "Source Channel"
                value = "<#${orig.id}>"
            }

            ifield {
                name = "By Staff"
                value = sourceMod
            }

            messages.reversed().forEach {
                val attachments = it.attachments
                val content = if (attachments.size > 0 && attachments[0].isImage())
                                  attachments[0].proxyUrl
                              else
                                  it.contentRaw

                field {
                    name = "Message"
                    value = "${it.author.asMention}: $content" // Can't mention in 'name'
                    inline = false
                }
            }

            setColor(Color.CYAN)
        }

