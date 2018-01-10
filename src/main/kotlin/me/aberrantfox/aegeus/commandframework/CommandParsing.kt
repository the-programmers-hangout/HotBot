package me.aberrantfox.aegeus.commandframework

import me.aberrantfox.aegeus.dsls.command.CommandsContainer
import me.aberrantfox.aegeus.extensions.*
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner

enum class ArgumentType {
    Integer, Double, Word, Choice, Manual, Sentence, UserID, Splitter, URL
}

const val seperatorCharacter = "|"

annotation class CommandSet

data class CommandStruct(val commandName: String, val commandArgs: List<String> = listOf())


fun produceContainer(): CommandsContainer {
    val pack = "me.aberrantfox.aegeus.commandframework.commands"
    val cmds = Reflections(pack, MethodAnnotationsScanner()).getMethodsAnnotatedWith(CommandSet::class.java)

    return cmds.map { it.invoke(null) }
        .map { it as CommandsContainer }
        .reduce { a, b -> a.join(b) }
}

fun convertArguments(actual: List<String>, expected: List<ArgumentType>, jda: JDA): List<Any>? {
    if(expected.contains(ArgumentType.Manual)) return actual

    if (actual.size != expected.size) {
        if((!expected.contains(ArgumentType.Sentence) && !expected.contains(ArgumentType.Splitter))) {
            return null
        }
    }

    val allMatch = actual.zip(expected).all {
        when (it.second) {
            ArgumentType.Integer -> it.first.isInteger()
            ArgumentType.Double -> it.first.isDouble()
            ArgumentType.Choice -> it.first.isBooleanValue()
            ArgumentType.UserID -> it.first.isUserID(jda)
            ArgumentType.URL -> it.first.containsURl()
            else -> true
        }
    }

    if ( !(allMatch) ) return null

    val returnVals = mutableListOf<Any>()

    actual.zip(expected).forEachIndexed { index, pair ->
        when(pair.second) {
            ArgumentType.Integer -> returnVals.add(pair.first.toInt())
            ArgumentType.Double -> returnVals.add(pair.first.toDouble())
            ArgumentType.Choice -> returnVals.add(pair.first.toBooleanValue())
            ArgumentType.UserID -> returnVals.add(pair.first)
            ArgumentType.Sentence -> returnVals.add(joinArgs(index, actual))
            ArgumentType.Splitter -> returnVals.add(splitArg(index, actual))
            else -> returnVals.add(pair.first)
        }
    }

    return returnVals
}

fun getCommandStruct(message: String, config: Configuration): CommandStruct {
    var trimmedMessage = message.substring(config.prefix.length)

    if(trimmedMessage.startsWith(config.prefix)) trimmedMessage = trimmedMessage.substring(config.prefix.length)

    if (!(message.contains(" "))) {
        return CommandStruct(trimmedMessage.toLowerCase())
    }

    val commandName = trimmedMessage.substring(0, trimmedMessage.indexOf(" ")).toLowerCase()
    val commandArgs = trimmedMessage.substring(trimmedMessage.indexOf(" ") + 1).split(" ")

    return CommandStruct(commandName, commandArgs)
}

private fun joinArgs(start: Int, actual: List<String>) = actual.subList(start, actual.size).reduce { a, b -> "$a $b" }

private fun splitArg(start: Int, actual: List<String>): List<String> {
    val joined = joinArgs(start, actual)

    if( !(joined.contains(seperatorCharacter)) ) return listOf(joined)

    return joined.split(seperatorCharacter).toList()
}