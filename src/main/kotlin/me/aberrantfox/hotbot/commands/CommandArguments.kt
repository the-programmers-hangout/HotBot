package me.aberrantfox.hotbot.commands

import me.aberrantfox.hotbot.commands.utility.macros
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.services.HelpConf
import me.aberrantfox.kjdautils.api.dsl.CommandArgument
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

object PermissionLevelArg : ArgumentType {
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = PermissionLevel.isLevel(arg)
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(PermissionLevel.convertToPermission(arg))
}

object MacroArg : ArgumentType {
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent)
            = macros.any { it.name.toLowerCase() == arg.toLowerCase() }

    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        val macro = macros.firstOrNull { it.name.toLowerCase() == arg.toLowerCase() }

        return if (macro != null) {
            ArgumentResult.Single(macro)
        } else {
            ArgumentResult.Error("Couldn't find macro: $arg")
        }
    }
}

object CategoryArg : ArgumentType {
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = HelpConf.listCategories().contains(arg.toLowerCase())
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(arg)
}