package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg


@CommandSet("MessageConfiguration")
fun messageConfiguration(mService: MService) = commands {
    command("setBotDescription") {
        description = "Set the description of the bot"
        expect(SentenceArg)
        execute {
            val description = it.args[0] as String
            mService.messages.botDescription = description
            mService.writeMessages()

            it.respond("Botinfo is now set to `$description`")
        }
    }

    command("setGagMessage") {
        description = "Set the message sent to someone when the `gag` command is used on them"
        expect(SentenceArg)
        execute {
            val gagResponse = it.args[0] as String
            mService.messages.gagResponse = gagResponse
            mService.writeMessages()

            it.respond("The message sent when someone is gagged is now `$gagResponse`")
        }
    }

    command("setPermanentInvite") {
        description = "Sets the invite link displayed when the `invite` command is run"
        expect(SentenceArg)
        execute {
            val invite = it.args[0] as String
            mService.messages.permanentInviteLink = invite
            mService.writeMessages()

            it.respond("The permanent invite link is now set to `$invite`")
        }
    }

    command("setServerDescription") {
        description = "Sets the description of the server displayed when the `serverinfo` command is run."
        expect(SentenceArg)
        execute {
            val description = it.args[0] as String
            mService.messages.serverDescription = description
            mService.writeMessages()

            it.respond("The server description is now set to `$description`")
        }
    }

    command("setWelcomeMessage") {
        description = "Set the message displayed at the bottom of every automatic greeting message"
        expect(SentenceArg)
        execute {
            val description  = it.args[0] as String
            mService.messages.welcomeDescription = description
            mService.writeMessages()

            it.respond("The server greeting is now set to `$description`")
        }
    }
}