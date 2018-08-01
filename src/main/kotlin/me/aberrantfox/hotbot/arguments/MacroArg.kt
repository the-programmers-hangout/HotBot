package me.aberrantfox.hotbot.arguments

import me.aberrantfox.hotbot.commands.utility.macros
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

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