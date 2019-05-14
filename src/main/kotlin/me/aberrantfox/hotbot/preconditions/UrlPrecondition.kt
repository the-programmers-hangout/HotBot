package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.extensions.jda.containsURL
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceUrlPrecondition(config: Configuration, permissionService: PermissionService) = exit@{ event: CommandEvent ->
    val failMessage = "You do not have the required permissions to send URLs"

    if (event.commandStruct.commandName in listOf("uploadtext", "suggest")) return@exit Pass
    if (!event.message.containsURL()) return@exit Pass
    if (permissionService.canPerformAction(event.author, config.permissionedActions.sendURL)) return@exit Pass

    return@exit Fail(failMessage)
}
