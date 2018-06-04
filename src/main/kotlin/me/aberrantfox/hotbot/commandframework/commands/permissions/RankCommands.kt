package me.aberrantfox.hotbot.commandframework.commands.permissions

import com.google.gson.Gson
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.jda.isRole
import me.aberrantfox.hotbot.services.configPath
import net.dv8tion.jda.core.entities.Role
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


    fun canUse(role: String) = config.acceptableRanks.contains(role.toLowerCase())

    fun add(role: String) {
        config.acceptableRanks.add(role.toLowerCase())
        save()
    }

    fun remove(role: String) {
        config.acceptableRanks.remove(role.toLowerCase())
        save()
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
        expect(ArgumentType.Role, ArgumentType.User)
        execute {
            handleGrant(it, true)
        }
    }

    command("revoke") {
        expect(ArgumentType.Role, ArgumentType.User)
        execute {
            handleGrant(it, false)
        }
    }

    command("makerolegrantable") {
        expect(ArgumentType.Role)
        execute {
            val role = it.args.component1() as Role
            val roleName = role.name

            if (RankContainer.canUse(roleName)) {
                it.respond("A role with that name is already grantable.")
                return@execute
            }

            RankContainer.add(roleName)
            it.safeRespond("The role: $roleName has been added to the role whitelist, and can now be assigned via the grant command.")
        }
    }

    command("makeroleungrantable") {
        expect(ArgumentType.Role)
        execute {
            val role = it.args.component1() as Role
            val roleName = role.name

            if (!(RankContainer.canUse(roleName))) {
                it.respond("Error: a role with that name hasn't been made grantable or doesn't exist")
                return@execute
            }

            RankContainer.remove(roleName)
            it.safeRespond("The role: $roleName has been un-whitelisted, meaning it can no longer be granted. ")
        }
    }

    command("listgrantableroles") {
        execute {
            it.safeRespond("Currently whitelisted roles: ${RankContainer.stringList()}")
        }
    }
}

private fun handleGrant(event: CommandEvent, grant: Boolean) {
    val role = event.args.component1() as Role
    val target = event.args.component2() as User
    val member = event.guild.getMember(target)
    val roleName = role.name

    if (!RankContainer.canUse(roleName)) {
        event.safeRespond("That role cannot be granted or revoked.")
        return
    }

    if (grant) {
        event.guild.controller.addRolesToMember(member, role).queue()
        event.safeRespond("$roleName assigned to ${target.fullName()}")
    } else {
        event.guild.controller.removeRolesFromMember(member, role).queue()
        event.safeRespond("$roleName removed from ${target.fullName()}")
    }
}
