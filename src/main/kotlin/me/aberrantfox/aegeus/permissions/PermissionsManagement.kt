package me.aberrantfox.aegeus.permissions

import me.aberrantfox.aegeus.database.savePermissions
import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.UserID
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role

typealias RoleID = String
typealias CommandName = String

data class PermissionManager(val map: HashMap<RoleID, HashSet<CommandName>> = HashMap(), private val roles: List<Role>,
                             val guild: Guild, val config: Configuration) {

    fun addPermission(roleID: RoleID, name: CommandName) {
        map.keys.map { map[it]!! }
            .filter { it.contains(name) }
            .forEach { it.remove(name) }

        if(map.containsKey(roleID)) {
            map.get(roleID)!!.add(name)
        } else {
            map.put(roleID, hashSetOf(name))
        }

        savePermissions(this)
    }

    fun roleRequired(commandName: CommandName): Role? {
        val containingMap = map.entries.firstOrNull { it.value.contains(commandName) }

        if(containingMap != null) {
            return containingMap.key.toRole(guild)
        } else {
            return null
        }
    }

    fun knowsCommand(commandName: CommandName) = map.containsKey(commandName)

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

        return roles.map { map.get(it) }
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