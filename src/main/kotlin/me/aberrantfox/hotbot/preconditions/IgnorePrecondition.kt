package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceIgnorePrecondition(config: Configuration) = exit@{ event: CommandEvent ->
    if (!config.security.ignoredIDs.contains(event.channel.id) && !config.security.ignoredIDs.contains(event.author.id)) return@exit Pass

    return@exit Fail()
}
