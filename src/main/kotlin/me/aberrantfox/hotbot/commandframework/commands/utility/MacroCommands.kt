package me.aberrantfox.hotbot.commandframework.commands.utility

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.arg
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
val macros = loadMacroList()

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
                saveMacroList(macros)

                CommandRecommender.addPossibility(name)

                it.safeRespond("**$name** (category: **$category**) will now respond with: **$message**")
            }

        }

        command("editmacro") {
            expect(ArgumentType.Macro, ArgumentType.Sentence)
            execute {
                val macro = it.args.component1() as Macro
                val name = macro.name
                val message = it.args.component2() as String

                macros.remove(macro)
                macros.add(macro.copy(message=message))

                saveMacroList(macros)

                it.safeRespond("**$name** (category: **${macro.category}**) will now respond with: **$message**")
            }
        }

        command("setmacrocategories") {
            expect(ArgumentType.Splitter)
            execute {
                val splitArgs = it.args.component1() as List<String>

                if (splitArgs.size > 2) {
                    return@execute it.respond("Too many arguments passed. Pass macros and a category only.")
                }

                val macroArgs = splitArgs.firstOrNull()?.trim()?.split(' ')
                        ?: return@execute it.respond("Must pass at least one macro")

                val newCategory = splitArgs.getOrNull(1)?.toLowerCase()?.trim()
                        ?: return@execute it.respond("Must pass a category.")

                macroArgs.forEach { arg ->
                    val macro = macros.find { it.name.toLowerCase() == arg.toLowerCase() }
                            ?: return@execute it.safeRespond("Couldn't find macro: $arg")

                    macros.remove(macro)
                    macros.add(macro.copy(category=newCategory))
                }

                saveMacroList(macros)

                it.safeRespond("${macroArgs.joinToString(", ")} moved to $newCategory")
            }
        }

        command("renamemacro") {
            expect(ArgumentType.Macro, ArgumentType.Word)
            execute {
                val oldMacro = it.args.component1() as Macro

                val newName = (it.args.component2() as String).toLowerCase()

                if (it.container.has(newName))
                    return@execute it.safeRespond("A command already exists with the name $newName")

                if (macros.any { it.name.toLowerCase() == newName })
                    return@execute it.safeRespond("The macro $newName already exists.")

                macros.remove(oldMacro)
                macros.add(oldMacro.copy(name=newName))

                saveMacroList(macros)

                val oldName = oldMacro.name

                CommandRecommender.removePossibility(oldName)
                CommandRecommender.addPossibility(newName)

                it.safeRespond("**$oldName** renamed to **$newName**")
            }
        }

        command("removemacro") {
            expect(ArgumentType.Macro)
            execute {
                val macro = it.args.component1() as Macro

                macros.remove(macro)
                saveMacroList(macros)

                CommandRecommender.removePossibility(macro.name)

                it.safeRespond("${macro.name} - this macro is now gone.")
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

                saveMacroList(macros)

                it.safeRespond("${toRemove.size} macros removed.")
            }
        }

        command("listmacros") {
            execute {
                val grouped = macros.groupBy { it.category.toLowerCase() }

                val macroEmbed = buildMacrosEmbed(grouped)

                it.respond(macroEmbed)
            }
        }
    }

private fun buildMacrosEmbed(groupedMacros: Map<String, List<Macro>>) =
        embed {
            title("Currently Available Macros")

            setColor(Color.GREEN)

            groupedMacros.toList().sortedByDescending { it.second.size }.forEach { (categoryName, macros) ->
                field {
                    name = categoryName.capitalize()
                    value = macros.map { it.name }.sorted().joinToString(", ")
                    inline = false
                }
            }
        }

private fun loadMacroList(): MutableList<Macro> {
    val file = File(mapLocation)
    val gson = Gson()

    if (!(file.exists())) {
        return mutableListOf()
    }

    val macros = gson.fromJson<MutableList<Macro>>(file.readText())

    return macros
}

private fun saveMacroList(macros: List<Macro>) {
    val gson = Gson()
    val json = gson.toJson(macros)
    val file = File(mapLocation)

    file.delete()
    file.printWriter().use { out -> out.println(json) }
}
