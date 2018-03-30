package me.aberrantfox.hotbot.commandframework.commands.administration

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.jda.isCommandInvocation
import me.aberrantfox.hotbot.extensions.jda.performActionIfIsID
import me.aberrantfox.hotbot.extensions.jda.sendPrivateMessage
import me.aberrantfox.hotbot.extensions.stdlib.randomListItem
import me.aberrantfox.hotbot.extensions.stdlib.retrieveIdToUser
import me.aberrantfox.hotbot.extensions.stdlib.toRole
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.utility.muteMember
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class ModerationCommands

@CommandSet
fun moderationCommands() = commands {
    command("nuke") {
        expect(ArgumentType.Integer)
        execute {
            val amount = it.args[0] as Int
            if (amount <= 0) {
                it.respond("Yea, what exactly is the point in nuking nothing... ?")
            } else {
                it.channel.history.retrievePast(amount + 1).queue { past ->
                    past.forEach { msg -> msg.delete().queue() }
                    it.respond("Be nice. No spam.")
                }
            }
        }
    }

    command("ignore") {
        expect(ArgumentType.Word)
        execute {
            val config = it.config
            val target = it.args[0] as String

            if (config.security.ignoredIDs.contains(target)) {
                config.security.ignoredIDs.remove(target)
                it.respond("Unignored $target")
            } else {
                config.security.ignoredIDs.add(target)
                it.respond("$target? Who? What? Don't know what that is. ;)")
            }
        }
    }

    command("gag") {
        expect(ArgumentType.User)
        execute {
            val user = it.args.component1() as User
            muteMember(it.guild, user, 5 * 1000 * 60, it.mService.messages.gagResponse, it.config, it.author)
        }
    }

    command("mute") {
        expect(ArgumentType.User, ArgumentType.Integer, ArgumentType.Sentence)
        execute {
            val args = it.args

            val user = args[0] as User
            val time = (args[1] as Int).toLong() * 1000 * 60
            val reason = args[2] as String

            muteMember(it.guild, user, time, reason, it.config, it.author)
        }
    }

    command("lockdown") {
        execute {
            val config = it.config
            config.security.lockDownMode = !config.security.lockDownMode
            it.respond("Lockdown mode is now set to: ${config.security.lockDownMode}.")
        }
    }

    command("prefix") {
        expect(ArgumentType.Word)
        execute {
            val newPrefix = it.args[0] as String
            it.config.serverInformation.prefix = newPrefix
            it.respond("Prefix is now $newPrefix. Please invoke commands using that prefix in the future." +
                "To save this configuration, use the saveconfigurations command.")
        }
    }

    command("setfilter") {
        expect(ArgumentType.Word)
        execute {
            val desiredLevel = (it.args[0] as String).toRole(it.guild)

            if (desiredLevel == null) {
                it.respond("Don't know that permission level boss... ")
            } else {
                it.config.permissionedActions.commandMention = desiredLevel.id
                it.respond("Permission level now set to: ${desiredLevel.name} ; be sure to save configurations.")
            }
        }
    }

    command("move") {
        expect(ArgumentType.Word, ArgumentType.Integer, ArgumentType.Word)
        execute {
            val args = it.args

            val targets = getTargets((args[0] as String))
            val searchSpace = args[1] as Int
            val chan = args[2] as String

            if (searchSpace < 0) {
                it.respond("... move what")
                return@execute
            }

            if (searchSpace > 99) {
                it.respond("Yea buddy, I'm not moving the entire channel into another, 99 messages or less")
                return@execute
            }

            val channel = it.guild.textChannels.firstOrNull { it.id == chan }

            if (channel == null) {
                it.respond("... to where?")
                return@execute
            }

            it.message.delete().queue()

            it.channel.history.retrievePast(searchSpace + 1).queue { past ->
                handleResponse(past, channel, targets, it.channel, it.author.asMention, it.config)
            }
        }
    }

    command("badname") {
        expect(ArgumentType.User, ArgumentType.Sentence)
        execute {
            val target = it.args[0] as User
            val reason = it.args[1] as String

            val targetMember = it.guild.getMember(target)

            it.guild.controller.setNickname(targetMember, it.mService.messages.names.randomListItem()).queue {
                target.sendPrivateMessage("Your name has been changed forcefully by a member of staff for reason: $reason")
            }
        }
    }

    command("joindate") {
        expect(ArgumentType.User)
        execute {
            val target = it.args[0] as User
            val member = it.guild.getMember(target)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val joinDateParsed = dateFormat.parse(member.joinDate.toString())
            val joinDate = dateFormat.format(joinDateParsed)

            it.respond("${member.fullName()}'s join date: $joinDate")
        }
    }

    command("restart") {
        execute {
            val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            val currentJar = File(ModerationCommands::class.java.protectionDomain.codeSource.location.toURI())

            if (!currentJar.name.endsWith(".jar")) return@execute

            ProcessBuilder(arrayListOf(javaBin, "-jar", currentJar.path)).start()
            System.exit(0)
        }
    }

    command("setbanreason") {
        expect(ArgumentType.Word, ArgumentType.Sentence)
        execute {
            val target = (it.args[0] as String)
            val reason = it.args[1] as String

            try {
                it.jda.performActionIfIsID(target) { user ->
                    updateOrSetReason(target, reason, it.author.id)
                    it.respond("The ban reason for $target has been logged")
                }
            } catch (e: IllegalArgumentException) {
                it.respond("$target is not a valid ID")
            }
        }
    }

    command("getbanreason") {
        expect(ArgumentType.Word)
        execute {
            val target = it.args[0] as String

            try {
                it.jda.performActionIfIsID(target) { user ->
                    val record = getReason(target)

                    if (record != null) {
                        it.respond("$target was banned by ${record.mod.retrieveIdToUser(it.jda).fullName()} for reason ${record.reason}")
                    } else {
                        it.respond("That user does not have a record logged.")
                    }

                }
            } catch (e: IllegalArgumentException) {
                it.respond("$target is not a valid ID")
            }
        }
    }

    command("note") {
        expect(ArgumentType.User, ArgumentType.Sentence)
        execute {
            val target = it.args.component1() as User
            val note = it.args.component2() as String

            insertNote(target.id, it.author.id, note)

            it.respond("Note added.")
        }
    }

    command("removenote") {
        expect(ArgumentType.Integer)
        execute {
            val noteId = it.args.component1() as Int
            val notesRemoved = removeNoteById(noteId)

            it.respond("Number of notes removed: $notesRemoved")
        }
    }

    command("cleansenotes") {
        expect(ArgumentType.User)
        execute {
            val target = it.args.component1() as User
            val amount = removeAllNotesByUser(target.id)

            it.respond("Notes for ${target.asMention} have been wiped. Total removed: $amount")
        }
    }

    command("badpfp") {
        expect(ArgumentType.User)
        execute {
            val user = it.args.component1() as User
            val avatar = user.effectiveAvatarUrl

            user.sendPrivateMessage("We have flagged your profile picture as inappropriate. " +
                "Please change it within the next 30 minutes or you will be banned.")

            Timer().schedule(1000 * 60 * 30) {
                if(avatar == user.id.retrieveIdToUser(it.jda).effectiveAvatarUrl) {
                    user.sendPrivateMessage("Hi, since you failed to change your profile picture, you are being banned.")
                    Timer().schedule(1000 * 10) {
                        it.guild.controller.ban(user, 1, "Having a bad profile picture and refusing to change it.").queue()
                    }
                } else {
                    user.sendPrivateMessage("Thank you for changing your avatar. You will not be banned.")
                }
            }
        }
    }
}

private fun handleResponse(past: List<Message>, channel: MessageChannel, targets: List<String>, error: MessageChannel,
                           source: String, config: Configuration) {

    val messages = if (past.firstOrNull()?.isCommandInvocation(config) == true)
                       // Without ++move command invocation message
                       past.subList(1, past.size).filter { targets.contains(it.author.id)}
                   else
                       /*
                       Without extra message that could've been the ++move message but wasn't.
                       Side effect of having to search searchSpace+1 because of queue/API request timings
                       causing the possibility but not guarantee of the ++move command invocation being
                       included in the past List.
                       */
                       past.subList(0, past.size - 1).filter {targets.contains(it.author.id)}

    if (messages.isEmpty()) {
        error.sendMessage("No messages found").queue()
        return
    }

    val responseEmbed = buildResponseEmbed(error, source, messages)

    channel.sendMessage(responseEmbed).queue {
        messages.forEach {
            it.delete().queue()
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

private fun getTargets(msg: String): List<String> =
    if (msg.contains(",")) {
        msg.split(",")
    } else {
        listOf(msg)
    }
