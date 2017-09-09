package me.aberrantfox.aegeus.commandframework.commands

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.produceCommandMap
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.CommandRecommender
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.io.File

val macroMap = loadMacroMap()
private val mapLocation = "macros.json"

@Command(ArgumentType.String, ArgumentType.Joiner)
fun addMacro(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val key = (args[0] as String).toLowerCase()

    if(produceCommandMap().containsKey(key)) {
        guildEvent.channel.sendMessage("You dummy. There is a command with that name already...").queue()
        return
    } else if (macroMap.containsKey(key)) {
        guildEvent.channel.sendMessage("Yea... that macro exists...").queue()
        return
    }

    val value = guildEvent.message.rawContent.substring("addmacro ".length + key.length + config.prefix.length + 1)

    macroMap[key] = value
    guildEvent.channel.sendMessage("**$key** will now respond with: **$value**").queue()

    saveMacroMap(macroMap)
    CommandRecommender.addPossibility(key)
}

@Command(ArgumentType.String)
fun removeMacro(event: CommandEvent) {
    val (guildEvent, args) = event
    val key = (args[0] as String).toLowerCase()

    if(macroMap.containsKey(key)) {
        macroMap.remove(key)
        saveMacroMap(macroMap)
        CommandRecommender.removePossibility(key)
        guildEvent.channel.sendMessage("$key - this macro is now gone.").queue()
        return
    }

    guildEvent.channel.sendMessage("$key isn't a macro... ").queue()
}

@Command
fun listMacros(event: CommandEvent) {
    val macros = macroMap.keys.reduce { acc, s -> "$acc, $s" }
    event.guildEvent.channel.sendMessage("Currently available macros: $macros.").queue()
}

private fun loadMacroMap(): HashMap<String, String> {
    val file = File(mapLocation)
    val gson = Gson()

    if( !(file.exists()) ) {
        return HashMap()
    }

    val type = object : TypeToken<HashMap<String, String>>() {}.type
    val map = gson.fromJson<HashMap<String, String>>(file.readText(), type)

    return map
}

private fun saveMacroMap(map: HashMap<String, String>) {
    val gson = Gson()
    val json = gson.toJson(map)
    val file = File(mapLocation)

    file.delete()
    file.printWriter().use { out -> out.println(json) }
}