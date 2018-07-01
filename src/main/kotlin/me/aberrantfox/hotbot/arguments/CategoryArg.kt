package me.aberrantfox.hotbot.arguments

import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

object CategoryArg : ArgumentType {
    override val name = "Command Category"
    override val examples = arrayListOf("fun", "utility", "suggestions", "embed")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = event.container.commands.values.map { it.category }.contains(arg.toLowerCase())
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(arg)
}