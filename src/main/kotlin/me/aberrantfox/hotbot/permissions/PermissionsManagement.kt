package me.aberrantfox.hotbot.permissions

import me.aberrantfox.hotbot.database.setPermission
import me.aberrantfox.hotbot.extensions.jda.getHighestRole
import me.aberrantfox.hotbot.extensions.jda.isEqualOrHigherThan
import me.aberrantfox.hotbot.extensions.jda.toMember
import me.aberrantfox.hotbot.extensions.stdlib.toRole
import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User

typealias RoleID = String
typealias CommandName = String

data class PermissionManager(val map: HashMap<RoleID, HashSet<CommandName>> = HashMap(), val jda: JDA, val config: Configuration) {

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

        setPermission(lower, roleID)
    }

    fun roleRequired(commandName: CommandName): Role? {
        val containingMap = map.entries.firstOrNull { it.value.contains(commandName.toLowerCase()) }
        return containingMap?.key?.toRole(jda.getGuildById(config.serverInformation.guildid))
    }

    fun canPerformAction(user: User, actionRoleID: RoleID): Boolean {
        if(user.id == config.serverInformation.ownerID) return true

        val highestRole = user.toMember(jda.getGuildById(config.serverInformation.guildid)).getHighestRole()
        val actionRole = actionRoleID.toRole(jda.getGuildById(config.serverInformation.guildid))

        return highestRole?.isEqualOrHigherThan(actionRole) ?: false
    }

    fun canUseCommand(user: User, commandName: CommandName): Boolean {
        if(user.id == config.serverInformation.ownerID) return true

        val highestRole = user.toMember(jda.getGuildById(config.serverInformation.guildid)).getHighestRole()
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

        val lowerRoles = getLowerRoleIds(roleID)

        lowerRoles.add(roleID)

        return lowerRoles.filter { map.containsKey(it) }
    }

    fun getLowerRoleIds(roleID: RoleID): ArrayList<String> {
        val guild = jda.getGuildById(config.serverInformation.guildid)
        val role = guild.roles.first { it.id == roleID }

        return ArrayList(guild.roles
                .filter { it.position < role.position }
                .map { it.id }
                .toList())
    }
}