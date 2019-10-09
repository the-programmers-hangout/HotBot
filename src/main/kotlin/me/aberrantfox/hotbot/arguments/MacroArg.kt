package me.aberrantfox.hotbot.arguments


import me.aberrantfox.hotbot.commands.utility.Macros
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

object MacroInstanceCopy {
    lateinit var macros: Macros
}

open class MacroArg(override val name : String = "Macro") : ArgumentType {
    companion object : MacroArg()

    override val examples = arrayListOf("ask", "wrapcode", "cc", "date")
    override val consumptionType = ConsumptionType.Single
    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        val macro = MacroInstanceCopy.macros.map[arg.toLowerCase()]

        return if (macro != null) {
            ArgumentResult.Single(macro)
        } else {
            ArgumentResult.Error("Couldn't find macro: $arg")
        }
    }
}