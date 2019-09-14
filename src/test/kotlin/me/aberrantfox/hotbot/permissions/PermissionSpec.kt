package me.aberrantfox.hotbot.permissions

import com.nhaarman.mockito_kotlin.*
import kotlinx.coroutines.runBlocking
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.discord.Discord
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import java.io.File
import java.nio.file.Files

private object TestData {
    const val commandName = "test-command"
    const val defaultPermissionCommand = "unknown-permission-command"
    val commands = listOf(Command(commandName), Command(defaultPermissionCommand))
    val container = CommandsContainer(HashMap(commands.associateBy { it.name }))

    const val permsFilePath = "fake-path"

    val serverInfo = ServerInformation(
            ownerID = "ownerid",
            guildid = "guildid"
    )

    val userMock = mock<User> { on { id } doReturn "some-user" }
    val moderatorUser = mock<User> { on { id } doReturn "staff-user" }

    val modRole = mock<Role> { on { id } doReturn "mod-role" }

    val memberMock = mock<Member> { on { roles } doReturn listOf<Role>() }
    val moderatorMember = mock<Member> { on { roles } doReturn listOf(modRole) }

    val guildMock = mock<Guild> {
        on { getMember(userMock) } doReturn memberMock
        on { getMember(moderatorUser) } doReturn moderatorMember
    }

    val config = mock<Configuration> {
        on { serverInformation } doReturn serverInfo
    }

    val jdaMock = mock<JDA> {
        on { getGuildById(config.serverInformation.guildid) } doReturn guildMock
    }

    val discordMock = mock<Discord> {
        on { jda } doReturn jdaMock
    }

}


object PermissionSpec : Spek({
    describe("Permission Manager") {
        val manager = PermissionService(TestData.discordMock, TestData.config)
        beforeGroup {
            manager.defaultAndPrunePermissions(TestData.container)
            runBlocking { manager.setPermission(TestData.commandName, PermissionLevel.JrMod).join() }
            manager.assignRoleLevel(TestData.modRole, PermissionLevel.Moderator)
        }


        on("command with default permissions") {
            it("should require Administrator") {
                manager.roleRequired(TestData.defaultPermissionCommand) shouldBe PermissionLevel.Administrator
            }
        }

        on("command with JrMod permission level") {
            it("should require JrMod") {
                manager.roleRequired(TestData.commandName) shouldBe PermissionLevel.JrMod
            }
            it("should be usable by Moderator") {
                manager.canUseCommand(TestData.moderatorUser, TestData.commandName) shouldBe true
            }
            it("should not be usable by user with no role") {
                manager.canUseCommand(TestData.userMock, TestData.commandName) shouldBe false
            }
        }

        on("command with unknown permission") {
            it("should require Owner") {
                manager.roleRequired("unknown-command") shouldBe PermissionLevel.Owner
            }
            it("should not be usable by Moderator") {
                manager.canUseCommand(TestData.moderatorUser, "unknown-command") shouldBe false
            }
        }

        on("comparing Moderator to non-staff user") {
            it("should be greater than 0") {
                manager.compareUsers(TestData.moderatorUser, TestData.userMock) shouldBeGreaterThan 0
            }
        }

        afterGroup {
            cleanupFiles()
        }
    }
})


private fun cleanupFiles() {
    val permsFile = File(TestData.permsFilePath)

    Files.deleteIfExists(permsFile.toPath())
}