package me.aberrantfox.hotbot.commandframework.commands.utility

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.services.CommandRecommender
import me.aberrantfox.hotbot.services.configPath
import java.io.File

data class Macro(@SerializedName("name") val name: String,
                 @SerializedName("message") val message: String,
                 @SerializedName("category") val category: String)

private val mapLocation = configPath("macros.json")
val macros = loadMacroMap()

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
                val macros = macroMap.keys.sorted()
                val macroString =
                        if (macros.isEmpty())
                            "none"
                        else
                            macros.joinToString(", ")

                it.respond("Currently available macros: $macroString.")
            }
        }

    }

private fun buildMacrosEmbed(groupedMacros: Map<String, List<Macro>>) =
        embed {
            title("Currently Available Macros")

            groupedMacros.toList().sortedByDescending { it.second.size }.forEach { (macroName, macros) ->
                field {
                    name = macroName
                    value = macros.map { it.name }.sorted().joinToString(", ")
                    inline = false
                }
            }
        }

private fun loadMacroMap(): MutableList<Macro> {
    val file = File(mapLocation)
    val gson = Gson()

    if (!(file.exists())) {
        return mutableListOf()
    }

    val macros = gson.fromJson<MutableList<Macro>>(file.readText())
    
    return macros
}

private fun saveMacroMap(macros: List<Macro>) {
    val gson = Gson()
    val json = gson.toJson(macros)
    val file = File(mapLocation)

    file.delete()
    file.printWriter().use { out -> out.println(json) }
}
