package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import khttp.get
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.CommandEvent
import org.jsoup.Jsoup
import java.net.URLEncoder

import java.util.*

@Command
fun cat(event: CommandEvent) {
    val json = get("http://random.cat/meow").jsonObject
    event.respond(json.getString("file"))
}

@Command(ArgumentType.Joiner)
fun ball(event: CommandEvent) {
    val query = event.args[0] as String
    val json = get("https://8ball.delegator.com/magic/JSON/abc").jsonObject
    event.respond(json.getJSONObject("magic").getString("answer"))
}

@Command
fun flip(event: CommandEvent) {
    val message = if (Random().nextBoolean()) "Heads" else "tails"
    event.respond(message)
}

@Command
fun dog(event: CommandEvent) {
    val json = get("https://dog.ceo/api/breeds/image/random").jsonObject
    event.respond(json.getString("message"))
}

@Command(ArgumentType.Joiner)
fun google(event: CommandEvent) {
    val google = "http://www.google.com/search?q="
    val search = event.args[0] as String
    val charset = "UTF-8"
    val userAgent = "Mozilla/5.0"

    val links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select(".g>.r>a")

    event.respond(links.first().absUrl("href"))
}