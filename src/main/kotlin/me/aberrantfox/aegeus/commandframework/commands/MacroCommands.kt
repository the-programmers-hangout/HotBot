package me.aberrantfox.aegeus.commandframework.commands

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.commands.dsl.commands
import me.aberrantfox.aegeus.commandframework.produceContainer
import me.aberrantfox.aegeus.services.CommandRecommender
import java.io.File

val macroMap = loadMacroMap()
private val mapLocation = "macros.json"

fun macroCommands() =
    commands {
        command("addmacro") {
            expect(ArgumentType.String, ArgumentType.Joiner)
            execute {
                val key = (it.args[0] as String).toLowerCase()

                when {
                    produceContainer().commands.containsKey(key) -> it.respond("You dummy. There is a command with that name already...")
                    macroMap.containsKey(key) -> it.respond("Yea... that macro exists...")
                    else -> {
                        val value = it.message.rawContent.substring("addmacro ".length + key.length + it.config.prefix.length + 1)

                        macroMap[key] = value
                        it.respond("**$key** will now respond with: **$value**")

                        saveMacroMap(macroMap)
                        CommandRecommender.addPossibility(key)
                    }
                }
            }
        }

        command("removemacro") {
            expect(ArgumentType.String)
            execute {
                val key = (it.args[0] as String).toLowerCase()

                if(macroMap.containsKey(key)) {
                    macroMap.remove(key)
                    saveMacroMap(macroMap)
                    CommandRecommender.removePossibility(key)
                    it.respond("$key - this macro is now gone.")
                } else {
                    it.respond("$key isn't a macro... ")
                }
            }
        }

        command("listmacros") {
            execute {
                val macros = macroMap.keys.reduce { acc, s -> "$acc, $s" }
                it.respond("Currently available macros: $macros.")
            }
        }

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