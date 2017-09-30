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
    val key = (event.args[0] as String).toLowerCase()

    if(produceCommandMap().containsKey(key)) {
        event.respond("You dummy. There is a command with that name already...")
        return
    } else if (macroMap.containsKey(key)) {
        event.respond("Yea... that macro exists...")
        return
    }

    val value = event.message.rawContent.substring("addmacro ".length + key.length + event.config.prefix.length + 1)

    macroMap[key] = value
    event.respond("**$key** will now respond with: **$value**")

    saveMacroMap(macroMap)
    CommandRecommender.addPossibility(key)
}

@Command(ArgumentType.String)
fun removeMacro(event: CommandEvent) {
    val key = (event.args[0] as String).toLowerCase()

    if(macroMap.containsKey(key)) {
        macroMap.remove(key)
        saveMacroMap(macroMap)
        CommandRecommender.removePossibility(key)
        event.respond("$key - this macro is now gone.")
        return
    }

    event.respond("$key isn't a macro... ")
}

@Command
fun listMacros(event: CommandEvent) {
    val macros = macroMap.keys.reduce { acc, s -> "$acc, $s" }
    event.respond("Currently available macros: $macros.")
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