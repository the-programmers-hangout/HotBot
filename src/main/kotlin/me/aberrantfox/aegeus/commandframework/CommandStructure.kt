package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.businessobjects.Configuration
import net.dv8tion.jda.core.entities.Role
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import java.lang.reflect.Method

enum class Permission {
    GUEST, MODERATOR, ADMIN, OWNER;

    fun matchStringToItem(choice: String): Permission? =
            when (choice.toLowerCase()) {
                "guest" -> GUEST
                "moderator" -> MODERATOR
                "admin" -> ADMIN
                "owner" -> OWNER
                else -> null
            }
}

annotation class Command(vararg val expectedArgs: ArgumentType = arrayOf())

fun produceCommandMap(): HashMap<String, Method> {
    val reflections = Reflections("me.aberrantfox.aegeus.commandframework.commands", MethodAnnotationsScanner())
    val commands = reflections.getMethodsAnnotatedWith(Command::class.java)
    val map: HashMap<String, Method> = HashMap()


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
