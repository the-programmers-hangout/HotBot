package me.aberrantfox.hotbot.commands.utility

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import me.aberrantfox.hotbot.commands.MacroArg
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.configPath
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.internal.command.CommandRecommender
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import net.dv8tion.jda.core.entities.MessageChannel
import org.joda.time.DateTime
import java.awt.Color
import java.io.File

typealias ChannelId = String
typealias Timestamp = Long

data class Macro(val name: String,
                 val message: String,
                 val category: String,
                 @Transient val lastUseTimestamp: MutableMap<ChannelId, Timestamp> = hashMapOf())

private val mapLocation = configPath("macros.json")
val macros = hashMapOf<String, Macro>()


const val macroCommandCategory = "macro-commands"

@CommandSet("macros")
fun macroCommands(permManager: PermissionManager) =
    commands {
        command("addmacro") {
            expect(WordArg, WordArg, SentenceArg)
            execute {
                val name = (it.args.component1() as String).toLowerCase()
                val category = (it.args.component2() as String).toLowerCase()
                val message = it.args.component3() as String

                if (name in macros) {
                    it.respond("Yea... that macro exists...")
                    return@execute
                }

                if (it.container.has(name)) {
                    it.respond("You dummy. There is a command with that name already...")
                    return@execute
                }

                addMacro(Macro(name, message, category), it.container, permManager)
                saveMacroList(macros.values)

                it.safeRespond("**$name** (category: **$category**) will now respond with: **$message**")
            }

        }

        command("editmacro") {
            expect(MacroArg, SentenceArg)
            execute {
                val macro = it.args.component1() as Macro
                val name = macro.name.toLowerCase()
                val message = it.args.component2() as String

                removeMacro(macro, it.container, permManager)
                addMacro(macro.copy(message=message), it.container, permManager)

                saveMacroList(macros.values)

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

                macroArgs.map { it.toLowerCase() }
                         .forEach { arg ->
                             val macro = macros[arg]
                                     ?: return@execute it.safeRespond("Couldn't find macro: $arg")

                             removeMacro(macro, it.container, permManager)
                             addMacro(macro.copy(category=newCategory), it.container, permManager)
                         }

                saveMacroList(macros.values)

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

                if (newName in macros)
                    return@execute it.safeRespond("The macro $newName already exists.")

                removeMacro(oldMacro, it.container, permManager)
                addMacro(oldMacro.copy(name=newName), it.container, permManager)

                saveMacroList(macros.values)

                it.safeRespond("**${oldMacro.name}** renamed to **$newName**")
            }
        }

        command("removemacro") {
            expect(MacroArg)
            execute {
                val macro = it.args.component1() as Macro

                removeMacro(macro, it.container, permManager)

                it.safeRespond("${macro.name} - this macro is now gone.")
            }
        }

        command("removemacros") {
            expect(WordArg)
            execute {
                val categoryName = (it.args.component1() as String).toLowerCase()

                val toRemove = macros.filterValues { it.category.toLowerCase() == categoryName }

                if (toRemove.isEmpty()) {
                    it.safeRespond("$categoryName isn't the name of a known macro category.")
                    return@execute
                }

                toRemove.values.forEach { macro -> removeMacro(macro, it.container, permManager) }

                saveMacroList(macros.values)

                it.safeRespond("${toRemove.size} macros removed.")
            }
        }

        command("listmacros") {
            execute {
                val grouped = macros.values.groupBy { it.category.toLowerCase() }

                val macroEmbed = buildMacrosEmbed(grouped)

                it.respond(macroEmbed)
            }
        }
    }

fun setupMacroCommands(container: CommandsContainer, manager: PermissionManager) = loadMacroList().forEach { addMacro(it, container, manager) }

fun addMacro(macro: Macro, container: CommandsContainer, manager: PermissionManager) {
    macros[macro.name.toLowerCase()] = macro
    container.command(macro.name, {
        category = macroCommandCategory
        expect(arg(SentenceArg, optional = true, default = ""))
        execute { it.respond(macro.message) }
    })
    CommandRecommender.addPossibility(macro.name)
    manager.setPermission(macro.name, PermissionLevel.Everyone)
}

fun removeMacro(macro: Macro, container: CommandsContainer, manager: PermissionManager) {
    macros.remove(macro.name)
    container.commands.remove(macro.name)
    CommandRecommender.removePossibility(macro.name)
}

private fun buildMacrosEmbed(groupedMacros: Map<String, Collection<Macro>>) =
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

private fun loadMacroList(): List<Macro> {
    val file = File(mapLocation)
    val gson = Gson()

    if (!(file.exists())) {
        return listOf()
    }

    return gson.fromJson<List<Macro>>(file.readText())
               .map { it.copy(lastUseTimestamp = hashMapOf()) } // hack, because gson was setting the transient field as null, not the default value
}

private fun saveMacroList(macros: Collection<Macro>) {
    val gson = Gson()
    val json = gson.toJson(macros)
    val file = File(mapLocation)

    file.delete()
    file.printWriter().use { out -> out.println(json) }
}

fun canUseMacro(macro: Macro, channel: MessageChannel, delay: Int): Boolean {
    if (delay <= 0) return true

    val previousTime = macro.lastUseTimestamp[channel.id]

    if (previousTime?.let { timeToDifference(it) < -delay * 1000} != false) {
        macro.lastUseTimestamp[channel.id] = DateTime.now().millis

        return true
    }

    return false
}