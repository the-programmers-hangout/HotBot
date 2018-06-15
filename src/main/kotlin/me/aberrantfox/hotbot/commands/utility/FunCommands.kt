package me.aberrantfox.hotbot.commands.utility

import com.github.ricksbrown.cowsay.Cowsay
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import org.jsoup.Jsoup
import java.io.File
import java.net.URLEncoder
import java.util.*
import khttp.get as kget

@CommandSet("fun")
fun funCommands() =
    commands {
        command("cat") {
            description = "Displays a picture of a cat."
            execute {
                val json = kget("http://aws.random.cat/meow").jsonObject
                it.respond(json.getString("file"))
            }
        }

        command("bird") {
            description = "Display a picture of a bird"
            execute {
                val json = kget("https://birdsare.cool/bird.json?exclude=webm,mp4").jsonObject
                it.respond(json.getString("url"))
            }
        }

        command("flip") {
            description = "Flips a coin. Optionally, print one of the choices given."
            expect(arg(SplitterArg, true, listOf("Heads", "Tails")))
            execute {
                val options = it.args[0] as List<String>
                var choice = options[Random().nextInt(options.size)]
                if (options.size == 1)
                    choice += "\n... were you expecting something else ? :thinking: Did you forget the `|` separator ?"
                it.safeRespond(choice)
            }
        }

        command("dog") {
            description = "Display a picture of a dog"
            execute {
                val json = kget("https://dog.ceo/api/breeds/image/random").jsonObject
                it.respond(json.getString("message"))
            }
        }

        command("google") {
            description = "google a thing"
            expect(SentenceArg)
            execute {
                val google = "http://www.google.com/search?q="
                val search = it.args[0] as String
                val charset = "UTF-8"
                val userAgent = "Mozilla/5.0"

                val links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select(".g>.r>a")

                it.respond(links.first().absUrl("href"))
            }
        }

        command("cowsay") {
            description = "Displays a cowsay with a given message. Run with no arguments to get a list of valid cows."
            expect(arg(WordArg, true, {""}), arg(SentenceArg, true, {""}))
            execute {
                val arg0 = it.args[0] as String
                val arg1 = it.args[1] as String

                it.safeRespond(when {
                    arg0.isBlank() && arg1.isBlank() -> CowsayData.validCows.joinToString (", ")
                    arg0.isBlank() -> "```${Cowsay.say(arrayOf(arg0))}```"
                    arg1.isBlank() -> "Message argument required"
                    CowsayData.validCows.contains(arg0) -> "```${Cowsay.say(arrayOf("-f $arg0", arg1))}```"
                    else -> "```${Cowsay.say(arrayOf("$arg0 $arg1"))}```"
                })
            }
        }

    }


object CowsayData {
    val validCows = Cowsay.say(arrayOf("-l")).split("\n").filterNot { listOf("sodomized", "head-in", "telebears").contains(it) }
}

