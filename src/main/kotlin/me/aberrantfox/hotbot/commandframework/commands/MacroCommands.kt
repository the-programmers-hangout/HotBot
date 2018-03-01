package me.aberrantfox.hotbot.commandframework.commands

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.services.CommandRecommender
import me.aberrantfox.hotbot.services.configPath
import java.io.File

private val mapLocation = configPath("macros.json")
val macroMap = loadMacroMap()

@CommandSet
fun macroCommands() =
    commands {
        command("addmacro") {
            expect(ArgumentType.Word, ArgumentType.Sentence)
            execute {
                val key = (it.args[0] as String).toLowerCase()

                when {
                    it.container.has(key) -> it.respond("You dummy. There is a command with that name already...")

                    macroMap.containsKey(key) -> it.respond("Yea... that macro exists...")

                    else -> {
                        val value = it.message.contentRaw.substring("addmacro ".length + key.length + it.config.serverInformation.prefix.length + 1)

                        macroMap[key] = value
                        it.respond("**$key** will now respond with: **$value**")

                        saveMacroMap(macroMap)
                        CommandRecommender.addPossibility(key)
                    }
                }
            }
        }

        command("removemacro") {
            expect(ArgumentType.Word)
            execute {
                val key = (it.args[0] as String).toLowerCase()

                if (macroMap.containsKey(key)) {
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
                val macros = macroMap.keys.toTypedArray().sortedArray()
                val macroString =
                        if (macros.isEmpty())
                            "none"
                        else
                            macros.joinToString(", ")

                it.respond("Currently available macros: $macroString.")
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
