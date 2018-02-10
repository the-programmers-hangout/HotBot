package me.aberrantfox.hotbot.extensions

import net.dv8tion.jda.core.MessageBuilder
import org.junit.Assert
import org.junit.Test

class InviteMatchTest {
    @Test
    fun invitesMatchValidInvites() = Assert.assertTrue(msg("https://discord.gg/BQN6BYE").containsInvite())

    @Test
    fun doesntMatchNonInvite() = Assert.assertFalse(msg("aaa bbb ccc 123aosd").containsInvite())

    @Test
    fun matchesMultipleInvites() =
        Assert.assertTrue(msg("https://discord.gg/BQN6BYE https://discord.gg/Basdas").containsInvite())

    @Test
    fun matchesInvitesWithNewLines() =
        Assert.assertTrue(msg("\n\nhttps://discord.gg/BQN6BYE \n \n https://discord.gg/BQN6BYE").containsInvite())

    @Test
    fun matchesCommandInvocation() =
        Assert.assertTrue(msg("\$https://discord.gg/sfwe34jd").containsInvite())
}

fun msg(data: String) = MessageBuilder().append(data).build()