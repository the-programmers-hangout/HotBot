package me.aberrantfox.aegeus.commandframework.commands

import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.extensions.idToName
import me.aberrantfox.aegeus.extensions.isRole
import net.dv8tion.jda.core.JDA
import java.io.File

private const val rankConfigPath = "rankconfig.json"

internal data class RankConfiguration(val acceptableRanks: HashSet<String> = HashSet())

object RankContainer {
    private val config: RankConfiguration
    private val file = File(rankConfigPath)
    private val gson = Gson()

    init {
        config = if(!file.exists()) {
            RankConfiguration()
        } else {

            gson.fromJson(file.readText(), RankConfiguration::class.java)
        }
    }

    fun canUse(role: String) = this.config.acceptableRanks.contains(role)

    fun add(role: String) {
        this.config.acceptableRanks.add(role)
        this.save()
    }

    fun remove(role: String) {
        this.config.acceptableRanks.remove(role)
        this.save()
    }

    fun stringList()  =
        if(config.acceptableRanks.isNotEmpty()) {
            config.acceptableRanks.reduce { a, b -> "$a, $b" }
        } else {
            "None."
        }

    private fun save() = file.writeText(gson.toJson(config))
}

@Command(ArgumentType.String, ArgumentType.UserID)
fun grant(event: CommandEvent) = handleGrant(event, true)

@Command(ArgumentType.String, ArgumentType.UserID)
fun ungrant(event: CommandEvent) = handleGrant(event, false)

@Command(ArgumentType.String)
fun whitelistRole(event: CommandEvent) {
    val role = event.args[0] as String

    if( !(event.jda.isRole(role)) ) {
        event.respond("Error, that is not a role, or there are more than one roles by that name.")
        return
    }

    RankContainer.add(role)
    event.respond("The role: $role has been added to the role whitelist, and can now be assigned via the grant command.")
}

@Command(ArgumentType.String)
fun removeWhitelistRole(event: CommandEvent) {
    val role = event.args[0] as String

    if( !(event.jda.isRole(role)) ) {
        event.respond("Error, that is not a role, or there are more than one roles by that name.")
        return
    }

    RankContainer.remove(role)
    event.respond("The role: $role has been un-whitelisted, meaning it can no longer be granted. ")
}

@Command
fun listRoleWhitelist(event: CommandEvent) = event.respond("Currently whitelisted roles: ${RankContainer.stringList()}")

private fun handleGrant(event: CommandEvent, grant: Boolean) {
    if(event.guild == null) return

    val roleName = event.args[0] as String
    val target = event.args[1] as String
    val member = event.guild.getMemberById(target)

    if( !(event.jda.isRole(roleName)) ) {
        event.respond("That is not a known role")
        return
    }

    val role = event.guild.getRolesByName(roleName, true)

    if( !(RankContainer.canUse(roleName)) ) {
        event.respond("That is not a grantable role")
        return
    }

    if(grant) {
        event.guild.controller.addRolesToMember(member, role).queue()
        event.respond("$roleName assigned to ${target.idToName(event.jda)}")
    } else {
        event.guild.controller.removeRolesFromMember(member, role).queue()
        event.respond("$roleName removed from ${target.idToName(event.jda)}")
    }
}