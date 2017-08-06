package me.aberrantfox.aegeus.commandframework.commands

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.io.File

val macroMap = loadMacroMap()

@Command(ArgumentType.Manual)
fun addMacro(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val key = args[0] as String
    val value = event.message.rawContent.substring("addmacro ".length + key.length + config.prefix.length + 1)

    macroMap[key.toLowerCase()] = value
    event.channel.sendMessage("**$key** will now respond with: **$value**").queue()
    saveMacroMap(macroMap)
}

@Command(ArgumentType.String)
fun removeMacro() {
    
}

private val gson = Gson()
private val mapLocation = "macros.json"

private fun loadMacroMap(): HashMap<String, String> {
    val map: HashMap<String, String> = HashMap()
    val file = File(mapLocation)

    if( !(file.exists()) ) {
        return map
    }
    val type = object : TypeToken<HashMap<String, String>>() {}.type

    return gson.fromJson(file.readText(), type)
}

fun saveMacroMap(map: HashMap<String, String>) {
    val json = gson.toJson(map)
    val file = File(mapLocation)

    file.delete()
    file.printWriter().use { out -> out.println(json) }
}