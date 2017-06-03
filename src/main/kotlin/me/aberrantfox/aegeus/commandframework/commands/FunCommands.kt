package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import khttp.get
import me.aberrantfox.aegeus.commandframework.ArgumentType
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

@Command
fun cat(event: MessageReceivedEvent) {
    val json = get("http://random.cat/meow").jsonObject
    event.channel.sendMessage(json.getString("file")).queue()
}

@Command(ArgumentType.MANUAL)
fun ball(event: MessageReceivedEvent) {
    val json = get("https://8ball.delegator.com/magic/JSON/abc").jsonObject
    event.channel.sendMessage(json.getJSONObject("magic").getString("answer")).queue()
}

@Command
fun flip(event: MessageReceivedEvent) {
    val message = if (Random().nextBoolean()) "Heads" else "tails"
    event.channel.sendMessage(message).queue()
}

