package me.aberrantfox.hotbot.permissions

import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class PermissionLevel {
    Everyone, Member, JrMod, Moderator, Administrator, Owner;
    companion object {
        fun isLevel(name: String) = PermissionLevel.values()
                .map { it.name.toLowerCase() }
                .any { it == name.toLowerCase() }

        fun convertToPermission(level: String) = PermissionLevel.values()
                .first { it.name.toLowerCase() == level.toLowerCase() }
    }
}

data class ChannelPermission (var command: PermissionLevel = PermissionLevel.Everyone,
                              var mention: PermissionLevel = PermissionLevel.Everyone)

data class PermissionsConfiguration(val permissions: ConcurrentHashMap<String, PermissionLevel> = ConcurrentHashMap(),
                                    val roleMappings: ConcurrentHashMap<String, PermissionLevel> = ConcurrentHashMap(),
                                    val channelIgnoreLevels: ConcurrentHashMap<String, ChannelPermission> = ConcurrentHashMap())

open class PermissionManager(val jda: JDA, val botConfig: Configuration,
                             permissionsConfigurationLocation: String = "config/permissions.json") {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val permissionsFile = File(permissionsConfigurationLocation)
    private val permissionsConfig: PermissionsConfiguration

    init {
        permissionsConfig = if (permissionsFile.exists()) {
            gson.fromJson(permissionsFile.readText(), PermissionsConfiguration::class.java)
        } else {
            PermissionsConfiguration()
        }

        launch(CommonPool) { save() }
    }

    fun defaultAndPrunePermissions(container: CommandsContainer): Job {
        val commandNames = container.commands.map { it.key.toLowerCase() }

        commandNames.filter { !(permissionsConfig.permissions.containsKey(it)) }
                .forEach { permissionsConfig.permissions[it] = PermissionLevel.Administrator }

        permissionsConfig.permissions.toMap()
                .filterKeys { it !in commandNames }
                .forEach { permissionsConfig.permissions.remove(it.key) }

        return launch(CommonPool) { save() }
    }

    fun removePermissions(command: String) {
        permissionsConfig.permissions.remove(command)
        launch(CommonPool) { save() }
    }

    fun save() = synchronized(permissionsFile) { permissionsFile.writeText(gson.toJson(permissionsConfig)) }

    fun setPermission(command: String, level: PermissionLevel): Job {
        permissionsConfig.permissions[command.toLowerCase()] = level
        return launch(CommonPool) { save() }
    }

    fun roleRequired(name: String) =  permissionsConfig.permissions[name.toLowerCase()] ?: PermissionLevel.Owner

    fun canPerformAction(user: User, actionLevel: PermissionLevel) = getPermissionLevel(user) >= actionLevel

    fun canUseCommand(user: User, command: String) = getPermissionLevel(user) >= permissionsConfig.permissions[command.toLowerCase()] ?: PermissionLevel.Owner

    fun setChannelCommandIgnore(channelId: String, level: PermissionLevel): Job {
        val channelPerm = permissionsConfig.channelIgnoreLevels[channelId] ?: ChannelPermission()
        channelPerm.command = level
        permissionsConfig.channelIgnoreLevels[channelId] = channelPerm
        return launch(CommonPool) { save() }
    }

    fun setChannelMentionIgnore(channelId: String, level: PermissionLevel): Job {
        val channelPerm = permissionsConfig.channelIgnoreLevels[channelId] ?: ChannelPermission()
        channelPerm.mention = level
        permissionsConfig.channelIgnoreLevels[channelId] = channelPerm
        return launch(CommonPool) { save() }
    }

    fun allChannelIgnoreLevels() = permissionsConfig.channelIgnoreLevels.toMap()

    fun canUseCommandInChannel(user: User, channelId: String)
            = getPermissionLevel(user) >= permissionsConfig.channelIgnoreLevels[channelId]?.command ?: PermissionLevel.Everyone

    fun canUseCleverbotInChannel(user: User, channelId: String)
            = getPermissionLevel(user) >= permissionsConfig.channelIgnoreLevels[channelId]?.mention ?: PermissionLevel.Everyone

    fun listAvailableCommands(user: User) = permissionsConfig.permissions
            .filter { it.value <= getPermissionLevel(user) }
            .map { it.key }
            .joinToString()

    fun assignRoleLevel(role: Role, level: PermissionLevel): Job {
        permissionsConfig.roleMappings[role.id] = level
        return launch(CommonPool) { save() }
    }

    fun roleAssignments() = permissionsConfig.roleMappings.entries

    fun compareUsers(userA: User, userB: User) = getPermissionLevel(userA).compareTo(getPermissionLevel(userB))

    private fun getPermissionLevel(user: User): PermissionLevel {
        if (botConfig.serverInformation.ownerID == user.id) return PermissionLevel.Owner

        val member = jda.getGuildById(botConfig.serverInformation.guildid).getMember(user)

        if (member.roles.isEmpty()) return PermissionLevel.Everyone

        if (member.roles.none { permissionsConfig.roleMappings.containsKey(it.id) }) return PermissionLevel.Everyone

        val highestRole = member.roles
                .map { it.id }
                .maxBy { permissionsConfig.roleMappings.getOrDefault(it, PermissionLevel.Everyone) }

        return permissionsConfig.roleMappings[highestRole]!!
    }
}