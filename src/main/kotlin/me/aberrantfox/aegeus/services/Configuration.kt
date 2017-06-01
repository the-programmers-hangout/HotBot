package me.aberrantfox.aegeus.services

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.Permission
import java.io.File
import java.lang.reflect.Method

data class Configuration(val token: String = "insert-token",
                         val ownerID: String = "insert-id",
                         val prefix: String = "insert-prefix",
                         val watchTime: Long = 30,
                         val welcomeMessage: String = "Welcome to %servername%, %name%! Be sure to check",
                         val lockDownMode: Boolean = false,
                         val commandPermissionMap: MutableMap<String, Permission> = HashMap(),
                         val rolePermissions: PermissionRoles = PermissionRoles())

class PermissionRoles(val moderatorRoles: Array<String> = arrayOf("moderator"),
                           val adminRoles: Array<String> = arrayOf("admin"),
                           val ownerRole: String = "owner")

val configLocation = "config.json"

fun produceConfigOrFail(commandMap: MutableMap<String, Method>, location: String = configLocation): Configuration? {
    val configFile = File(location)
    val gson = Gson()

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

fun saveConfigurationFile(config: Configuration, location: String = configLocation) {
    val file = File(location)
    val gson = Gson()
    val json = gson.toJson(config)

    file.delete()
    file.printWriter().use { it.print(json) }
}

