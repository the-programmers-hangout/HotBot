package framework

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.convertArguments
import org.junit.Assert.*
import org.junit.Test

class ArgumentConversionTests {

    @Test fun incorrectLengthsShouldReturnNull() {
        val actual = listOf("2", "2.22", "abc", "true")
        val expected = arrayOf(ArgumentType.Integer, ArgumentType.Double, ArgumentType.String)

        assertNull(convertArguments(actual, expected))
    }

    @Test fun integerShouldParseToInt() {
        val actual = listOf("1")
        val expected = arrayOf(ArgumentType.Integer)

        val converted = convertArguments(actual, expected)

        if(converted == null) {
            fail("Conversaion of single int, when int is expected should not return null.")
            return
        }

        converted[0] as Int
    }

    @Test fun doubleShouldParseToDouble() {
        val actual = listOf("1")
        val expected = arrayOf(ArgumentType.Double)

        val converted = convertArguments(actual, expected)

        if(converted == null) {
            fail("Conversaion of single double, when int is expected should not return null.")
            return
        }

        converted[0] as Double
    }

    @Test fun booleanOfAllStylesShouldParseToBooleanCorrectly() {
        val actual = listOf("t", "true", "f", "false")
        val expected = arrayOf(ArgumentType.Boolean, ArgumentType.Boolean, ArgumentType.Boolean, ArgumentType.Boolean)
        val converted = convertArguments(actual, expected)

        if(converted == null) {
            fail("Boolean conversion fail, it should parse all types of boolean without returning null")
            return
        }

        val t = converted[0] as Boolean
        val trueValue = converted[1] as Boolean
        val f = converted[2] as Boolean
        val falseValue = converted[3] as Boolean

        if(!t || !trueValue) {
            fail("Conversion error, expected True, actual False")
        }

        if(f || falseValue) {
            fail("Conversion error, expected False, actual True")
        }
    }

    @Test fun stringShouldParseToString() {
        val actual = listOf("abc", "1", "1.23", "false")
        val expected = arrayOf(ArgumentType.String, ArgumentType.String, ArgumentType.String, ArgumentType.String)

        val converted = convertArguments(actual, expected)

        if(converted == null) {
            fail("Failed to convert arguments to String, returned null instead")
        }
    }

    @Test fun manualShouldBeIgnored() {
        val actual = listOf("1", "abc", "123")
        var expected = arrayOf(ArgumentType.Manual)

        val converted = convertArguments(actual, expected)

        if(converted == null) {
            fail("Failed to ignore arguments as Manual, returned null instead")
            return
        }

        assertTrue(converted.zip(actual).all { it.first == it.second })
    }
}