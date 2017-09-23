package me.aberrantfox.aegeus.commandframework.util

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User


fun User.fullName() = "$name#$discriminator"

fun Member.fullName() = "${this.user.name}#${this.user.discriminator}"

fun Member.descriptor() = "${fullName()} :: ID :: ${this.user.id}"

fun User.descriptor() = "${fullName()} :: ID :: $id"
