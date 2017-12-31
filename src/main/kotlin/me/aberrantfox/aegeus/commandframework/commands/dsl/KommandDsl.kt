package me.aberrantfox.aegeus.commandframework.commands.dsl

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*

data class CommandEvent(val args: List<Any>, val config: Configuration, val jda: JDA, val channel: MessageChannel,
                        val author: User, val message: Message, val guild: Guild) {

    fun respond(msg: String) = this.channel.sendMessage(msg).queue()
    fun respond(embed: MessageEmbed) = this.channel.sendMessage(embed).queue()
}

@CommandTagMarker
class Command(var expectedArgs: Array<out CommandArgument> = arrayOf(), var execute: (CommandEvent) -> Unit = {},
              var requiresGuild: Boolean = false) {
    val parameterCount = expectedArgs.size

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
        val clone = Array(args.size) { arg(ArgumentType.String) }

        for (x in args.indices) {
            clone[x] = arg(args[x])
        }

        this.expectedArgs = clone
    }

    fun expect(args: Command.() -> Array<out CommandArgument>) {
        this.expectedArgs = args()
    }
}

data class CommandArgument(val type: ArgumentType, val optional: Boolean = false, val defaultValue: Any = "") {
    override fun equals(other: Any?): Boolean {
        if(other == null) return false

        if(other !is CommandArgument) return false

        return other.type == this.type
    }
}

@CommandTagMarker
data class CommandsContainer(val commands: HashMap<String, Command> = HashMap()) {
    operator fun invoke(args: CommandsContainer.() -> Unit) {}

    fun command(name: String, construct: Command.() -> Unit): Command {
        val command = Command()
        command.construct()
        this.commands.put(name, command)
        return command
    }

    fun join(vararg cmds: CommandsContainer) {
        cmds.forEach {
            this.commands.putAll(it.commands)
        }
    }

    fun has(name: String) = this.commands.containsKey(name)

    fun get(name: String) = this.commands.get(name)
}

@DslMarker
annotation class CommandTagMarker

fun commands(construct: CommandsContainer.() -> Unit): CommandsContainer {
    val commands = CommandsContainer()
    commands.construct()
    return commands
}

fun arg(type: ArgumentType, optional: Boolean = false) = CommandArgument(type, optional)