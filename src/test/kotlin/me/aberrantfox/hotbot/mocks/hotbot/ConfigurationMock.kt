package me.aberrantfox.hotbot.mocks.hotbot

import io.mockk.mockk
import me.aberrantfox.hotbot.services.Configuration

val _configurationMock = mockk<Configuration> (relaxed = true) {
}