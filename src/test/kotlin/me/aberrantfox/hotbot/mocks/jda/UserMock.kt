package me.aberrantfox.hotbot.mocks.jda

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import me.aberrantfox.hotbot.constants.sampleModeratorName
import me.aberrantfox.hotbot.constants.sampleModeratorUserID
import me.aberrantfox.hotbot.constants.sampleUserID
import me.aberrantfox.hotbot.constants.sampleUserName
import me.aberrantfox.kjdautils.extensions.jda.fullName
import net.dv8tion.jda.api.entities.User

val _userMock = mockk<User> (relaxed = true) {
    every { id } returns sampleUserID

}

val _moderatorUserMock = mockk<User> (relaxed = true) {
    every { id } returns sampleModeratorUserID

}
