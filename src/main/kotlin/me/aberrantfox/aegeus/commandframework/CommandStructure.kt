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

fun getHighestPermissionLevel(guild: Guild?, config: Configuration, jda: JDA): Permission {

    val roles = getRoles(guild, config, jda)

    val roleNames = roles.map { it.name }

    when {
        roleNames.contains(config.rolePermissions.ownerRole) -> return Permission.OWNER
        roleNames.any { config.rolePermissions.adminRoles.contains(it) } -> return Permission.ADMIN
        roleNames.any { config.rolePermissions.moderatorRoles.contains(it) } -> return Permission.MODERATOR
    }

    return Permission.GUEST
}

private fun getRoles(guild: Guild?, config: Configuration, jda: JDA): List<Role> =
        if (guild != null) {
            guild.roles
        } else {
            jda.getGuildById(config.guildid).roles
        }

fun stringToPermission(choice: String): Permission? =
        try {
            enumValueOf<Permission>(choice)
        } catch (e: IllegalArgumentException) {
            null
        }