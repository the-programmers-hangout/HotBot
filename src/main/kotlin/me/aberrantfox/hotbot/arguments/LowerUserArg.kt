package me.aberrantfox.hotbot.arguments

import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.extensions.stdlib.trimToID
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType
import me.aberrantfox.kjdautils.internal.command.tryRetrieveSnowflake
import net.dv8tion.jda.core.entities.User

open class LowerUserArg(override val name : String = "Lower Ranked User") : ArgumentType {
    companion object : LowerUserArg()

    lateinit var manager: PermissionService

    override val examples = arrayListOf("@Bob", "197780697866305536", "302134543639511050")
    override val consumptionType = ConsumptionType.Single

    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        val retrieved = tryRetrieveSnowflake(event.jda) { it.retrieveUserById(arg.trimToID()).complete() }

        return when {
            retrieved == null -> ArgumentResult.Error("Couldn't retrieve user: $arg")
            manager.compareUsers(retrieved as User, event.author) >= 0 -> ArgumentResult.Error("You don't have the permission to use this command on the target user.")
            else -> ArgumentResult.Single(retrieved)
        }
    }
}
