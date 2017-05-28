package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.businessobjects.Configuration

enum class ArgumentType {
    INTEGER, DOUBLE, STRING, BOOLEAN, MANUAL
}

data class CommandStruct(val commandName: String, val commandArgs: List<String> = listOf())

fun convertArguments(actual: List<String>, expected: Array<out ArgumentType>): List<Any>? {
    if(expected.contains(ArgumentType.MANUAL)) {
        return actual
    }
    
    if (actual.size != expected.size) {
        return null
    }

    val allMatch = actual.zip(expected).all {
        when (it.second) {
            ArgumentType.INTEGER -> it.first.isInteger()
            ArgumentType.DOUBLE -> it.first.isDouble()
            ArgumentType.BOOLEAN -> it.first.isBooleanValue()
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
            ArgumentType.BOOLEAN -> it.first.toBooleanValue()
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


fun String.isBooleanValue(): Boolean =
        when(this.toLowerCase()) {
            "true" -> true
            "false" -> true
            "t" -> true
            "f" -> true
            else -> false
        }

fun String.toBooleanValue(): Boolean =
        when(this.toLowerCase()) {
            "true" -> true
            "t" -> true
            else -> false
        }