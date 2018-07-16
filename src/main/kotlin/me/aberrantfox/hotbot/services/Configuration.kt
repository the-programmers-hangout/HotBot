package me.aberrantfox.hotbot.services

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.kjdautils.internal.logging.ChannelIdHolder
import java.io.File
import java.util.*

open class Configuration(open val serverInformation: ServerInformation = ServerInformation(),
                         val security: Security = Security(),
                         val messageChannels: MessageChannels = MessageChannels(),
                         val apiConfiguration: ApiConfiguration = ApiConfiguration(),
                         val databaseCredentials: DatabaseCredentials = DatabaseCredentials(),
                         val logChannels: ChannelIdHolder = ChannelIdHolder(),
                         val permissionedActions: PermissionedActions = PermissionedActions(),
                         val botInformation: BotInformation = BotInformation())

class ServerInformation(val token: String = "insert-token",
                        val ownerID: String = "insert-id",
                        var prefix: String = "insert-prefix",
                        val guildid: String = "insert-guild-id",
                        val macroDelay: Int = 30,
                        val suggestionPoolLimit: Int = 20,
                        val deleteWelcomeOnLeave: Boolean = true,
                        val maxSelfmuteMinutes: Int = 60)

data class Security(@Transient val ignoredIDs: MutableSet<String> = mutableSetOf(),
                    var lockDownMode: Boolean = false,
                    val infractionActionMap: HashMap<Int, InfractionAction> = hashMapOf(
                            0 to InfractionAction.Warn,
                            1 to InfractionAction.Mute(60),
                            2 to InfractionAction.Mute(24 * 60),
                            3 to InfractionAction.Ban),
                    val mutedRole: String = "Muted",
                    val strikeCeil: Int = 3)

data class MessageChannels(val welcomeChannel: String = "insert-id",
                           val suggestionChannel: String = "insert-id",
                           val profileChannel: String = "insert-channel-id")

data class ApiConfiguration(val cleverbotAPIKey: String = "insert-api-key",
                            val cleverBotApiCallLimit: Int = 10000,
                            val enableCleverBot: Boolean = false)

data class PermissionedActions(var sendInvite: PermissionLevel = PermissionLevel.Moderator,
                               var sendURL: PermissionLevel = PermissionLevel.Moderator,
                               var commandMention: PermissionLevel = PermissionLevel.Moderator,
                               val ignoreLogging: PermissionLevel = PermissionLevel.Moderator,
                               var voiceChannelMuteThreshold: PermissionLevel = PermissionLevel.Moderator)

data class DatabaseCredentials(val username: String = "root",
                               val password: String = "",
                               val hostname: String = "hotbotdb",
                               val database: String = "hotbot")

data class BotInformation(val developmentMode: Boolean = true)


sealed class InfractionAction {
    object Warn : InfractionAction()
    object Kick : InfractionAction()
    object Ban : InfractionAction()
    data class Mute(val duration: Long) : InfractionAction() // in minutes
}


private val configDir = System.getenv("HOTBOT_CONFIG_DIR") ?: "config"
private val configLocation = "config.json"

private val infractionAdapter = RuntimeTypeAdapterFactory
        .of(InfractionAction::class.java)
        .registerSubtype(InfractionAction.Warn::class.java)
        .registerSubtype(InfractionAction.Kick::class.java)
        .registerSubtype(InfractionAction.Ban::class.java)
        .registerSubtype(InfractionAction.Mute::class.java)

private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapterFactory(infractionAdapter)
        .create()

fun configPath(fileName: String) = "${configDir}/${fileName}"

fun loadConfig(): Configuration? {
    val configFile = File(configPath(configLocation))

    if(!configFile.exists()) {
        val jsonData = gson.toJson(Configuration())
        configFile.printWriter().use { it.print(jsonData) }

        return null
    }

    val json = configFile.readLines().stream().reduce("", { a: String, b: String -> a + b })
    val configuration = gson.fromJson<Configuration>(json)

    return configuration
}

fun saveConfig(config: Configuration) {
    val file = File(configPath(configLocation))
    val json = gson.toJson(config)

    file.delete()
    file.printWriter().use { it.print(json) }
}

