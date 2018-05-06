package me.aberrantfox.hotbot.commandframework.commands.utility

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.services.CommandRecommender
import me.aberrantfox.hotbot.services.configPath
import java.awt.Color
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
            expect(ArgumentType.Word, ArgumentType.Word, ArgumentType.Sentence)
            execute {
                val name = (it.args.component1() as String).toLowerCase()
                val category = (it.args.component2() as String).toLowerCase()
                val message = it.args.component3() as String

                if (it.container.has(name)) {
                    it.respond("You dummy. There is a command with that name already...")
                    return@execute
                }

                if (macros.any { it.name.toLowerCase() == name }) {
                    it.respond("Yea... that macro exists...")
                    return@execute
                }

                macros.add(Macro(name, message, category))
                saveMacroMap(macros)

                CommandRecommender.addPossibility(name)

                it.safeRespond("**$name** (category: **$category**) will now respond with: **$message**")
            }

        }

        command("editmacro") {
            expect(ArgumentType.Word, ArgumentType.Sentence)
            execute {
                val name = (it.args.component1() as String).toLowerCase()
                val message = it.args.component2() as String

                val macro = macros.firstOrNull { it.name.toLowerCase() == name }
                    ?: return@execute it.safeRespond("$name isn't a macro.")

                macros.remove(macro)
                macros.add(macro.copy(message=message))

                saveMacroMap(macros)

                it.safeRespond("**$name** (category: **${macro.category}**) will now respond with: **$message**")
            }
        }

        command("setmacrocategory") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val name = (it.args.component1() as String).toLowerCase()
                val category = (it.args.component2() as String).toLowerCase()

                val macro = macros.firstOrNull { it.name.toLowerCase() == name }
                    ?: return@execute it.safeRespond("$name isn't a macro.")

                macros.remove(macro)
                macros.add(macro.copy(category=category))

                saveMacroMap(macros)

                it.safeRespond("**$name** category changed from **${macro.category}** to **$category**")
            }
        }

        command("renamemacro") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val oldName = (it.args.component1() as String).toLowerCase()
                val newName = (it.args.component2() as String).toLowerCase()

                val macro = macros.firstOrNull { it.name.toLowerCase() == oldName }
                        ?: return@execute it.safeRespond("$oldName isn't a macro.")

                macros.remove(macro)
                macros.add(macro.copy(name=newName))

                saveMacroMap(macros)

                CommandRecommender.removePossibility(oldName)
                CommandRecommender.addPossibility(newName)

                it.safeRespond("**$oldName** renamed to **$newName**")
            }
        }

        command("removemacro") {
            expect(ArgumentType.Word)
            execute {
                val name = (it.args.component1() as String).toLowerCase()

                val macro = macros.firstOrNull { it.name.toLowerCase() == name }
                        ?: return@execute it.safeRespond("$name isn't a macro.")

                macros.remove(macro)
                saveMacroMap(macros)

                CommandRecommender.removePossibility(name)

                it.safeRespond("$name - this macro is now gone.")
            }
        }

        command("removemacros") {
            expect(ArgumentType.Word)
            execute {
                val categoryName = (it.args.component1() as String).toLowerCase()

                val toRemove = macros.filter { it.category.toLowerCase() == categoryName }

                if (toRemove.isEmpty()) {
                    it.safeRespond("$categoryName isn't the name of a known macro category.")
                    return@execute
                }

                toRemove.forEach {
                    macros.remove(it)
                    CommandRecommender.removePossibility(it.name)
                }

                saveMacroMap(macros)

                it.safeRespond("${toRemove.size} macros removed.")
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
