package me.aberrantfox.hotbot.arguments

import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

open class CategoryArg(override val name : String = "Command Category") : ArgumentType {
    companion object : CategoryArg()

    override val examples = arrayListOf("fun", "utility", "suggestions", "embed")
    override val consumptionType = ConsumptionType.Single
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(arg)
}