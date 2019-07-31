package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceLockdownPrecondition(config: Configuration) = exit@{ event: CommandEvent ->
    val failMessage = "Only the owner can invoke commands in lockdown mode."

    if (!config.security.lockDownMode) return@exit Pass
    if (event.author.id == config.serverInformation.ownerID) return@exit Pass

    return@exit Fail(failMessage)
}

