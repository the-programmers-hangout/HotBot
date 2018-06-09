package me.aberrantfox.hotbot.commands.utility

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import me.aberrantfox.hotbot.commands.MacroArg
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.configPath
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.internal.command.CommandRecommender
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageChannel
import org.joda.time.DateTime
import java.awt.Color
import java.io.File

data class Macro(@SerializedName("name") val name: String,
                 @SerializedName("message") val message: String,
                 @SerializedName("category") val category: String)

private val mapLocation = configPath("macros.json")
val macros = loadMacroList()

// ChannelId -> (Macro -> Timestamp)
var macroPreviousTime = hashMapOf<String, HashMap<String, Long>>()

@CommandSet
fun macroCommands(permManager: PermissionManager) =
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

                addMacro(Macro(name, message, category), it.container, permManager)
                saveMacroList(macros)

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

                    removeMacro(macro, it.container, permManager)
                    addMacro(macro.copy(category=newCategory), it.container, permManager)
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

                removeMacro(oldMacro, it.container, permManager)
                addMacro(oldMacro.copy(name=newName), it.container, permManager)

                saveMacroList(macros)

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

                val toRemove = macros.filter { it.category.toLowerCase() == categoryName }

                if (toRemove.isEmpty()) {
                    it.safeRespond("$categoryName isn't the name of a known macro category.")
                    return@execute
                }

                toRemove.forEach { macro -> removeMacro(macro, it.container, permManager) }

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

fun setupMacroCommands(container: CommandsContainer, manager: PermissionManager, config: Configuration, guilds: List<Guild>) {
    macros.forEach { macro ->
        container.command(macro.name, { execute { if (checkMacroDelay(macro, it.channel, config)) it.respond(macro.message) } })
        CommandRecommender.addPossibility(macro.name)
        manager.setPermission(macro.name, PermissionLevel.Everyone)
    }
    macroPreviousTime.putAll(guilds.map{ it.textChannels }.flatten().associate { it.id to hashMapOf<String, Long>() } )
}

fun addMacro(macro: Macro, container: CommandsContainer, manager: PermissionManager) {
    macros.add(macro)
    container.command(macro.name, { execute { it.respond(macro.message) } })
    CommandRecommender.addPossibility(macro.name)
    manager.setPermission(macro.name, PermissionLevel.Everyone)
}

fun removeMacro(macro: Macro, container: CommandsContainer, manager: PermissionManager) {
    macros.remove(macro)
    container.commands.remove(macro.name)
    CommandRecommender.removePossibility(macro.name)
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

fun checkMacroDelay(macro: Macro, channel: MessageChannel, config: Configuration): Boolean {
    val delay = config.serverInformation.macroDelay * 1000
    if (delay <= 0)
        return true

    val channelMap = macroPreviousTime[channel.id]
    val previousTime = channelMap?.get(macro.name)

    if (previousTime?.let { timeToDifference(it) < -delay} != false) {
        val now = DateTime.now().millis
        channelMap?.set(macro.name, now)

        return true
    }

    return false
}