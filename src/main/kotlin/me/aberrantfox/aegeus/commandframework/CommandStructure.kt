package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import java.lang.reflect.Method

enum class Permission {
    GUEST, MODERATOR, ADMIN, OWNER;
}

annotation class Command(vararg val expectedArgs: ArgumentType = arrayOf())

annotation class RequiresGuild(val useDefault: Boolean = true)

fun produceCommandMap(): HashMap<String, Method> {
    val reflections = Reflections("me.aberrantfox.aegeus.commandframework.commands", MethodAnnotationsScanner())
    val commands = reflections.getMethodsAnnotatedWith(Command::class.java)
    val map: HashMap<String, Method> = HashMap()

    commands.forEach { map[it.name.toLowerCase()] = it }

    return map
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