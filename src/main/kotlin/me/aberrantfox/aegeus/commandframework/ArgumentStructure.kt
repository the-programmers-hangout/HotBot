package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.commandframework.util.isBooleanValue
import me.aberrantfox.aegeus.commandframework.util.isDouble
import me.aberrantfox.aegeus.commandframework.util.isInteger
import me.aberrantfox.aegeus.commandframework.util.toBooleanValue
import me.aberrantfox.aegeus.services.Configuration

enum class ArgumentType {
    Integer, Double, String, Boolean, Manual
}

data class CommandStruct(val commandName: String, val commandArgs: List<String> = listOf())

fun convertArguments(actual: List<String>, expected: Array<out ArgumentType>): List<Any>? {
    if(expected.contains(ArgumentType.Manual)) {
        return actual
    }
    
    if (actual.size != expected.size) {
        return null
    }

    val allMatch = actual.zip(expected).all {
        when (it.second) {
            ArgumentType.Integer -> it.first.isInteger()
            ArgumentType.Double -> it.first.isDouble()
            ArgumentType.Boolean -> it.first.isBooleanValue()
            else -> true
        }
    }

    if ( !(allMatch) ) {
        return null
    }

    return actual.zip(expected).map {
        when(it.second) {
            ArgumentType.Integer -> it.first.toInt()
            ArgumentType.Double -> it.first.toDouble()
            ArgumentType.Boolean -> it.first.toBooleanValue()
            else -> it.first
        }
    }

}

fun getCommandStruct(message: String, config: Configuration): CommandStruct {
    val trimmedMessage = message.substring(config.prefix.length)

    if (!(message.contains(" "))) {
        return CommandStruct(trimmedMessage.toLowerCase())
    }

    val commandName = trimmedMessage.substring(0, trimmedMessage.indexOf(" ")).toLowerCase()
    val commandArgs = trimmedMessage.substring(trimmedMessage.indexOf(" ") + 1).split(" ")

    return CommandStruct(commandName, commandArgs)
}

