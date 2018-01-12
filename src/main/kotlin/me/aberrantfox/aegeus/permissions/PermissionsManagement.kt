package me.aberrantfox.aegeus.permissions

import net.dv8tion.jda.core.entities.Role

typealias RoleID = String
typealias CommandName = String

data class PermissionManager(val map: HashMap<RoleID, HashSet<CommandName>> = HashMap(), private val roles: List<Role>) {

    fun addPermission(roleID: RoleID, name: CommandName) =
        if(map.containsKey(roleID)) {
            map.get(roleID)!!.add(name)
        } else {
            map.put(roleID, hashSetOf(name))
        }

    fun hasPermission(roleID: RoleID, commandName: CommandName) =
        getAllRelevantRoleIds(roleID)
            .filter { map.containsKey(it) }
            .any { map[it]!!.contains(commandName) }

    fun listAvailableCommands(roleID: RoleID) = getAllRelevantRoleIds(roleID)
        .map { map.get(it) }
        .reduceRight { a, b -> a!!.addAll(b!!) ; a }!!

    private fun getAllRelevantRoleIds(roleID: RoleID): List<String> {
        val role = roles.first { it.id == roleID }
        val lowerRoles = ArrayList(roles
            .filter { it.position < role.position }
            .map { it.id }
            .toList())

        lowerRoles.add(roleID)

        return lowerRoles.filter {map.containsKey(it)}
    }
}