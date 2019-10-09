package me.aberrantfox.hotbot.commands.permissions


import me.aberrantfox.hotbot.arguments.LowerMemberArg
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.arguments.*
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.exceptions.PermissionException

@CommandSet("ranks")
fun rankCommands(rankContainer: RankContainer) = commands {
    command("grant") {
        description = "Grant a role to a user."
        requiresGuild = true
        expect(RoleArg, LowerMemberArg)
        execute {
            val role = it.args.component1() as Role
            val member = it.args.component2() as Member

            val result = handleGrant(role, member, true, rankContainer)

            it.respond(result)
        }
    }

    command("revoke") {
        description = "Revoke a role from a user"
        requiresGuild = true
        expect(RoleArg, LowerMemberArg)
        execute {
            val role = it.args.component1() as Role
            val member = it.args.component2() as Member

            val result = handleGrant(role, member, false, rankContainer)

            it.respond(result)
        }
    }

    command("makerolegrantable") {
        description = "Allow a role to be granted."
        expect(RoleArg)
        execute {
            val role = it.args.component1() as Role
            val roleName = role.name

            if (rankContainer.canUse(roleName)) {
                it.respond("A role with that name is already grantable.")
                return@execute
            }

            rankContainer.add(roleName)
            it.respond("The role: $roleName has been added to the role whitelist, and can now be assigned via the grant command.")
        }
    }

    command("makeroleungrantable") {
        description = "Disallow granting of this role"
        expect(WordArg("Role Name"))
        execute {
            val roleName = (it.args.component1() as String).toLowerCase()

            if (!rankContainer.canUse(roleName)) {
                it.respond("Error: a role with that name hasn't been made grantable or doesn't exist")
                return@execute
            }

            rankContainer.remove(roleName)
            it.respond("The role: $roleName has been un-whitelisted, meaning it can no longer be granted. ")
        }
    }

    command("listgrantableroles") {
        description = "List grantable roles"
        execute {
            it.respond("Currently whitelisted roles: ${rankContainer.stringList()}")
        }
    }
}

private fun handleGrant(role: Role, member: Member, grant: Boolean, rankContainer: RankContainer): String {
    val guild = member.guild
    val roleName = role.name

    if (!rankContainer.canUse(roleName)) {
        return "That role cannot be granted or revoked."
    }

    try {
        if (grant) {
            guild.addRoleToMember(member, role).queue()
        } else {
            guild.removeRoleFromMember(member, role).queue()
        }
    } catch (ex: PermissionException) {
        return "Bot does not have permission to grant/revoke this role on this member!"
    }

    return "$roleName ${if (grant) "assigned to" else "removed from"} ${member.fullName()}"
}
