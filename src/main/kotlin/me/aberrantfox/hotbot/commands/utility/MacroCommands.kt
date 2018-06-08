package me.aberrantfox.hotbot.commands.utility

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.aberrantfox.hotbot.commands.MacroArg
import me.aberrantfox.hotbot.services.configPath
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.internal.command.CommandRecommender
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
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
            expect(WordArg, WordArg, SentenceArg)
            execute {
                val name = (it.args.component1() as String).toLowerCase()
                val category = (it.args.component2() as String).toLowerCase()
                val message = it.args.component3() as String

                if (macros.any { it.name.toLowerCase() == name }) {
                    it.respond("Yea... that macro exists...")
                    return@execute
                }

                if (it.container.has(name)) {
                    it.respond("You dummy. There is a command with that name already...")
                    return@execute
                }

                macros.add(Macro(name, message, category))
                saveMacroList(macros)

                it.container.command(name, { execute { it.respond(message) } })

                CommandRecommender.addPossibility(name)

                it.safeRespond("**$name** (category: **$category**) will now respond with: **$message**")
            }

        }

        command("editmacro") {
            expect(MacroArg, SentenceArg)
            execute {
                val macro = it.args.component1() as Macro
                val name = macro.name.toLowerCase()
                val message = it.args.component2() as String

                macros.remove(macro)
                it.container.commands.remove(name)

                macros.add(macro.copy(message=message))
                it.container.command(name, { execute { it.respond(message) } })

                saveMacroList(macros)

                it.safeRespond("**$name** (category: **${macro.category}**) will now respond with: **$message**")
            }
        }

        command("setmacrocategories") {
            expect(SplitterArg)
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
            expect(MacroArg, WordArg)
            execute {
                val oldMacro = it.args.component1() as Macro

                val newName = (it.args.component2() as String).toLowerCase()

                if (it.container.has(newName))
                    return@execute it.safeRespond("A command already exists with the name $newName")

                if (macros.any { it.name.toLowerCase() == newName })
                    return@execute it.safeRespond("The macro $newName already exists.")

                macros.remove(oldMacro)
                it.container.commands.remove(oldMacro.name)

                macros.add(oldMacro.copy(name=newName))
                it.container.command(newName, { execute { it.respond(oldMacro.message) } })

                saveMacroList(macros)

                val oldName = oldMacro.name

                CommandRecommender.removePossibility(oldName)
                CommandRecommender.addPossibility(newName)

                it.safeRespond("**$oldName** renamed to **$newName**")
            }
        }

        command("removemacro") {
            expect(MacroArg)
            execute {
                val macro = it.args.component1() as Macro

                macros.remove(macro)
                saveMacroList(macros)

                it.container.commands.remove(macro.name)

                CommandRecommender.removePossibility(macro.name)

                it.safeRespond("${macro.name} - this macro is now gone.")
            }
        }

        command("removemacros") {
            expect(WordArg)
            execute {
                val categoryName = (it.args.component1() as String).toLowerCase()

                val toRemove = macros.filter { it.category.toLowerCase() == categoryName }

                if (toRemove.isEmpty()) {
                    it.safeRespond("$categoryName isn't the name of a known macro category.")
                    return@execute
                }

                toRemove.forEach { macro ->
                    macros.remove(macro)
                    it.container.commands.remove(macro.name)

                    CommandRecommender.removePossibility(macro.name)
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
