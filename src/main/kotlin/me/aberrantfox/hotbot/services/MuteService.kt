package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.database.getAllMutedMembers
import me.aberrantfox.hotbot.utility.scheduleUnmute
import me.aberrantfox.hotbot.utility.timeToDifference
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild

private typealias GuildID = String
private typealias MuteRoleID = String

class MuteService(val jda: JDA,
                  val config: Configuration,
                  val roleName: String = config.security.mutedRole,
                  val log: BotLogger) {
    private val muteMap = hashMapOf<GuildID, MuteRoleID>()

    init {
        jda.guilds.forEach { setupMutedRole(it) }
        handleLTSMutes()
    }

    fun getMutedRole(guild: Guild) = jda.getRoleById(muteMap[guild.id])!!

    private fun setupMutedRole(guild: Guild) {
        val possibleRole = guild.getRolesByName(roleName, true).firstOrNull()
        val mutedRole = possibleRole ?: guild.controller.createRole().setName(roleName).complete()

        muteMap[guild.id] = mutedRole.id

        guild.textChannels
                .filter {
                    it.rolePermissionOverrides.none {
                        it.role.name.toLowerCase() == roleName.toLowerCase()
                    }
                }
                .forEach {
                    it.createPermissionOverride(mutedRole).setDeny(Permission.MESSAGE_WRITE).queue()
                }
    }

    private fun handleLTSMutes() {
        getAllMutedMembers().forEach {
            val difference = timeToDifference(it.unmuteTime)
            val guild = jda.getGuildById(it.guildId)
            val user = guild.getMemberById(it.user)

            if (user != null) {
                scheduleUnmute(guild, user.user, config, log, difference, it)
            }
        }
    }
}