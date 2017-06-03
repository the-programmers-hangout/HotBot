package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import khttp.get
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Command
fun cat(event: MessageReceivedEvent) {
    val json = get("http://random.cat/meow").jsonObject
    event.channel.sendMessage(json.getString("file")).queue()
}