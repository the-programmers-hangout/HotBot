package me.aberrantfox.hotbot.commands.administration


import io.mockk.verify
import me.aberrantfox.hotbot.constants.sampleModeratorUserID
import me.aberrantfox.hotbot.constants.sampleUserID
import me.aberrantfox.hotbot.mocks.hotbot._databaseServiceMock
import me.aberrantfox.hotbot.mocks.jda._moderatorUserMock
import me.aberrantfox.hotbot.mocks.jda._userMock
import me.aberrantfox.hotbot.mocks.kutils.createCommandEventMock
import org.junit.jupiter.api.Test

private const val testReason = "Test ban reason"

class BanCommandTest {
    val cmds = createBanCommands(_databaseServiceMock)

    @Test
    fun `The ban command should call Guild#ban and log the ban in the DB`() {
        val event = createCommandEventMock(_moderatorUserMock, _userMock, 1, testReason)
        cmds["ban"]!!.execute(event)

        verify {
            _databaseServiceMock.bans.updateOrSetReason(sampleUserID, testReason, sampleModeratorUserID)
            event.guild!!.ban(_userMock, 1, testReason)
        }
    }

    @Test
    fun `SetBanReason should call on the db to store the ban reason`() {
        val event = createCommandEventMock(_moderatorUserMock, _userMock, testReason)
        cmds["setbanreason"]!!.execute(event)

        verify {
            _databaseServiceMock.bans.updateOrSetReason(sampleUserID, testReason, sampleModeratorUserID)
        }
    }

    @Test
    fun `GetBanReason should pull the correct information from the DB and return it`() {
        val event = createCommandEventMock(_moderatorUserMock, _userMock)
        cmds["getbanreason"]!!.execute(event)

        verify {
            _databaseServiceMock.bans.getReason(sampleUserID)
        }
    }
}