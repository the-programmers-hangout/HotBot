package me.aberrantfox.hotbot.extensions

import org.junit.Assert
import org.junit.Test


class UrlMatchTests {
    @Test
    fun shouldntMatchANonURL() = Assert.assertFalse(msg("abc 123 https:/ asdasdd 0-0204234mm . com ").containsURL())

    @Test
    fun shouldMatchAFullyFormedURL() = Assert.assertTrue(msg("http://www.google.com/").containsURL())

    @Test
    fun shouldMatchAHttpsUrl() = Assert.assertTrue(msg("https://www.google.com/a/b/c/d/e/f").containsURL())

    @Test
    fun shouldMatchUrlWithoutAProtocol() = Assert.assertTrue(msg("www.google.net").containsURL())

    @Test
    fun shouldNotMatchTextThatLooksLikeAUrl() =
        Assert.assertFalse(msg("http abc123123123 :: // www.g00ad oiwoifjweofij wefw\n asd./com").containsURL())
}