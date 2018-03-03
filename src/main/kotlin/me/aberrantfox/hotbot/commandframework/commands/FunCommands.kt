package me.aberrantfox.hotbot.commandframework.commands

import com.github.ricksbrown.cowsay.Cowsay
import khttp.get as kget
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import org.jsoup.Jsoup
import java.io.File
import java.net.URLEncoder

import java.util.*

@CommandSet
fun funCommands() =
    commands {
        command("cat") {
            execute {
                val json = kget("http://random.cat/meow").jsonObject
                it.respond(json.getString("file"))
            }
        }

        command("bird") {
            execute {
                val json = kget("https://birdsare.cool/bird.json?exclude=webm,mp4").jsonObject
                it.respond(json.getString("url"))
            }
        }

        command("flip") {
            execute {
                val message = if (Random().nextBoolean()) "Heads" else "tails"
                it.respond(message)
            }
        }

        command("dog") {
            execute {
                val json = kget("https://dog.ceo/api/breeds/image/random").jsonObject
                it.respond(json.getString("message"))
            }
        }

        command("google") {
            expect(ArgumentType.Sentence)
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
            expect(ArgumentType.Sentence)
            execute {
                val sentence = it.args[0] as String

                val response = parseCowsayArgs(sentence.split(" "))
                if(!response.isBlank()){
                    it.safeRespond(response)
                    return@execute
                }

                val specialChars = Regex("(`+|@|<@[0-9]+>)")
                val args = sentence
                        .replace("\n", " ")
                        .replace(specialChars, "")
                        .split(" ")
                        .toTypedArray()

                val result = Cowsay.say(args)

                if(!result.isBlank()){
                    val response = Cowsay.say(args)
                    if (response.length > 1994){
                        it.safeRespond("that message was too long, moo!")
                        return@execute
                    }

                    it.safeRespond("```" + Cowsay.say(args) + "```")
                }
            }
        }
    }


private fun parseCowsayArgs(arguments: List<String>): String {
    val flagsWithArgs = Regex("-T|-W|-f|-e|--alt|--lang")
    val flagsWithNoArgs = Regex("-b|-d|-g|-l|-n|-p|-s|-t|-w|-y")

    if(!arguments.mapIndexedNotNull{index, s -> if (flagsWithArgs.matches(s)) index + 1 else null }
            .all { it < arguments.size && !arguments[it].startsWith("-") && !arguments[it].contains(Regex("/|\\\\")) }){
        return "one of your flags is missing an argument, or the supplied argument is invalid"
    }

    var skipNextArg = false
    arguments.forEach{
        if (!skipNextArg) {
            if(flagsWithArgs.matches(it)){
                skipNextArg = true
            }
            if(it == "-h"){
                return "```" + Scanner(File("cowsayhelp.txt")).useDelimiter("\\Z").next() + "```"
            }
            if(!flagsWithArgs.matches(it) && !flagsWithNoArgs.matches(it)){
                return ""
            }
        }
        else {
            skipNextArg = false
        }
    }
    return ""
}