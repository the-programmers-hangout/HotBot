package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.extensions.jda.mentionsSomeone
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceMentionPrecondition(config: Configuration, permissionManager: PermissionManager) = exit@{ event: CommandEvent ->
    val failMessage = "You do not have the required permissions to use a command mention"

    if (permissionManager.canPerformAction(event.author, config.permissionedActions.commandMention))  return@exit Pass
    if (!event.message.mentionsSomeone()) return@exit Pass

    return@exit Fail(failMessage)
}
