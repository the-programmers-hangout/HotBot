package me.aberrantfox.hotbot.services

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.internal.command.CommandRecommender
import java.io.File

@Service
class AliasService(private val manager: PermissionService,
                   private val container: CommandsContainer) {

    private val aliases: HashMap<String, String> = hashMapOf()
    private val aliasesFile = File("config/aliases.json")

    init {
        loadAliases()
    }

    fun loadAliases() {
        val json = if (!aliasesFile.exists()) {
            AliasService::class.java.getResource("/default-aliases.json").readText()
        } else {
            aliasesFile.readText()
        }

        val gson = Gson()
        gson.fromJson<HashMap<String, String>>(json)
            .forEach { add(it.key to it.value) }
    }

    private fun saveAliases() {
        val gson = Gson()
        val json = gson.toJson(aliases)

        aliasesFile.delete()
        aliasesFile.printWriter().use { out -> out.println(json) }
    }

    enum class CreationResult {
        Success, InvalidCommand, UnavailableName
    }

    fun add(pair: Pair<String, String>): CreationResult {
        val alias = pair.first.toLowerCase()
        var target = pair.second.toLowerCase()

        // if target itself is an alias, make the aliased command the new target
        aliases[target]?.let { target = it }

        val targetCommand = container[target] ?: return CreationResult.InvalidCommand
        if (container.has(alias)) return CreationResult.UnavailableName

        val command = container.command(alias) {
            description = "Alias of $target\n${targetCommand.description}"
            category = "aliases"
            expectedArgs = targetCommand.expectedArgs
            execute = targetCommand.execute
            requiresGuild = targetCommand.requiresGuild
        }

        manager.setPermission(alias, manager.roleRequired(target))
        aliases[alias] = target
        CommandRecommender.addPossibility(command!!)
        saveAliases()
        return CreationResult.Success
    }

    fun remove(alias: String): Boolean {
        return if (alias in aliases) {
            aliases.remove(alias)
            manager.removePermissions(alias)
            val command = container.commands.remove(alias)
            CommandRecommender.removePossibility(command!!)
            saveAliases()
            true
        }
        else false
    }
}