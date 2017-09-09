package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import khttp.get
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.listeners.CommandEvent
import java.util.*

@Command
fun cat(event: CommandEvent) {
    val json = get("http://random.cat/meow").jsonObject
    event.guildEvent.channel.sendMessage(json.getString("file")).queue()
}

@Command(ArgumentType.Manual)
fun ball(event: CommandEvent) {
    val json = get("https://8ball.delegator.com/magic/JSON/abc").jsonObject
    event.guildEvent.channel.sendMessage(json.getJSONObject("magic").getString("answer")).queue()
}

@Command
fun flip(event: CommandEvent) {
    val message = if (Random().nextBoolean()) "Heads" else "tails"
    event.guildEvent.channel.sendMessage(message).queue()
}

@Command
fun dog(event: CommandEvent) {
    val json = get("https://dog.ceo/api/breeds/image/random").jsonObject
    event.guildEvent.channel.sendMessage(json.getString("message")).queue()
}