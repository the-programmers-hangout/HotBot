package me.aberrantfox.aegeus.commandframework.commands.dsl

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*

data class CommandEvent(val args: List<Any>, val config: Configuration, val jda: JDA, val channel: MessageChannel,
                        val author: User, val message: Message, val guild: Guild?) {

    fun respond(msg: String) = this.channel.sendMessage(msg).queue()
    fun respond(embed: MessageEmbed) = this.channel.sendMessage(embed).queue()
}

@CommandTagMarker
class Command(var expectedArgs: Array<out CommandArgument> = arrayOf(), var execute: (CommandEvent) -> Unit = {}, var requiresGuild: Boolean = false) {
    operator fun invoke(args: Command.() -> Unit) {}

    fun requiresGuild(requiresGuild: Boolean) {
        this.requiresGuild = requiresGuild
    }

    fun execute(execute: (CommandEvent) -> Unit) {
        this.execute = execute
    }

    fun expect(vararg args: CommandArgument) {
        this.expectedArgs = args
    }

    fun expect(vararg args: ArgumentType) {
        val clone = arrayOf<CommandArgument>()
        for (x in args.indices) {
            clone[x] = arg(args[x])
        }

        this.expectedArgs = clone
    }

    fun expect(args: Command.() -> Array<out CommandArgument>) {
        this.expectedArgs = args()
    }
}

data class CommandArgument(val type: ArgumentType, val optional: Boolean = false, val defaultValue: Any = "")

@CommandTagMarker
data class Commands(val commands: HashMap<String, Command> = HashMap()) {
    operator fun invoke(args: Commands.() -> Unit) {}

    fun command(name: String, construct: Command.() -> Unit): Command {
        val command = Command()
        command.construct()
        this.commands.put(name, command)
        return command
    }

    fun join(vararg cmds: Commands) {
        cmds.forEach {
            this.commands.putAll(it.commands)
        }
    }
}

@DslMarker
annotation class CommandTagMarker

fun commands(construct: Commands.() -> Unit): Commands {
    val commands = Commands()
    commands.construct()
    return commands
}

fun arg(type: ArgumentType, optional: Boolean = false) = CommandArgument(type, optional)

fun sampleCommands() =
    commands {
        command("abc") {
            expect(arg(ArgumentType.Joiner, true))
            requiresGuild(false)
            execute {
                it.respond("hi")
            }
        }
    }
