package me.aberrantfox.hotbot.arguments

import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.internal.command.ArgumentResult
import me.aberrantfox.kjdautils.internal.command.ArgumentType
import me.aberrantfox.kjdautils.internal.command.ConsumptionType

object PermissionLevelArg : ArgumentType {
    override val name = "Permission Level"
    override val examples = arrayListOf("Everyone", "Member", "JrMod", "Moderator", "Administrator", "Owner")
    override val consumptionType = ConsumptionType.Single
    override fun isValid(arg: String, event: CommandEvent) = PermissionLevel.isLevel(arg)
    override fun convert(arg: String, args: List<String>, event: CommandEvent) = ArgumentResult.Single(PermissionLevel.convertToPermission(arg))
}