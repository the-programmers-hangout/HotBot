package me.aberrantfox.hotbot.commandframework

import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.services.Configuration


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

fun getArgCountError(actual: List<String>, cmd: Command): String? {
    val optionalCount = cmd.expectedArgs.count { it.optional }
    val argCountRange = cmd.parameterCount - optionalCount..cmd.parameterCount

    if (cmd.expectedArgs.any { it.type in multiplePartArgTypes }) {
        if (actual.size < argCountRange.start) {
            return "You didn't enter the minimum number of required arguments: ${cmd.expectedArgs.size - optionalCount}."
        }
    } else {
        if (actual.size !in argCountRange && !cmd.expectedArgs.contains(arg(ArgumentType.Manual))) {
            return "This command requires at least ${argCountRange.start} and a maximum of ${argCountRange.endInclusive} arguments."
        }
    }

    return null
}