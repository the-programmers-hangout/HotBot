package me.aberrantfox.hotbot.commands.utility


import me.aberrantfox.hotbot.arguments.MacroArg
import me.aberrantfox.hotbot.extensions.createContinuableField
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.api.annotation.Data
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.internal.arguments.*
import me.aberrantfox.kjdautils.internal.command.CommandRecommender
import me.aberrantfox.kjdautils.internal.di.PersistenceService
import net.dv8tion.jda.api.entities.MessageChannel
import org.joda.time.DateTime
import java.awt.Color

typealias ChannelId = String
typealias Timestamp = Long

data class Macro(val name: String,
                 val message: String,
                 val category: String,
                 @Transient val lastUseTimestamp: MutableMap<ChannelId, Timestamp> = hashMapOf())

@Data("config/macros.json")
data class Macros(val map: HashMap<String, Macro>)

const val macroCommandCategory = ""

@CommandSet("macros")
fun macroCommands(permService: PermissionService,
                  macros: Macros,
                  container: CommandsContainer,
                  manager: PermissionService,
                  persistenceService: PersistenceService) =
    commands {
        macros.map.forEach {
            addMacro(it.value, container, manager, macros)
        }
        command("addmacro") {
            description = "Add a macro which will respond with the given message when invoked by the given name."
            expect(WordArg("Name"), WordArg("Category"), SentenceArg("Message"))
            execute {
                val name = (it.args.component1() as String).toLowerCase()
                val category = (it.args.component2() as String).toLowerCase()
                val message = it.args.component3() as String

                if (name in macros.map) {
                    it.respond("Yea... that macro exists...")
                    return@execute
                }

                if (it.container.has(name)) {
                    it.respond("You dummy. There is a command with that name already...")
                    return@execute
                }

                addMacro(Macro(name, message, category), it.container, permService, macros)
                persistenceService.save(macros)

                it.respond("**$name** (category: **$category**) will now respond with: **$message**")
            }

        }

        command("editmacro") {
            description = "Change a macro's response message"
            expect(MacroArg, SentenceArg("New Message"))
            execute {
                val macro = it.args.component1() as Macro
                val name = macro.name.toLowerCase()
                val message = it.args.component2() as String

                removeMacro(macro, it.container, permService, macros)
                addMacro(macro.copy(message=message), it.container, permService, macros)

                persistenceService.save(macros)

                it.respond("**$name** (category: **${macro.category}**) will now respond with: **$message**")
            }
        }

        command("setmacrocategories") {
            description = "Move one or many macros to a category."
            expect(MultipleArg(MacroArg), WordArg("Category"))
            execute {
                val macroArgs = it.args.component1() as List<Macro>
                val newCategory = it.args.component2() as String

                macroArgs.forEach { macro ->
                             removeMacro(macro, it.container, permService, macros)
                             addMacro(macro.copy(category=newCategory), it.container, permService, macros)
                         }

                persistenceService.save(macros)

                it.respond("${macroArgs.joinToString(", ") { it.name }} moved to $newCategory")
            }
        }

        command("renamemacro") {
            description = "Change a macro's name, keeping the original response"
            expect(MacroArg, WordArg("New Name"))
            execute {
                val oldMacro = it.args.component1() as Macro

                val newName = (it.args.component2() as String).toLowerCase()

                if (it.container.has(newName))
                    return@execute it.respond("A command already exists with the name $newName")

                if (newName in macros.map)
                    return@execute it.respond("The macro $newName already exists.")

                removeMacro(oldMacro, it.container, permService, macros)
                addMacro(oldMacro.copy(name=newName), it.container, permService, macros)

                persistenceService.save(macros)

                it.respond("**${oldMacro.name}** renamed to **$newName**")
            }
        }

        command("removemacro") {
            description = "Removes a macro with the given name"
            expect(MacroArg)
            execute {
                val macro = it.args.component1() as Macro

                removeMacro(macro, it.container, permService, macros)

                it.respond("${macro.name} - this macro is now gone.")
            }
        }

        command("removemacros") {
            description = "Removes a whole category of macros"
            expect(WordArg("Category"))
            execute {
                val categoryName = (it.args.component1() as String).toLowerCase()

                val toRemove = macros.map.filterValues { it.category.toLowerCase() == categoryName }

                if (toRemove.isEmpty()) {
                    it.respond("$categoryName isn't the name of a known macro category.")
                    return@execute
                }

                toRemove.values.forEach { macro -> removeMacro(macro, it.container, permService, macros) }

                persistenceService.save(macros)

                it.respond("${toRemove.size} macros removed.")
            }
        }

        command("listmacros") {
            description = "List all of the currently available macros."
            execute {
                val grouped = macros.map.values.groupBy { it.category.toLowerCase() }

                val macroEmbed = buildMacrosEmbed(grouped)

                it.respond(macroEmbed)
            }
        }
    }



fun addMacro(macro: Macro, container: CommandsContainer, manager: PermissionService, macros: Macros) {
    macros.map[macro.name.toLowerCase()] = macro
    val command = container.command(macro.name) {
        category = macroCommandCategory
        expect(arg(SentenceArg, optional = true, default = ""))
        execute { it.respond(macro.message) }
    }
    CommandRecommender.addPossibility(command!!)
    manager.setPermission(macro.name, PermissionLevel.Everyone)
}

fun removeMacro(macro: Macro, container: CommandsContainer, manager: PermissionService, macros: Macros) {
    macros.map.remove(macro.name)
    val command = container.commands.remove(macro.name)
    manager.removePermissions(macro.name)
    CommandRecommender.removePossibility(command!!)
}

private fun buildMacrosEmbed(groupedMacros: Map<String, Collection<Macro>>) =
        embed {
            title = "Currently Available Macros"
            color = Color.GREEN

            groupedMacros.toList()
                .sortedByDescending { it.second.size }
                .forEach { (categoryName, macros) ->
                    val categoryList = macros.map { it.name }.sorted().joinToString(", ")
                    createContinuableField(categoryName.capitalize(), categoryList)
                }
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