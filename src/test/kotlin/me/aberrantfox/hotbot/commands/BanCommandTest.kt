package me.aberrantfox.hotbot.commands


import io.mockk.verify
import me.aberrantfox.hotbot.commands.administration.createBanCommands
import me.aberrantfox.hotbot.mocks.hotbot._databaseServiceMock
import me.aberrantfox.hotbot.mocks.jda._moderatorUserMock
import me.aberrantfox.hotbot.mocks.jda._userMock
import me.aberrantfox.hotbot.mocks.kutils.createCommandEventMock
import org.junit.jupiter.api.Test

private const val testReason = "Test ban reason"

class BanCommandTest {
    val cmds = createBanCommands(_databaseServiceMock)

    @Test
    fun `The ban command should call Guild#ban`() {

        val event = createCommandEventMock(_moderatorUserMock, _userMock, 1, testReason)
        cmds["ban"]!!.execute(event)

        verify(exactly = 1) {
            event.guild!!.ban(_userMock, 1, testReason)
        }
    }
}