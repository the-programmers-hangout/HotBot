package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.listeners.antispam.NewPlayers


enum class SecurityLevel(val matchCount: Int, val waitPeriod: Int, val maxAmount: Int) {
    Normal(6, 10, 5), Elevated(6, 5, 5), High(4, 5, 4), Max(3, 3, 3)
}

fun names() = SecurityLevel.values().map { it.name }

object SecurityLevelState {
    var alertLevel: SecurityLevel = SecurityLevel.Normal
}

@CommandSet
fun securityCommands() = commands {
    command("setSecuritylevel") {
        expect(ArgumentType.Word)
        execute {
            val targetLevel = (it.args[0] as String).capitalize()

            try {
                val parsed = SecurityLevel.valueOf(targetLevel)
                SecurityLevelState.alertLevel = parsed
                it.respond("Level set to ${parsed.name}")
            } catch (e: IllegalArgumentException) {
                it.respond("SecurityLevel: $targetLevel is unknown, known levels are: ${names()}")
            }
        }
    }

    command("securitylevel") {
        execute {
            it.respond("Current security level: ${SecurityLevelState.alertLevel}")
        }
    }

    command("viewnewplayers") {
        execute {
            it.respond("Current tracked new players: ${NewPlayers.names(it.jda)}")
        }
    }

    command("resetsecuritylevel") {
        execute {
            SecurityLevelState.alertLevel = SecurityLevel.Normal
            it.respond("Security level set to normal.")
        }
    }
}
