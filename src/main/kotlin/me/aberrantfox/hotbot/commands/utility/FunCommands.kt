package me.aberrantfox.hotbot.commands.utility

import com.github.ricksbrown.cowsay.Cowsay
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.internal.command.arguments.ChoiceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import org.jsoup.Jsoup
import java.awt.Color
import java.net.URLEncoder
import java.util.*
import khttp.get as kget

@CommandSet("fun")
fun funCommands() =
    commands {
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

        command("animal") {
            description = "Shows a cute animal. Animals implemented are ${animalMap.keys.joinToString(", ")}"
            expect(arg(ChoiceArg("Animal", *animalMap.keys.toTypedArray()), true, "random"))
            execute {
                var animal = it.args[0] as String

                if(animal == "random"){ animal = randomAnimal() }
                it.respond(buildAnimalEmbed(animalMap[animal]!!.invoke()))
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
                    arg1.isBlank() -> "```${Cowsay.say(arrayOf(arg0))}```"
                    CowsayData.validCows.contains(arg0) -> "```${Cowsay.say(arrayOf("-f $arg0", arg1))}```"
                    else -> "```${Cowsay.say(arrayOf("$arg0 $arg1"))}```"
                })
            }
        }

    }

object CowsayData {
    val validCows = Cowsay.say(arrayOf("-l")).split("\n").filterNot { listOf("sodomized", "head-in", "telebears").contains(it) }
}

private fun randomAnimal(): String{
    return animalMap.keys.shuffled().first()
}

private fun buildAnimalEmbed(URL: String) = embed {
    setColor(Color.decode("#52be80"))
    setImage(URL)
}

private val animalMap = mapOf(
        "dog" to { kget("https://dog.ceo/api/breeds/image/random").jsonObject.getString("message") },
        "cat" to { kget("https://api.chewey-bot.ga/cat").jsonObject.getString("data") },
        "fox" to { kget("https://randomfox.ca/floof").jsonObject.getString("image") },
        "bird" to { kget("https://api.chewey-bot.ga/birb").jsonObject.getString("data") },
        "snake" to { kget("https://api.chewey-bot.ga/snake").jsonObject.getString("data") },
        "otter" to { kget("https://api.chewey-bot.ga/otter").jsonObject.getString("data") },
        "rabbit" to { kget("https://api.chewey-bot.ga/rabbit").jsonObject.getString("data") }
)

