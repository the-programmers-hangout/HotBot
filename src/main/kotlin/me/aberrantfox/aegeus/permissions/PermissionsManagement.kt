package me.aberrantfox.aegeus.permissions

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role


class RoleContainer(private val roleMap: HashMap<String, Pair<Role, Boolean>> = HashMap(),
                    private val commandMap: HashMap<String, String> = HashMap()) {
    fun addRole(id: String, role: Role) = roleMap.put(id, Pair(role, false))

    fun removeRole(id: String) = roleMap.remove(id)

    fun getRole(id: String) = roleMap[id]

    fun listRoles() = roleMap.keys
}