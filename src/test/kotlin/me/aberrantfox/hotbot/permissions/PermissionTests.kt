package me.aberrantfox.hotbot.permissions

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.experimental.runBlocking
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandsContainer
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.ServerInformation
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import org.junit.*
import java.io.File
import java.nio.file.Files

private const val commandName = "test-command"
private const val permsFilePath = "fake-path"

class PermissionTests {
    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeAll() = cleanupFiles()

        @AfterClass
        @JvmStatic
        fun afterAll() = cleanupFiles()
    }

    private lateinit var manager: PermissionManager

    @Before
    fun beforeEach() { manager = produceManager() }

    @Test
    fun correctPermissionIsStored() = assert(manager.roleRequired(commandName) == PermissionLevel.JrMod)

    @Test
    fun permissionStoredIsNotEqualToOtherPermissions() = assert(manager.roleRequired(commandName) != PermissionLevel.Moderator)

    @Test
    fun unknownCommandIsOfLevelOwner() = assert(manager.roleRequired("unknown-cmd-test") == PermissionLevel.Owner)
}

private fun produceManager(): PermissionManager {
    val userMock = mock<User> {
        on { id } doReturn "non-blank"
    }

    val memberMock = mock<Member> {
        on { roles } doReturn listOf<Role>()
    }

    val guildMock = mock<Guild> {
        on { getMember(userMock) } doReturn memberMock
    }

    val commandMock = mock<Command> {
        on { name } doReturn commandName
    }

    val containerMock = mock<CommandsContainer> {
        on { commands } doReturn hashMapOf(commandName to commandMock)
    }

    val serverInformationMock = mock<ServerInformation> {
        on { ownerID } doReturn ""
    }

    val config = mock<Configuration> {
        on { serverInformation } doReturn serverInformationMock
    }

    val manager = PermissionManager(guildMock, containerMock, config, permsFilePath)
    runBlocking { manager.setPermission(commandName, PermissionLevel.JrMod).join() }

    return manager
}

private fun cleanupFiles() {
    val permsFile = File(permsFilePath)

    Files.deleteIfExists(permsFile.toPath())
}