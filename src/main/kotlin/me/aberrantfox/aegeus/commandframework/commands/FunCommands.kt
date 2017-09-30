package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import khttp.get
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.listeners.CommandEvent
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