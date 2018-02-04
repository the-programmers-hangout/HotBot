package me.aberrantfox.hotbot.commandframework

import kotlinx.coroutines.experimental.runBlocking
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandArgument
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

fun convertAndQueue(actual: List<String>, expected: List<CommandArgument>, instance: CommandListener, event: CommandEvent, invokedInGuild: Boolean,
                    command: Command, config: Configuration) {

    val expectedTypes = expected.map { it.type }

    if (expectedTypes.contains(ArgumentType.Manual)) {
        instance.executeEvent(command, event, invokedInGuild)
        return
    }

    val standardParsed = parseStandardArgs(actual, expected, event)

    if (standardParsed == null) {
        event.respond("Incorrect arguments passed to command, try viewing the help documentation via: ${config.serverInformation.prefix}help <commandName>")
        return
    }

    event.args = standardParsed

    if (expectedTypes.contains(ArgumentType.User)) {
        dispatchRequestRequiredEvent(expectedTypes, standardParsed, event, command, instance, invokedInGuild)
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
                try{
                    val parsedUser = event.jda.retrieveUserById((it.first as String).trimToID()).complete()
                    if(parsedUser == null) {
                        event.safeRespond("Error, cannot find user by ID: ${it.first}")
                        return@runBlocking
                    }

                    fullyParsed.add(parsedUser)
                } catch (e: Exception) {
                    event.safeRespond("Error, cannot find user: ${it.first}")
                    return@runBlocking
                }
            } else {
                fullyParsed.add(it.first)
            }
        }
        event.args = fullyParsed
        instance.executeEvent(command, event, invokedInGuild)
    }
}

private fun parseStandardArgs(actual: List<String>, expected: List<CommandArgument>, event: CommandEvent): List<Any>? {
    val returnVals = arrayListOf<Any?>()
    expected.mapTo(returnVals) { null }

    for (index in 0 until actual.size) { // Need `break`, so can't use forEachIndexed
        val actualArg = actual[index]

        val nextMatchingIndex = expected.withIndex().indexOfFirst { arg ->
            val argIndex = arg.index
            val argValue = arg.value

            matchesArgType(actualArg, argValue.type) && returnVals[argIndex] == null
        }
        if (nextMatchingIndex == -1) return null

        returnVals[nextMatchingIndex] = convertArgument(actualArg, expected[nextMatchingIndex].type, index, actual)

        // rest of arguments are the sentence
        if (expected[nextMatchingIndex].type == ArgumentType.Sentence) break
    }

    // TODO: prioritise non-optional args by not just first but filter and sort?
    // TODO: Unit tests
    // TODO: User parsing

    // Fill in optional args or error out if non-optional not filled
    returnVals.forEachIndexed { index, returnVal ->
        if (returnVal == null) {
            val expectedArg = expected[index]

            if (expectedArg.optional) {
                returnVals[index] = expectedArg.defaultValue
            } else return null
        }
    }

    return returnVals.filterNotNull()
}

private fun matchesArgType(arg: String, type: ArgumentType): Boolean {
    return when (type) {
        ArgumentType.Integer -> arg.isInteger()
        ArgumentType.Double -> arg.isDouble()
        ArgumentType.Choice -> arg.isBooleanValue()
        ArgumentType.URL -> arg.containsURl()
        else -> true
    }
}

private fun convertArgument(arg: String, type: ArgumentType, index: Int, actual: List<String>): Any {
    return when (type) {
        ArgumentType.Integer -> arg.toInt()
        ArgumentType.Double -> arg.toDouble()
        ArgumentType.Choice -> arg.toBooleanValue()
        ArgumentType.User -> arg
        ArgumentType.Sentence -> joinArgs(index, actual)
        ArgumentType.Splitter -> splitArg(index, actual)
        else -> arg
    }
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