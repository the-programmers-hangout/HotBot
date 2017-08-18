package me.aberrantfox.aegeus.services

import com.github.salomonbrys.kotson.fromJson
import com.google.common.collect.ConcurrentHashMultiset
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.Permission
import java.io.File
import java.lang.reflect.Method
import java.util.*

data class Configuration(val token: String = "insert-token",
                         val ownerID: String = "insert-id",
                         var prefix: String = "insert-prefix",
                         val welcomeMessage: String = "Welcome to %servername%, %name%! Be sure to check",
                         val leaveMessage: String = "%name% left... :wave: ... noob",
                         val welcomeChannel: String = "insert-id",
                         val leaveChannel: String = "insert-id",
                         var lockDownMode: Boolean = false,
                         val commandPermissionMap: MutableMap<String, Permission> = HashMap(),
                         val rolePermissions: PermissionRoles = PermissionRoles(),
                         val ignoredIDs: MutableSet<String> = mutableSetOf(),
                         val mutedMembers: Vector<String> = Vector(),
                         var mentionFilterLevel: Permission = Permission.GUEST,
                         val databaseCredentials: DatabaseCredentials = DatabaseCredentials())

class PermissionRoles(val moderatorRoles: Array<String> = arrayOf("moderator"),
                      val adminRoles: Array<String> = arrayOf("admin"),
                      val ownerRole: String = "owner")

data class DatabaseCredentials(val username: String = "db-user", val password: String = "db-password")

private val configLocation = "config.json"
private val gson = Gson()

fun loadConfig(commandMap: MutableMap<String, Method>): Configuration? {
    val configFile = File(configLocation)

    if(!configFile.exists()) {
        val jsonData = gson.toJson(Configuration())
        configFile.printWriter().use { it.print(jsonData) }

        return null
    }

    val json = configFile.readLines().stream().reduce("", { a: String, b: String -> a + b })
    val configuration = gson.fromJson<Configuration>(json)

    commandMap.keys.filter { !configuration.commandPermissionMap.containsKey(it) }
            .forEach { configuration.commandPermissionMap[it] = Permission.ADMIN }

    return configuration
}

fun saveConfig(config: Configuration) {
    val file = File(configLocation)
    val json = gson.toJson(config)

    file.delete()
    file.printWriter().use { it.print(json) }
}

