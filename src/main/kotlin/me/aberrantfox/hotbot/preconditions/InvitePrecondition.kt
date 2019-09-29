package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.services.PermissionService
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.extensions.jda.containsInvite
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceInvitePrecondition(config: Configuration, permissionService: PermissionService) = exit@{ event: CommandEvent ->
    val failMessage = "You do not have the required permissions to send an invite."

    if (permissionService.canPerformAction(event.author, config.permissionedActions.sendInvite))  return@exit Pass
    if (!event.message.containsInvite()) return@exit Pass

    return@exit Fail(failMessage)
}