package me.aberrantfox.hotbot.arguments

import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType
import me.aberrantfox.kjdautils.internal.command.arguments.MemberArg
import me.aberrantfox.kjdautils.internal.command.tryRetrieveSnowflake
import net.dv8tion.jda.api.entities.Member

open class LowerMemberArg(override val name : String = "Lower Ranked Member") : ArgumentType {
    companion object : LowerMemberArg()

    lateinit var manager: PermissionService

    override val examples = arrayListOf("@Bob", "197780697866305536", "302134543639511050")
    override val consumptionType = ConsumptionType.Single

    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        val conversionResult = tryRetrieveSnowflake(event.discord.jda) { MemberArg.convert(arg, args, event) }
        val member =
                if (conversionResult is ArgumentResult.Single) {
                    conversionResult.result as Member
                } else {
                    return conversionResult as? ArgumentResult ?: ArgumentResult.Error("Couldn't retrieve member: $arg")
                }

        return when {
            manager.compareUsers(member.user, event.author) >= 0 -> ArgumentResult.Error("You don't have the permission to use this command on the target member.")
            else -> ArgumentResult.Single(member)
        }
    }
}
