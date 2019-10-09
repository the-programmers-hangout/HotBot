package me.aberrantfox.hotbot.services


import me.aberrantfox.kjdautils.api.annotation.Data
import me.aberrantfox.kjdautils.api.dsl.PrefixDeleteMode
import me.aberrantfox.kjdautils.internal.logging.ChannelIdHolder
import java.util.HashMap

@Data("config/config.json")
open class Configuration(open val serverInformation: ServerInformation = ServerInformation(),
                         val security: Security = Security(),
                         val messageChannels: MessageChannels = MessageChannels(),
                         val databaseCredentials: DatabaseCredentials = DatabaseCredentials(),
                         val logChannels: ChannelIdHolder = ChannelIdHolder(),
                         val permissionedActions: PermissionedActions = PermissionedActions())

class ServerInformation(val ownerID: String = "insert-id",
                        var prefix: String = "insert-prefix",
                        val guildid: String = "insert-guild-id",
                        val macroDelay: Int = 30,
                        val deleteWelcomeOnLeave: Boolean = true,
                        val maxSelfmuteMinutes: Int = 60 * 24,
                        val karmaGiveDelay: Int = 1000 * 60 * 60)

data class Security(@Transient val ignoredIDs: MutableSet<String> = mutableSetOf(),
                    var lockDownMode: Boolean = false,
                    val infractionActionMap: HashMap<Int, InfractionAction> = hashMapOf(
                            0 to InfractionAction("Warn"),
                            1 to InfractionAction("Mute", 60 * 60),
                            2 to InfractionAction("Mute", 60 * 60 * 24),
                            3 to InfractionAction("Ban")
                    ),
                    val mutedRole: String = "Muted",
                    val strikeCeil: Int = 3)

data class MessageChannels(val welcomeChannel: String = "insert-id",
                           val suggestionChannel: String = "insert-id",
                           val suggestionArchive: String = "insert-id",
                           val profileChannel: String = "insert-id")

data class PermissionedActions(var sendInvite: PermissionLevel = PermissionLevel.Moderator,
                               var sendURL: PermissionLevel = PermissionLevel.Moderator,
                               var commandMention: PermissionLevel = PermissionLevel.Moderator,
                               val ignoreLogging: PermissionLevel = PermissionLevel.Moderator,
                               var sendUnfilteredFiles: PermissionLevel = PermissionLevel.Moderator)

data class DatabaseCredentials(val username: String = "root",
                               val password: String = "",
                               val hostname: String = "hotbotdb",
                               val database: String = "hotbot")

data class InfractionAction(val punishment: String, val time: Int? = null)