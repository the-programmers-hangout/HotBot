package me.aberrantfox.hotbot.arguments

import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.kjdautils.internal.command.arguments.ChoiceArg

object PermissionLevelArg : ChoiceArg("Permission Level", *PermissionLevel.values())
