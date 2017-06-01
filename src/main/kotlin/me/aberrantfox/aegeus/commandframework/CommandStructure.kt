package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.entities.Role
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import java.lang.reflect.Method

enum class Permission {
    GUEST, MODERATOR, ADMIN, OWNER;
}

annotation class Command(vararg val expectedArgs: ArgumentType = arrayOf())

fun produceCommandMap(): HashMap<String, Method> {
    val reflections = Reflections("me.aberrantfox.aegeus.commandframework.commands", MethodAnnotationsScanner())
    val commands = reflections.getMethodsAnnotatedWith(Command::class.java)
    val map: HashMap<String, Method> = HashMap()

    commands.forEach { map[it.name.toLowerCase()] = it }

    return map
}

fun getHighestPermissionLevel(roles: List<Role>, config: Configuration): Permission {
    val roleNames = roles.map { it.name }

    when {
        roleNames.contains(config.rolePermissions.ownerRole) -> return Permission.OWNER
        roleNames.any { config.rolePermissions.adminRoles.contains(it) } -> return Permission.ADMIN
        roleNames.any { config.rolePermissions.moderatorRoles.contains(it) } -> return Permission.MODERATOR
    }

    return Permission.GUEST
}

fun stringToPermission(choice: String): Permission? =
        try {
            enumValueOf<Permission>(choice)
        } catch (e: IllegalArgumentException) {
            null
        }