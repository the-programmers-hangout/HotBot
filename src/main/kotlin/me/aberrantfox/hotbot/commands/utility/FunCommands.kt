package me.aberrantfox.hotbot.commands.utility


import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.internal.arguments.*
import java.util.Random

@CommandSet("fun")
fun funCommands() =
    commands {
        command("flip") {
            description = "Flips a coin. Optionally, print one of the choices given."
            expect(arg(SplitterArg("Choice 1 | Choice 2 | ..."), true, listOf("Heads", "Tails")))
            execute {
                val options = it.args[0] as List<String>
                val choice = options[Random().nextInt(options.size)]
                val response = if (options.size == 1) "Yeah, I don't know what you are expecting by only giving one choice."
                                      else "Flipping amongst (${options.joinToString(", ")}) got you...\n$choice!"

                it.respond(response)
            }
        }
    }
