package me.aberrantfox.hotbot.commandframework.commands.utility

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands

@CommandSet
fun giveawayCommands() = commands {
    command("giveaway"){
        expect(ArgumentType.TimeString, ArgumentType.Sentence)
        execute {
        }
    }
    command("giveawayend"){
        expect(ArgumentType.Word)
        execute{
        }
    }
    command("giveawayreroll"){
        expect(ArgumentType.Word)
        execute {
        }
    }
}

