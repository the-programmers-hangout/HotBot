package me.aberrantfox.hotbot.commands.utility

import com.github.ricksbrown.cowsay.Cowsay
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
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
            description = "Shows a cute animal. Animals implemented are dog, cat, fox, bird, snake, otter and rabbit"
            expect(arg(WordArg, true, "random"))
            execute {
                var animal = it.args[0] as String

                if(animal == "random"){ animal = randomAnimal() }

                when (animal) {
                    "dog" -> it.respond(buildAnimalEmbed(getDogImg()))
                    "cat" -> it.respond(buildAnimalEmbed(getCatImg()))
                    "fox" -> it.respond(buildAnimalEmbed(getFoxImg()))
                    "bird" -> it.respond(buildAnimalEmbed(getBirdImg()))
                    "snake" -> it.respond(buildAnimalEmbed(getSnakeImg()))
                    "otter" -> it.respond(buildAnimalEmbed(getOtterImg()))
                    "rabbit" -> it.respond(buildAnimalEmbed(getRabbitImg()))

                    else -> {it.respond(embed{
                        setTitle("$animal is not an animal")
                        setDescription("You can use one of the following dog, cat, fox, bird, snake, otter or rabbit")
                        setColor(Color.RED)
                    })}
                }
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
    val animals = listOf("dog", "cat", "fox", "bird", "snake", "otter", "rabbit")

    return animals.shuffled().first()
}

private fun buildAnimalEmbed(URL: String) = embed {
    setColor(Color.decode("#52be80"))
    setImage(URL)
}

private fun getDogImg(): String = kget("https://dog.ceo/api/breeds/image/random").jsonObject.getString("message")

private fun getCatImg(): String = kget("https://api.cheweybot.ga/cat").jsonObject.getString("data")

private fun getFoxImg(): String = kget("https://randomfox.ca/floof").jsonObject.getString("image")

private fun getBirdImg(): String = kget("https://api.cheweybot.ga/birb").jsonObject.getString("data")

private fun getSnakeImg(): String = kget("https://api.cheweybot.ga/snake").jsonObject.getString("data")

private fun getOtterImg(): String = kget("https://api.cheweybot.ga/otter").jsonObject.getString("data")

private fun getRabbitImg(): String = kget("https://api.cheweybot.ga/rabbit").jsonObject.getString("data")