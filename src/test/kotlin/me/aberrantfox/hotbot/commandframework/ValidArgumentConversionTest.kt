package me.aberrantfox.hotbot.commandframework

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType.*
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType.Double
import me.aberrantfox.hotbot.commandframework.parsing.convertMainArgs
import me.aberrantfox.hotbot.dsls.command.CommandArgument
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.dsls.command.produceContainer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class ValidArgumentConversionTest {

    // test(input args, expected args, expected result)

    private val validSingleTypeArgs = listOf(
            test(emptyList(), emptyList(), emptyList()),

            test(listOf("98"), listOf(arg(Integer)), listOf(98)),
            test(listOf("832.8235"), listOf(arg(Double)), listOf(832.8235)),
            test(listOf("AWordHere"), listOf(arg(Word)), listOf("AWordHere")),
            test(listOf("402546539849056267"), listOf(arg(User)), listOf("402546539849056267")),

            test(listOf("this is a load of text that shouldn't be modified"),
                    listOf(arg(Manual)),
                    listOf("this is a load of text that shouldn't be modified")),

            test(listOf("5", "23#", "this", "is", "a", "test", "sentence", "5", "2k31o23", "asd"),
                    listOf(arg(Sentence)),
                    listOf("5 23# this is a test sentence 5 2k31o23 asd")),

            test(listOf("Question", "with", "words", "?|", "asdjop", "asjkdoasdp", "|", "asldpasd", "as"),
                    listOf(arg(Splitter)),
                    listOf(listOf("Question with words ?", " asdjop asjkdoasdp ", " asldpasd as"))),

            test(listOf("https://i.imgur.com/t2atGQL.jpg"),
                    listOf(arg(URL)),
                    listOf("https://i.imgur.com/t2atGQL.jpg")),

            test(listOf("true", "false", "f", "t"),
                    listOf(arg(Choice), arg(Choice), arg(Choice), arg(Choice)),
                    listOf(true, false, false, true))
    )

    private val validComboArgs = listOf(
            test(listOf("124", "https://github.com/AberrantFox/hotbot", "832.23482"),
                    listOf(arg(Integer), arg(URL), arg(Double)),
                    listOf(124, "https://github.com/AberrantFox/hotbot", 832.23482)),

            test(listOf("wordjasidkashere", "f", "a", "question", "?|yes|no|Maybe"),
                    listOf(arg(Word), arg(Choice), arg(Splitter)),
                    listOf("wordjasidkashere", false, listOf("a question ?", "yes", "no", "Maybe"))),

            test(listOf("402868974108409867", "message", "with", "spaces", "test", "sentence"),
                    listOf(arg(User), arg(Sentence)),
                    listOf("402868974108409867", "message with spaces test sentence"))
    )

    private val validOptionalArgs = listOf(
            test(listOf("404665584274505728", "ajkdlsajd", "sentence", "from", "hereon", "52", "asd"),
                    listOf(arg(User), arg(Integer, true, 1), arg(Sentence)),
                    listOf("404665584274505728", null, "ajkdlsajd sentence from hereon 52 asd")),

            test(emptyList(),
                    listOf(arg(Word, true, "qweuiop")),
                    listOf(null)),

            test(listOf("23"),
                    listOf(arg(Integer, true, 49)),
                    listOf(23)),

            test(listOf("wordjaskdas"),
                    listOf(arg(Integer, true, 28), arg(Word)),
                    listOf(null, "wordjaskdas")),

            test(listOf("13", "asdjkasld", "asjdoaspdj", "no"),
                    listOf(arg(Integer), arg(Integer, true, 18), arg(Sentence)),
                    listOf(13, null, "asdjkasld asjdoaspdj no")),

            test(listOf("true", "is", "no?|yes|no|maybe|off"),
                    listOf(arg(Choice), arg(Choice, true, true), arg(Splitter)),
                    listOf(true, null, listOf("is no?", "yes", "no", "maybe", "off")))
    )

    private val container = produceContainer()

    @TestFactory
    fun testValidSingleTypeArgs() = mapValidTests(validSingleTypeArgs)

    @TestFactory
    fun testValidComboArgs() = mapValidTests(validComboArgs)

    @TestFactory
    fun testValidOptionalArgs() = mapValidTests(validOptionalArgs)

    private fun mapValidTests(argData: List<Pair<List<String>, Pair<List<CommandArgument>, List<Any?>>>>) =
            argData.map { (input, expected) ->
                DynamicTest.dynamicTest(
                        "Input $input should parse to ${expected.second}") {
                    Assertions.assertEquals(expected.second, convertMainArgs(input, expected.first, container).results)
                }
            }

    private fun test(input: List<String>, expectedArgs: List<CommandArgument>, expectedResult: List<Any?> = emptyList()) = input to Pair(expectedArgs, expectedResult)
}