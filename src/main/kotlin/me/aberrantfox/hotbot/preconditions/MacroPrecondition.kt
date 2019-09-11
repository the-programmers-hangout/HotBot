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
    val macro = macros[event.commandStruct.commandName] ?: return@exit Pass

    if (canUseMacro(macro, event.channel, config.serverInformation.macroDelay)) return@exit Pass

    return@exit Fail()
}
