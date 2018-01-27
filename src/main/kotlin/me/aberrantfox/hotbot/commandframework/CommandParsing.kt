package me.aberrantfox.hotbot.commandframework

import kotlinx.coroutines.experimental.runBlocking
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.extensions.*
import me.aberrantfox.hotbot.listeners.CommandListener
import me.aberrantfox.hotbot.services.Configuration
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner

enum class ArgumentType {
    Integer, Double, Word, Choice, Manual, Sentence, User, Splitter, URL
}

const val seperatorCharacter = "|"

annotation class CommandSet

data class CommandStruct(val commandName: String, val commandArgs: List<String> = listOf())


fun produceContainer(): CommandsContainer {
    val pack = "me.aberrantfox.hotbot.commandframework.commands"
    val cmds = Reflections(pack, MethodAnnotationsScanner()).getMethodsAnnotatedWith(CommandSet::class.java)

    val container = cmds.map { it.invoke(null) }
        .map { it as CommandsContainer }
        .reduce { a, b -> a.join(b) }

    val lowMap = HashMap<String, Command>()

    container.commands.keys.forEach {
        lowMap.put(it.toLowerCase(), container.commands[it]!!)
    }

    container.commands = lowMap

    return container
}

fun convertAndQueue(actual: List<String>, expected: List<ArgumentType>, instance: CommandListener, event: CommandEvent, invokedInGuild: Boolean,
                    command: Command, config: Configuration) {
    if (expected.contains(ArgumentType.Manual)) {
        instance.executeEvent(command, event, invokedInGuild)
        return
    }

    if (actual.size != expected.size) {
        if ((!expected.contains(ArgumentType.Sentence) && !expected.contains(ArgumentType.Splitter))) {
            event.respond("You do not have the minimum number of arguments. Required:${expected.size}, given: ${actual.size}")
            return
        }
    }

    val standardParsed = parseStandardArgs(actual, expected, event)

    if (standardParsed == null) {
        event.respond("Incorrect arguments passed to command, try viewing the help documentation via: ${config.serverInformation.prefix}help <commandName>")
        return
    }

    event.args = standardParsed

    if (expected.contains(ArgumentType.User)) {
        dispatchRequestRequiredEvent(expected, standardParsed, event, command, instance, invokedInGuild)
    } else {
        instance.executeEvent(command, event, invokedInGuild)
    }
}

private fun dispatchRequestRequiredEvent(expected: List<ArgumentType>, standard: List<Any>, event: CommandEvent, command: Command,
                                         instance: CommandListener, invokedInGuild: Boolean) {
    val zip = standard.zip(expected)

    runBlocking {
        val fullyParsed = ArrayList<Any>()

        zip.forEach {
            if (it.second == ArgumentType.User) {
                val parsedUser = event.jda.retrieveUserById((it.first as String).trimToID()).complete()

                if(parsedUser == null) {
                    event.respond("Error, cannot find user by ID: ${it.first}")
                    return@runBlocking
                }

                fullyParsed.add(parsedUser)
            } else {
                fullyParsed.add(it.first)
            }
        }
        event.args = fullyParsed
        instance.executeEvent(command, event, invokedInGuild)
    }
}

private fun parseStandardArgs(actual: List<String>, expected: List<ArgumentType>, event: CommandEvent): List<Any>? {
    val allMatch = actual.zip(expected).all {
        when (it.second) {
            ArgumentType.Integer -> it.first.isInteger()
            ArgumentType.Double -> it.first.isDouble()
            ArgumentType.Choice -> it.first.isBooleanValue()
            ArgumentType.URL -> it.first.containsURl()
            else -> true
        }
    }

    if (!(allMatch)) {
        return null
    }

    val returnVals = mutableListOf<Any>()

    actual.zip(expected).forEachIndexed { index, pair ->
        when (pair.second) {
            ArgumentType.Integer -> returnVals.add(pair.first.toInt())
            ArgumentType.Double -> returnVals.add(pair.first.toDouble())
            ArgumentType.Choice -> returnVals.add(pair.first.toBooleanValue())
            ArgumentType.User -> returnVals.add(pair.first)
            ArgumentType.Sentence -> returnVals.add(joinArgs(index, actual))
            ArgumentType.Splitter -> returnVals.add(splitArg(index, actual))
            else -> returnVals.add(pair.first)
        }
    }

    return returnVals
}

fun getCommandStruct(message: String, config: Configuration): CommandStruct {
    var trimmedMessage = message.substring(config.serverInformation.prefix.length)

    if (trimmedMessage.startsWith(config.serverInformation.prefix)) trimmedMessage = trimmedMessage.substring(config.serverInformation.prefix.length)

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

    if (!(joined.contains(seperatorCharacter))) return listOf(joined)

    return joined.split(seperatorCharacter).toList()
}