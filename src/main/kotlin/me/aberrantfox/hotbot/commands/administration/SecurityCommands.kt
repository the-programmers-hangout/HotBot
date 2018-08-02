package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.listeners.antispam.NewPlayers
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.ChoiceArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg

enum class SecurityLevel(val matchCount: Int, val waitPeriod: Int, val maxAmount: Int) {
    Normal(6, 10, 5), Elevated(6, 5, 5), High(4, 5, 4), Max(3, 3, 3)
}

fun names() = SecurityLevel.values().map { it.name }

object SecurityLevelState {
    var alertLevel: SecurityLevel = SecurityLevel.Normal
}

@CommandSet("security")
fun securityCommands() = commands {
    command("setSecuritylevel") {
        description = "Set the bot's security level to one of: ${names()}"
        expect(ChoiceArg(*SecurityLevel.values()))
        execute {
            SecurityLevelState.alertLevel = it.args[0] as SecurityLevel
            it.respond("Level set to ${SecurityLevelState.alertLevel}")
        }
    }

    command("securitylevel") {
        description = "See what the current server security level is."
        execute {
            it.respond("Current security level: ${SecurityLevelState.alertLevel}")
        }
    }

    command("viewnewplayers") {
        description = "View what the bot deems as new players"
        execute {
            it.respond("Current tracked new players: ${NewPlayers.names(it.jda)}")
        }
    }

    command("resetsecuritylevel") {
        description = "Set the security level back to normal."
        execute {
            SecurityLevelState.alertLevel = SecurityLevel.Normal
            it.respond("Security level set to normal.")
        }
    }
}
