package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.commands.utility.canUseMacro
import me.aberrantfox.hotbot.commands.utility.macros
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceMacroPrecondition(config: Configuration) = exit@{ event: CommandEvent ->

    if (event.commandStruct.commandName !in macros)  return@exit Pass
    if (canUseMacro(macros[event.commandStruct.commandName]!!, event.channel, config.serverInformation.macroDelay)) return@exit Pass

    return@exit Fail()
}
