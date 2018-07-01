package me.aberrantfox.hotbot.commands

import me.aberrantfox.hotbot.commands.utility.macros
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.extensions.stdlib.trimToID
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType
import me.aberrantfox.kjdautils.internal.command.tryRetrieveSnowflake
import net.dv8tion.jda.core.entities.User

object PermissionLevelArg : ArgumentType {
    override val name = "Permission Level"
    override val examples = arrayListOf("Everyone", "Member", "JrMod", "Moderator", "Administrator", "Owner")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = PermissionLevel.isLevel(arg)
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(PermissionLevel.convertToPermission(arg))
}

object MacroArg : ArgumentType {
    override val name = "Macro"
    override val examples = arrayListOf("ask", "wrapcode", "cc", "date")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = arg.toLowerCase() in macros

    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        val macro = macros[arg.toLowerCase()]

        return if (macro != null) {
            ArgumentResult.Single(macro)
        } else {
            ArgumentResult.Error("Couldn't find macro: $arg")
        }
    }
}

object CategoryArg : ArgumentType {
    override val name = "Command Category"
    override val examples = arrayListOf("fun", "utility", "suggestions", "embed")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = event.container.commands.values.map { it.category }.contains(arg.toLowerCase())
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(arg)
}

object LowerUserArg : ArgumentType {
    lateinit var manager: PermissionManager

    override val name = "Lower Ranked User"
    override val examples = arrayListOf("@Bob", "197780697866305536", "302134543639511050")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = true

    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        val retrieved = tryRetrieveSnowflake(event.jda) { it.retrieveUserById(arg.trimToID()).complete() }

        return when {
            retrieved == null -> ArgumentResult.Error("Couldn't retrieve user: $arg")
            manager.compareUsers(retrieved as User, event.author) >= 0 -> ArgumentResult.Error("You don't have the permission to use this command on the target user.")
            else -> ArgumentResult.Single(retrieved)
        }
    }
}

object HexColourArg : ArgumentType {
    override val name = "Hex Colour"
    override val examples = arrayListOf("#000000", "FFFF00", "#3498db", "db3434")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = arg.length == 6 || arg.length == 7 && arg[0] == '#'

    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        if (arg.length != 7 && arg.length != 6) return ArgumentResult.Error("Invalid colour argument.")

        val hex = if (arg.length == 7) arg.substring(1) else arg
        return try {
            ArgumentResult.Single(hex.toInt(16))
        } catch (e: NumberFormatException) {
            ArgumentResult.Error("Invalid colour argument.")
        }
    }
}