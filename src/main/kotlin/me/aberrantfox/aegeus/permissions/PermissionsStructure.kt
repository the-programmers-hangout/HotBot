package me.aberrantfox.aegeus.permissions

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*

enum class Permission {
    GUEST, MEMBER, JRMOD, MODERATOR, SRMOD, ADMIN, OWNER;
}

fun getHighestPermissionLevel(guild: Guild?, config: Configuration, jda: JDA, userID: String): Permission {
    val roles = getRoles(guild, config, jda, userID).map { it.name }

    return when {
        roles.contains(config.rolePermissions.ownerRole) -> Permission.OWNER
        roles.any { config.rolePermissions.adminRoles.contains(it) } -> Permission.ADMIN
        roles.any { config.rolePermissions.moderatorRoles.contains(it) } -> Permission.MODERATOR
        else -> Permission.GUEST
    }
}

private fun getRoles(guild: Guild?, config: Configuration, jda: JDA, userID: String): List<Role> =
        if (guild != null) {
            guild.getMemberById(userID).roles
        } else {
            jda.getGuildById(config.guildid).getMemberById(userID).roles
        }

fun stringToPermission(choice: String): Permission? =
        try {
            enumValueOf<Permission>(choice)
        } catch (e: IllegalArgumentException) {
            null
        }