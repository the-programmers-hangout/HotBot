package me.aberrantfox.hotbot.commandframework

import me.aberrantfox.hotbot.commandframework.ArgumentType.*
import me.aberrantfox.hotbot.commandframework.ArgumentType.Double
import me.aberrantfox.hotbot.dsls.command.CommandArgument
import me.aberrantfox.hotbot.dsls.command.arg
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class InvalidArgumentConversionTest {

    // test(input args, expected args)

    private val invalidSingleTypeArgs = listOf(
            test(emptyList(), listOf(arg(Integer))),
            test(emptyList(), listOf(arg(Sentence))),
            test(emptyList(), listOf(arg(Splitter))),

            test(listOf("hello"), listOf(arg(Integer))),
            test(listOf("test"), listOf(arg(Double))),
            test(listOf("5123"), listOf(arg(Choice))),
            test(listOf("ajaslkdas"), listOf(arg(URL)))
    )

    private val invalidComboArgs = listOf(
            test(listOf("5"), listOf(arg(Integer), arg(Sentence))),
            test(listOf("f"), listOf(arg(Choice), arg(Splitter))),

            test(listOf("124", "832.23482"), listOf(arg(Integer), arg(URL), arg(Double))),
            test(listOf("921.25", "woaskpdas", "asdasd", "asdasd"), listOf(arg(Double), arg(Word), arg(Word))),
            test(listOf("word", "91293", "41.2"), listOf(arg(Double), arg(Word), arg(Integer))),
            test(listOf("402868974108409867"), listOf(arg(User), arg(Word))),

            test(emptyList(), listOf(arg(Word), arg(Integer), arg(User)))
    )

    private val invalidOptionalArgs = listOf(
            test(emptyList(), listOf(arg(Integer), arg(Word, true, "asdads"))),

            test(listOf("adasd"), listOf(arg(Integer, true, 42))),
            test(listOf("hello", "test", "123"), listOf(arg(Integer), arg(Sentence, true, "asjodaspd asd"))),
            test(listOf("404665584274505728", "5"), listOf(arg(User), arg(Integer, true, 12), arg(Sentence))),
            test(listOf("test"), listOf(arg(Integer, true, 54), arg(Double))),
            test(listOf("kdas"), listOf(arg(Integer, true, 52), arg(Double, true, 25.2), arg(Word), arg(Word))),

            test(listOf("4", "akdasd", "2"), listOf(arg(Integer, true, 24), arg(Double), arg(Splitter)))
    )

    @TestFactory
    fun testInvalidSingleTypeArgs() = mapInvalidTest(invalidSingleTypeArgs)

    @TestFactory
    fun testInvalidComboArgs() = mapInvalidTest(invalidComboArgs)

    @TestFactory
    fun testInvalidOptionalArgs() = mapInvalidTest(invalidOptionalArgs)

    private fun mapInvalidTest(argData: List<Pair<List<String>, List<CommandArgument>>>) =
            argData.map { (input, expected) ->
                DynamicTest.dynamicTest("Input $input against ${expected.map { Pair(it.type, it.optional) }} should cause a null return") {
                    Assertions.assertNull(convertMainArgs(input, expected))
                }
            }

    private fun test(input: List<String>, expectedArgs: List<CommandArgument>) = input to expectedArgs
}