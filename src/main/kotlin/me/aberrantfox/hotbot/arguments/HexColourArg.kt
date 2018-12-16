package me.aberrantfox.hotbot.arguments

import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

open class HexColourArg(override val name : String = "Hex Colour") : ArgumentType {
    companion object : HexColourArg()

    override val examples = arrayListOf("#000000", "FFFF00", "#3498db", "db3434")
    override val consumptionType = ConsumptionType.Single
    override fun convert(arg: String, args: List<String>, event: CommandEvent): ArgumentResult {
        if (arg.length != 7 && arg.length != 6) return ArgumentResult.Error("Invalid colour argument.")

        val hex = if (arg.length == 7) arg.substring(1) else arg
        return try {
            val int = hex.toInt(16)
            when {
                int >= 0 -> ArgumentResult.Single(int)
                else -> ArgumentResult.Error("Last I checked, hex colors are positive")
            }
        } catch (e: NumberFormatException) {
            ArgumentResult.Error("Invalid colour argument.")
        }
    }
}