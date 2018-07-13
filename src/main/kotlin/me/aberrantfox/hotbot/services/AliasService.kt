package me.aberrantfox.hotbot.services

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.internal.command.CommandRecommender
import java.io.File


class AliasService(private val manager: PermissionManager,
                   private val aliasesLocation: String = "config/aliases.json") {
    lateinit var container: CommandsContainer

    val aliases: HashMap<String, String> = hashMapOf()

    fun loadAliases() {
        val file = File(aliasesLocation)
        val json = if (!file.exists()) {
            AliasService::class.java.getResource("/default-aliases.json").readText()
        } else {
            file.readText()
        }

        val gson = Gson()
        gson.fromJson<HashMap<String, String>>(json)
            .forEach { add(it.key to it.value) }
    }

    private fun saveAliases() {
        val gson = Gson()
        val json = gson.toJson(aliases)
        val file = File(aliasesLocation)

        file.delete()
        file.printWriter().use { out -> out.println(json) }
    }

    enum class CreationResult {
        Success, InvalidCommand, UnavailableName
    }

    fun add(pair: Pair<String, String>): CreationResult {
        val (alias, target) = pair

        val targetCommand = container[target] ?: return CreationResult.InvalidCommand
        if (container.has(alias)) return CreationResult.UnavailableName

        container.command(alias) {
            description = "Alias of $target\n${targetCommand.description}"
            category = "aliases"
            expectedArgs = targetCommand.expectedArgs
            execute = targetCommand.execute
            requiresGuild = targetCommand.requiresGuild
        }

        manager.setPermission(alias, manager.roleRequired(target))
        aliases[alias] = target
        CommandRecommender.addPossibility(alias)
        saveAliases()
        return CreationResult.Success
    }

    fun remove(alias: String): Boolean {
        return if (alias in aliases) {
            aliases.remove(alias)
            manager.removePermissions(alias)
            CommandRecommender.removePossibility(alias)
            saveAliases()
            true
        }
        else false
    }
}