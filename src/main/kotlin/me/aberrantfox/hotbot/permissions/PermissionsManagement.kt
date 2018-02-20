package me.aberrantfox.hotbot.permissions

import me.aberrantfox.hotbot.database.savePermissions
import me.aberrantfox.hotbot.extensions.*
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.UserID
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role

typealias RoleID = String
typealias CommandName = String

data class PermissionManager(val map: HashMap<RoleID, HashSet<CommandName>> = HashMap(), private val roles: List<Role>,
                             val guild: Guild, val config: Configuration) {

    fun addPermission(roleID: RoleID, name: CommandName) {
        val lower = name.toLowerCase()
        map.keys.map { map[it]!! }
            .filter { it.contains(lower) }
            .forEach { it.remove(lower) }

        if(map.containsKey(roleID)) {
            map[roleID]!!.add(lower)
        } else {
            map[roleID] = hashSetOf(lower)
        }

        savePermissions(this)
    }

    fun roleRequired(commandName: CommandName): Role? {
        val containingMap = map.entries.firstOrNull { it.value.contains(commandName.toLowerCase()) }
        return containingMap?.key?.toRole(guild)
    }

    fun knowsCommand(commandName: CommandName) = map.containsKey(commandName.toLowerCase())

    fun canPerformAction(userId: UserID, actionRoleID: RoleID): Boolean {
        if(userId == config.serverInformation.ownerID) return true

        val highestRole = userId.idToUser(guild.jda).toMember(guild).getHighestRole()
        val actionRole = actionRoleID.toRole(guild)

        return highestRole?.isEqualOrHigherThan(actionRole) ?: false
    }

    fun canUseCommand(userId: UserID, commandName: CommandName): Boolean {
        if(userId == config.serverInformation.ownerID) return true

        val highestRole = userId.idToUser(guild.jda).toMember(guild).getHighestRole()
        val roles = getAllRelevantRoleIds(highestRole?.id)

        return roles.map { map[it] }
            .any { it!!.contains(commandName) }
    }

    fun listAvailableCommands(roleID: RoleID?): String {
        val roles = getAllRelevantRoleIds(roleID)

        if(roles.isEmpty()) return "None"

        return roles.map { map[it] }
            .reduceRight { a, b -> a!!.addAll(b!!) ; a }!!
            .joinToString(", ") { a -> a }
    }

    private fun getAllRelevantRoleIds(roleID: RoleID?): List<String> {
        if(roleID == null) return ArrayList()

        val role = roles.first { it.id == roleID }
        val lowerRoles = ArrayList(roles
            .filter { it.position < role.position }
            .map { it.id }
            .toList())

        lowerRoles.add(roleID)

        return lowerRoles.filter { map.containsKey(it) }
    }
}