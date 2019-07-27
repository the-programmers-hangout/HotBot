package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun producePermissionPrecondition(permissionService: PermissionService) = exit@{ event: CommandEvent ->
    val failMessage = "Did you really think I would let you do that? :thinking:"

    if (permissionService.canUseCommand(event.author, event.commandStruct.commandName))  return@exit Pass
    if (!event.container.has(event.commandStruct.commandName)) return@exit Pass

    return@exit Fail(failMessage)
}
