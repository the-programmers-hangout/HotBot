package me.aberrantfox.hotbot.services

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.database.deleteMutedMember
import me.aberrantfox.hotbot.database.getAllMutedMembers
import me.aberrantfox.hotbot.database.insertMutedMember
import me.aberrantfox.hotbot.database.isMemberMuted
import me.aberrantfox.hotbot.utility.*
import me.aberrantfox.kjdautils.api.annotation.Service
import me.aberrantfox.kjdautils.discord.Discord
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.getRoleByName
import me.aberrantfox.kjdautils.extensions.jda.sendPrivateMessage
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.*
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import java.util.*

private typealias GuildID = String
private typealias MuteRoleID = String
private typealias UserId = String

@Service
class MuteService(val discord: Discord,
                  val config: Configuration,
                  val loggingService: LoggingService) {
    private val muteMap = hashMapOf<GuildID, MuteRoleID>()
    private val roleName = config.security.mutedRole
    private val timer = Timer()
    private val unmuteTimerTaskMap = hashMapOf<Pair<GuildID, UserId>, TimerTask>()

    enum class MuteState {
        None,
        TrackedMute,
        UntrackedMute,
    }

    init {
        discord.jda.guilds.forEach { setupMutedRole(it) }
        handleLTSMutes()
    }

    fun getMutedRole(guild: Guild) = discord.jda.getRoleById(muteMap[guild.id]!!)!!

    fun muteMember(member: Member, time: Long, reason: String, moderator: User) {
        require(time > 0) { "time must be non-zero" }
        val guild = member.guild
        val user = member.user

        // If user is already muted, override it
        val key = toKey(member)
        if (key in unmuteTimerTaskMap) {
            unmuteTimerTaskMap[key]?.cancel()
            unmuteTimerTaskMap.remove(key)
            deleteMutedMember(user.id, guild.id)
        }

        val timeString = time.convertToTimeString()
        val timeToUnmute = futureTime(time)
        val record = MuteRecord(timeToUnmute, reason, moderator.id, user.id,
                guild.id)
        val muteEmbed = buildMuteEmbed(user.asMention, timeString, reason)

        guild.addRoleToMember(member, getMutedRole(guild)).queue()
        user.sendPrivateMessage(muteEmbed, loggingService.logInstance)
        insertMutedMember(record)
        scheduleUnmute(member, time)
        notifyMuteAction(guild, user, timeString, reason, config)
    }

    fun cancelMute(member: Member) {
        unmuteTimerTaskMap[toKey(member)]?.cancel()
        unmute(member)
    }

    fun checkMuteState(member: Member) = when {
        isMemberMuted(member.user.id, member.guild.id) -> MuteState.TrackedMute
        member.roles.contains(getMutedRole(member.guild)) -> MuteState.UntrackedMute
        else -> MuteState.None
    }

    private fun toKey(member: Member) = member.guild.id to member.user.id

    private fun setupMutedRole(guild: Guild) {
        val possibleRole = guild.getRoleByName(roleName, true)
        val mutedRole = possibleRole ?: guild.createRole().setName(roleName).complete()

        muteMap[guild.id] = mutedRole.id

        guild.textChannels
                .filter {
                    it.rolePermissionOverrides.none { override ->
                        override.role == mutedRole
                    }
                }
                .forEach {
                    it.createPermissionOverride(mutedRole).setDeny(Permission.MESSAGE_WRITE).queue()
                }
    }

    private fun handleLTSMutes() {
        getAllMutedMembers().forEach {
            val difference = timeToDifference(it.unmuteTime)
            val guild = discord.jda.getGuildById(it.guildId)
            if (guild == null) {
                loggingService.logInstance.error("Couldn't re-add mute to user ${it.user}, because the retrieval of guild ${it.guildId} failed.")
                return@forEach
            }

            val member = guild.getMemberById(it.user)

            if (member != null) {
                scheduleUnmute(member, difference)
            }
        }
    }

    private fun scheduleUnmute(member: Member, time: Long) {
        if (time <= 0) {
            unmute(member)
            return
        }

        val timerTask = object : TimerTask() {
            override fun run() {
                unmute(member)
            }
        }

        unmuteTimerTaskMap[toKey(member)] = timerTask
        timer.schedule(timerTask, time)
    }

    private fun unmute(member: Member) {
        if(checkMuteState(member) == MuteState.None){
            return
        }

        val user = member.user
        val guild = member.guild
        if (user.mutualGuilds.isNotEmpty()) {
            removeMuteRole(member, config, loggingService.logInstance)
        }

        deleteMutedMember(user.id, guild.id)
        unmuteTimerTaskMap.remove(toKey(member))
    }
}