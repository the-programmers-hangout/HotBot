package me.aberrantfox.hotbot.mocks.jda

import io.mockk.every
import io.mockk.mockk
import me.aberrantfox.kjdautils.discord.Discord

val _discordMock = mockk<Discord> (relaxed = true) {
    every { jda } returns _jdaMock
}