package me.aberrantfox.hotbot.commandframework.commands

import com.google.gson.Gson
import me.aberrantfox.hotbot.commandframework.ArgumentType
import me.aberrantfox.hotbot.commandframework.CommandSet
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.jda.isRole
import me.aberrantfox.hotbot.services.configPath
import net.dv8tion.jda.core.entities.User
import java.io.File

private val rankConfigPath = configPath("rankconfig.json")

internal data class RankConfiguration(val acceptableRanks: HashSet<String> = HashSet())

object RankContainer {
    private val config: RankConfiguration
    private val file = File(rankConfigPath)
    private val gson = Gson()

    init {
        config = if (!file.exists()) {
            RankConfiguration()
        } else {

            gson.fromJson(file.readText(), RankConfiguration::class.java)
        }
    }


    fun canUse(role: String) = this.config.acceptableRanks.contains(role.toLowerCase())

    fun add(role: String) {
        this.config.acceptableRanks.add(role.toLowerCase())
        this.save()
    }

    fun remove(role: String) {
        this.config.acceptableRanks.remove(role.toLowerCase())
        this.save()
    }

    fun stringList() =
        if (config.acceptableRanks.isNotEmpty()) {
            config.acceptableRanks.reduce { a, b -> "$a, $b" }
        } else {
            "None."
        }

    private fun save() = file.writeText(gson.toJson(config))
}

@CommandSet
fun rankCommands() = commands {
    command("grant") {
        expect(ArgumentType.Word, ArgumentType.User)
        execute {
            handleGrant(it, true)
        }
    }

    command("revoke") {
        expect(ArgumentType.Word, ArgumentType.User)
        execute {
            handleGrant(it, false)
        }
    }

    command("makerolegrantable") {
        expect(ArgumentType.Word)
        execute {
            val role = it.args[0] as String

            if (!(it.jda.isRole(role))) {
                it.respond("Error, that is not a role, or there are more than one roles by that name.")
                return@execute
            }

            if (RankContainer.canUse(role)) {
                it.respond("A role with that name is already grantable.")
                return@execute
            }

            RankContainer.add(role)
            it.respond("The role: $role has been added to the role whitelist, and can now be assigned via the grant command.")
        }
    }

    command("makeroleungrantable") {
        expect(ArgumentType.Word)
        execute {
            val role = it.args[0] as String

            if (!(RankContainer.canUse(role))) {
                it.respond("Error: a role with that name hasn't been made grantable or doesn't exist")
                return@execute
            }

            RankContainer.remove(role)
            it.respond("The role: $role has been un-whitelisted, meaning it can no longer be granted. ")
        }
    }

    command("listgrantableroles") {
        execute {
            it.respond("Currently whitelisted roles: ${RankContainer.stringList()}")
        }
    }
}

private fun handleGrant(event: CommandEvent, grant: Boolean) {
    val roleName = event.args[0] as String
    val target = event.args[1] as User
    val member = event.guild.getMember(target)

    if (!(event.jda.isRole(roleName))) {
        event.respond("That is not a known role")
        return
    }

    val role = event.guild.getRolesByName(roleName, true)

    if (!(RankContainer.canUse(roleName))) {
        event.respond("That is not a grantable role")
        return
    }

    if (grant) {
        event.guild.controller.addRolesToMember(member, role).queue()
        event.respond("$roleName assigned to ${target.fullName()}")
    } else {
        event.guild.controller.removeRolesFromMember(member, role).queue()
        event.respond("$roleName removed from ${target.fullName()}")
    }
}
