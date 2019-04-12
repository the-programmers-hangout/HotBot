package me.aberrantfox.hotbot.preconditions

import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.Precondition
import me.aberrantfox.kjdautils.internal.command.Fail
import me.aberrantfox.kjdautils.internal.command.Pass

@Precondition
fun produceChannelPrecondition(permissionManager: PermissionManager) = exit@{ event: CommandEvent ->
    if (permissionManager.canUseCommandInChannel(event.author, event.channel.id)) return@exit Pass

    return@exit Fail()
}
