package me.aberrantfox.hotbot.mocks.kutils

import io.mockk.every
import io.mockk.mockk
import me.aberrantfox.hotbot.mocks.jda._discordMock
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import net.dv8tion.jda.api.entities.User


fun createCommandEventMock(_author: User, vararg _args: Any) = mockk<CommandEvent> (relaxed = true) {
    every { author } returns _author
    every { args } returns _args.toList()
    every { discord } returns _discordMock
}