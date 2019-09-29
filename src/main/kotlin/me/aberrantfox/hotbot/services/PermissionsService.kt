package me.aberrantfox.hotbot.services

import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.discord.Discord
import me.aberrantfox.kjdautils.internal.command.tryRetrieveSnowflake
import net.dv8tion.jda.api.entities.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class PermissionLevel {
    Everyone, Member, JrMod, Moderator, Administrator, Owner;
}

data class PermissionsConfiguration(val permissions: ConcurrentHashMap<String, PermissionLevel> = ConcurrentHashMap(),
                                    val roleMappings: ConcurrentHashMap<String, PermissionLevel> = ConcurrentHashMap())
@Service
open class PermissionService(val discord: Discord, private val botConfig: Configuration, val container: CommandsContainer) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val permissionsFile = File("config/permissions.json")
    private val permissionsConfig: PermissionsConfiguration

    init {
        defaultAndPrunePermissions(container)
        permissionsConfig = if (permissionsFile.exists()) {
            gson.fromJson(permissionsFile.readText(), PermissionsConfiguration::class.java)
        } else {
            PermissionsConfiguration()
        }
        save()
    }

    fun defaultAndPrunePermissions(container: CommandsContainer): Job {
        val commandNames = container.commands.map { it.key.toLowerCase() }

        commandNames.filter { !(permissionsConfig.permissions.containsKey(it)) }
                .forEach { permissionsConfig.permissions[it] = PermissionLevel.Administrator }

        permissionsConfig.permissions.toMap()
                .filterKeys { it !in commandNames }
                .forEach { permissionsConfig.permissions.remove(it.key) }

        return GlobalScope.launch { save() }
    }

    fun removePermissions(command: String) {
        permissionsConfig.permissions.remove(command)
        GlobalScope.launch { save() }
    }

    fun save() = synchronized(permissionsFile) { permissionsFile.writeText(gson.toJson(permissionsConfig)) }

    fun setPermission(command: String, level: PermissionLevel): Job {
        permissionsConfig.permissions[command.toLowerCase()] = level
        return GlobalScope.launch { save() }
    }

    fun roleRequired(name: String) =  permissionsConfig.permissions[name.toLowerCase()] ?: PermissionLevel.Owner

    fun canPerformAction(user: User, actionLevel: PermissionLevel) = getPermissionLevel(user) >= actionLevel

    fun canUseCommand(user: User, command: String) = getPermissionLevel(user) >= permissionsConfig.permissions[command.toLowerCase()] ?: PermissionLevel.Owner

    fun assignRoleLevel(role: Role, level: PermissionLevel): Job {
        permissionsConfig.roleMappings[role.id] = level
        return GlobalScope.launch { save() }
    }

    fun roleAssignments() = permissionsConfig.roleMappings.entries

    fun compareUsers(userA: User, userB: User) = getPermissionLevel(userA).compareTo(getPermissionLevel(userB))

    private fun getPermissionLevel(user: User): PermissionLevel {
        if (botConfig.serverInformation.ownerID == user.id) return PermissionLevel.Owner

        val member = tryRetrieveSnowflake(discord.jda) { discord.jda.getGuildById(botConfig.serverInformation.guildid)?.getMember(user) } as Member?
                ?: return PermissionLevel.Everyone

        if (member.roles.isEmpty()) return PermissionLevel.Everyone

        if (member.roles.none { permissionsConfig.roleMappings.containsKey(it.id) }) return PermissionLevel.Everyone

        val highestRole = member.roles
                .map { it.id }
                .maxBy { permissionsConfig.roleMappings.getOrDefault(it, PermissionLevel.Everyone) }

        return permissionsConfig.roleMappings[highestRole]!!
    }
}