package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.businessobjects.Configuration

enum class ArgumentType {
    INTEGER, DOUBLE, STRING
}

data class CommandStruct(val commandName: String, val commandArgs: List<String> = listOf())

fun convertArguments(actual: List<String>, expected: Array<out ArgumentType>): List<Any>? {
    if (actual.size != expected.size) {
        return null
    }

    val allMatch = actual.zip(expected).all {
        when (it.second) {
            ArgumentType.INTEGER -> it.first.isInteger()
            ArgumentType.DOUBLE -> it.first.isDouble()
            else -> true
        }
    }

    if ( !(allMatch) ) {
        return null
    }

    return actual.zip(expected).map {
        when(it.second) {
            ArgumentType.INTEGER -> it.first.toInt()
            ArgumentType.DOUBLE -> it.first.toDouble()
            else -> it.second
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

fun String.isInteger(): Boolean =
        try {
            this.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }

fun String.isDouble(): Boolean =
        try {
            this.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }