package me.aberrantfox.hotbot.permissions

import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.io.File

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

data class PermissionsConfiguration(val permissions: HashMap<String, PermissionLevel> = HashMap(),
                                    val roleMappings: HashMap<String, PermissionLevel> = HashMap())

open class PermissionManager(val jda: JDA, val container: CommandsContainer, val botConfig: Configuration,
                             private val permissionsConfigurationLocation: String = "config/permissions.json") {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val permissionsFile = File(permissionsConfigurationLocation)
    private val permissionsConfig: PermissionsConfiguration

    init {
        permissionsConfig = if (permissionsFile.exists()) {
            gson.fromJson(permissionsFile.readText(), PermissionsConfiguration::class.java)
        } else {
            PermissionsConfiguration()
        }

        container.commands
                .map { it.key.toLowerCase() }
                .filter { !(permissionsConfig.permissions.containsKey(it)) }
                .forEach { permissionsConfig.permissions[it] = PermissionLevel.Administrator }

        launch(CommonPool) { save() }
    }

    fun save() = permissionsFile.writeText(gson.toJson(permissionsConfig))

    fun setPermission(command: String, level: PermissionLevel): Job {
        permissionsConfig.permissions[command.toLowerCase()] = level
        return launch(CommonPool) { save() }
    }

    fun roleRequired(name: String) =  permissionsConfig.permissions[name.toLowerCase()] ?: PermissionLevel.Owner

    fun canPerformAction(user: User, actionLevel: PermissionLevel) = getPermissionLevel(user) >= actionLevel

    fun canUseCommand(user: User, command: String) = getPermissionLevel(user) >= permissionsConfig.permissions[command] ?: PermissionLevel.Owner

    fun listAvailableCommands(user: User) = permissionsConfig.permissions
            .filter { it.value <= getPermissionLevel(user) }
            .map { it.key }
            .joinToString()

    fun assignRoleLevel(role: Role, level: PermissionLevel): Job {
        permissionsConfig.roleMappings[role.id] = level
        return launch(CommonPool) { save() }
    }

    fun roleAssignemts() = permissionsConfig.roleMappings.entries

    private fun getPermissionLevel(user: User): PermissionLevel {
        if (botConfig.serverInformation.ownerID == user.id) return PermissionLevel.Owner

        val member = jda.getGuildById(botConfig.serverInformation.guildid).getMember(user)

        if (member.roles.isEmpty()) return PermissionLevel.Everyone

        if (member.roles.none { permissionsConfig.roleMappings.containsKey(it.id) }) return PermissionLevel.Everyone

        val highestRole = member.roles
                .map { it.id }
                .maxBy { permissionsConfig.roleMappings[it]!! }

        return permissionsConfig.roleMappings[highestRole]!!
    }
}