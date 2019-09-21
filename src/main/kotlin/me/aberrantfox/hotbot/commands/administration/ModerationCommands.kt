package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.arguments.*
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.extensions.safeDeleteMessages
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.randomListItem
import me.aberrantfox.kjdautils.internal.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.*
import java.text.SimpleDateFormat


@CommandSet("moderation")
fun moderationCommands(kConfig: KConfiguration,
                       config: Configuration,
                       messages: Messages,
                       manager: PermissionService,
                       logger: BotLogger) = commands {
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
                        if (users.isNotEmpty()) {
                            noSinglePrefixMsg.filter { it.author.id in users }
                        } else {
                            noSinglePrefixMsg
                        }

                channel.safeDeleteMessages(userFiltered) {
                    it.sendMessage("Be nice. No spam.").queue()
                }

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
        expect(WordArg("New Prefix"))
        execute {
            val newPrefix = it.args[0] as String
            config.serverInformation.prefix = newPrefix
            kConfig.prefix = newPrefix
            it.respond("Prefix is now $newPrefix. Please invoke commands using that prefix in the future." +
                "To save this configuration, use the saveconfigurations command.")
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

            it.guild!!.modifyNickname(targetMember, messages.names.randomListItem()).queue {
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

