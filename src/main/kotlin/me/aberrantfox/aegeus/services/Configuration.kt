package me.aberrantfox.aegeus.services

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.Permission
import me.aberrantfox.aegeus.extensions.MuteRecord
import java.io.File
import java.lang.reflect.Method
import java.util.*

data class Configuration(val token: String = "insert-token",
                         val ownerID: String = "insert-id",
                         var prefix: String = "insert-prefix",
                         val leaveMessage: String = "%name% left... :wave: ... noob",
                         val welcomeChannel: String = "insert-id",
                         val logChannel: String = "insert-id",
                         val suggestionChannel: String = "insert-id",
                         var lockDownMode: Boolean = false,
                         val commandPermissionMap: MutableMap<String, Permission> = HashMap(),
                         val rolePermissions: PermissionRoles = PermissionRoles(),
                         val ignoredIDs: MutableSet<String> = mutableSetOf(),
                         val mutedMembers: ArrayList<MuteRecord> = ArrayList(),
                         var mentionFilterLevel: Permission = Permission.GUEST,
                         val databaseCredentials: DatabaseCredentials = DatabaseCredentials(),
                         val infractionActionMap: HashMap<Int, InfractionAction> = HashMap(),
                         val mutedRole: String = "Muted",
                         val strikeCeil: Int = 3,
                         val invitePermissionLevel: Permission = Permission.MODERATOR,
                         val suggestionPoolLimit: Int = 20,
                         val guildid: String = "244230771232079873",
                         val urlFilterPermissionLevel: Permission = Permission.MODERATOR,
                         val cleverbotAPIKey: String = "insert-api-key",
                         val profileChannel: String = "insert-channel-id",
                         val cleverBotApiCallLimit: Int = 10000)

class PermissionRoles(val moderatorRoles: Array<String> = arrayOf("Moderator"),
                      val adminRoles: Array<String> = arrayOf("Admin"),
                      val ownerRole: String = "Owner")

data class DatabaseCredentials(val username: String = "db-user", val password: String = "db-password")

enum class InfractionAction {
    Warn, Mute, Kick, Ban
}

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

