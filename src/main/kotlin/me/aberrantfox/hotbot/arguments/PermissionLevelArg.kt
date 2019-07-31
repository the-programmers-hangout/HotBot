package me.aberrantfox.hotbot.arguments

import me.aberrantfox.hotbot.services.PermissionLevel
import me.aberrantfox.kjdautils.internal.command.arguments.ChoiceArg

object PermissionLevelArg : ChoiceArg("Permission Level", *PermissionLevel.values())
