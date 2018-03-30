package me.aberrantfox.hotbot.commandframework.parsing

import me.aberrantfox.hotbot.dsls.command.CommandArgument
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.extensions.stdlib.*
import net.dv8tion.jda.core.JDA

const val separatorCharacter = "|"

val consumingArgTypes = listOf(ArgumentType.Sentence, ArgumentType.Splitter)
val multiplePartArgTypes = listOf(ArgumentType.Sentence, ArgumentType.Splitter, ArgumentType.TimeString)

enum class ArgumentType {
    Integer, Double, Word, Choice, Manual, Sentence, User, Splitter, URL
}

data class ConversionResult(val results: List<Any?>? = null,
                            val error: String? = null,
                            val consumed: List<Any?>? = null) {

    fun then(function: (List<Any?>) -> Any): ConversionResult =
            if (hasError()) {
                this
            } else {
                val nextResult = function.invoke(results!!)

                when (nextResult) {
                    is ConversionResult -> nextResult
                    is List<*> -> ConversionResult(nextResult)
                    is Unit -> this
                    else -> throw IllegalArgumentException("Function must return List, Unit or ConversionResult.")
                }
            }

    fun thenIf(condition: Boolean, function: (List<Any?>) -> Any) =
            if (condition) {
                then(function)
            } else {
                this
            }

    fun hasError() = error != null || results == null
}

fun convertArguments(actual: List<String>, expected: List<CommandArgument>, event: CommandEvent): ConversionResult {

    val expectedTypes = expected.map { it.type }

    if (expectedTypes.contains(ArgumentType.Manual)) {
        return ConversionResult(actual)
    }

    return convertMainArgs(actual, expected)
            .thenIf(expectedTypes.contains(ArgumentType.User)) {
                retrieveUserArguments(expected, it, event.jda)
            }.then {
                convertOptionalArgs(it, expected, event)
            }
}

fun retrieveUserArguments(expected: List<CommandArgument>, filledArgs: List<Any?>, jda: JDA): ConversionResult {
    val zip = filledArgs.zip(expected)

    val usersConverted =
            zip.map {
                val (arg, expectedArg) = it

                if (expectedArg.type != ArgumentType.User || arg == null) return@map arg

                val parsedUser =
                        try {
                            jda.retrieveUserById((arg as String).trimToID()).complete()
                        } catch (e: RuntimeException) {
                            null
                        }

                if (parsedUser == null) return ConversionResult(null, "Couldn't retrieve user: $arg")

                return@map parsedUser
            }

    return ConversionResult(usersConverted)
}

/**
 * Converts a list of strings to the matching expected argument types
 *
 * If the argument isn't converted, it is left as null;
 * e.g. if it's optional and the actual args have already been fully converted
 *
 * Returns null if the actual args cannot fit into the expected ones;
 * e.g. if there is still an arg to convert but no matching place to put it.
 *
 * @return A list of arguments converted to their expected type.
 *
 * null if the actual args do not fit into the expected ones.
 *
 */
fun convertMainArgs(actual: List<String>,
                    expected: List<CommandArgument>): List<Any?>? {

    val converted = arrayOfNulls<Any?>(expected.size)

    for (index in 0 until actual.size) {
        val actualArg = actual[index]

        val nextMatchingIndex = expected.withIndex().indexOfFirst {
            matchesArgType(actualArg, it.value.type) && converted[it.index] == null
        }
        if (nextMatchingIndex == -1) return null

        converted[nextMatchingIndex] = convertArg(actualArg, expected[nextMatchingIndex].type, index, actual)

        if (expected[nextMatchingIndex].type in multiplePartArgTypes) break
    }

    if (converted.filterIndexed { i, arg -> arg == null && !expected[i].optional }.isNotEmpty())
        return null

    return converted.toList()
}


/**
 * Converts any null arguments in a list of converted arguments to their default value
 *
 * If the default is a function, it is invoked, passing in the CommandEvent.
 * If the value isn't null, it is left unmodified.
 *
 * @return A list of arguments with the optionals filled
 *
 */
@Suppress("UNCHECKED_CAST")
fun convertOptionalArgs(args: List<Any?>, expected: List<CommandArgument>, event: CommandEvent) =
        args.mapIndexed { i, arg ->
            arg ?: if (expected[i].defaultValue is Function<*>)
                       (expected[i].defaultValue as (CommandEvent) -> Any).invoke(event)
                   else
                       expected[i].defaultValue
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

private fun convertArg(arg: String, type: ArgumentType, index: Int, actual: List<String>): Any {
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


private fun joinArgs(start: Int, actual: List<String>) = actual.subList(start, actual.size).reduce { a, b -> "$a $b" }

private fun splitArg(start: Int, actual: List<String>): List<String> {
    val joined = joinArgs(start, actual)

    if (!(joined.contains(separatorCharacter))) return listOf(joined)

    return joined.split(separatorCharacter).toList()
}

