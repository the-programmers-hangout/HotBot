package me.aberrantfox.hotbot.commandframework.commands.permissions

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.listeners.antispam.RecentInvites

@CommandSet
fun inviteCommands() =
    commands {
        command("whitelistinvite") {
            expect(ArgumentType.Word)
            execute {
                val inv = it.args[0] as String
                RecentInvites.ignore.add(inv)
                it.respond("Added $inv to the invite whitelist, it can now be posted freely.")
                warning("$inv was added to the whitelist by ${it.author.fullName()} and it can now be posted freely")
            }
        }

        command("unwhitelistinvite") {
            expect(ArgumentType.Word)
            execute {
                val inv = it.args[0] as String
                RecentInvites.ignore.remove(inv)
                it.respond("$inv has been removed from the invite whitelist, posting it the configured amount of times will result in a ban.")
                warning("$inv was removed from the whitelist by ${it.author.fullName()}, the bot will now delete messages containing this invite.")
            }
        }
    }
