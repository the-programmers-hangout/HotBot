package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.services.AliasService
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg


@CommandSet("utility")
fun aliasCommands(aliasService: AliasService) = commands {
    command("alias") {
        description = "Create an alias called the second argument to the first"
        expect(WordArg, WordArg)
        execute {
            val original = it.args.component1() as String
            val alias = it.args.component2() as String

            it.respond(when (aliasService.add(alias to original)) {
                AliasService.CreationResult.Success -> "Added alias $alias -> $original"
                AliasService.CreationResult.InvalidCommand -> "Does $original sound like a command to you ? :thinking:"
                AliasService.CreationResult.UnavailableName -> "You dummy, $alias is already a command :weary:"
            })
        }
    }

    command("removeAlias") {
        description = "Remove an alias"
        expect(WordArg)
        execute {
            val alias = it.args.component1() as String

            it.respond(if (aliasService.remove(alias)) "Alias $alias removed."
                       else "No alias named $alias exists.")
        }
    }
}

