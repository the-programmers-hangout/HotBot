package me.aberrantfox.hotbot.permissions

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.experimental.runBlocking
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.ServerInformation
import me.aberrantfox.kjdautils.api.dsl.Command
import me.aberrantfox.kjdautils.api.dsl.CommandsContainer
import me.aberrantfox.kjdautils.internal.logging.DefaultLogger
import net.dv8tion.jda.core.JDA
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

    val command = Command(commandName)

    val container = CommandsContainer(hashMapOf(commandName to command))

    val serverInfo = ServerInformation(
            ownerID = "",
            guildid = "guildid"
    )

    val userMock = mock<User> {
        on { id } doReturn "non-blank"
    }

    val memberMock = mock<Member> {
        on { roles } doReturn listOf<Role>()
    }

    val guildMock = mock<Guild> {
        on { getMember(userMock) } doReturn memberMock
    }

    val config = mock<Configuration> {
        on { serverInformation } doReturn serverInfo
    }

    val jdaMock = mock<JDA> {
        on { getGuildById(config.serverInformation.guildid) } doReturn guildMock
    }

    val manager = PermissionManager(jdaMock, config, permsFilePath)
    manager.setDefaultPermissions(container)
    runBlocking { manager.setPermission(commandName, PermissionLevel.JrMod).join() }

    return manager
}

private fun cleanupFiles() {
    val permsFile = File(permsFilePath)

    Files.deleteIfExists(permsFile.toPath())
}