package me.aberrantfox.aegus

import me.aberrantfox.aegeus.commandframework.extensions.containsInvite
import me.aberrantfox.aegeus.commandframework.extensions.containsURL
import net.dv8tion.jda.core.MessageBuilder
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object MessageExtensionsSpec : Spek ({
    describe("Message extensions") {
        on("Invite Check") {
            it("should match an invite") {
                assertTrue(msg("https://discord.gg/BQN6BYE").containsInvite())
            }
            it("shouldn't match a string that doesn't contain an invite") {
                assertFalse(msg("aaa bbb ccc 123aosd").containsInvite())
            }
            it("should match multiple invites") {
                assertTrue(msg("https://discord.gg/BQN6BYE https://discord.gg/Basdas").containsInvite())
            }
            it("should match invite messages containing new lines") {
                assertTrue(msg("\n\nhttps://discord.gg/BQN6BYE \n \n https://discord.gg/BQN6BYE").containsInvite())
            }
            it("should match a command invocation") {
                assertTrue(msg("\$https://discord.gg/sfwe34jd").containsInvite())
            }
        }
        on("URL Check") {
            it("shouldn't match a string containing no URL") {
                assertFalse(msg("abc 123 https:/ asdasdd 0-0204234mm . com ").containsURL())
            }
            it("should match a fully formed URL") {
                assertTrue(msg("http://www.google.com/").containsURL())
            }
            it("should match a https URL") {
                assertTrue(msg("https://www.google.com/a/b/c/d/e/f").containsURL())
            }
            it("should match a string with new lines containing a URL") {
                assertTrue(msg("https://www.google.com/ \n \n").containsURL())
            }
            it("should match a url without a protocol") {
                assertTrue(msg("www.google.net").containsURL())
            }
            it("should not match random text that looks like a URL") {
                assertFalse(msg("http abc123123123 :: // www.g00ad oiwoifjweofij wefw\n asd./com").containsURL())
            }
            it("shouldnt match based on just dots") {
                assertFalse(msg("a.b.c").containsURL())
            }
        }
    }
})

private fun msg(data: String) = MessageBuilder().append(data).build()