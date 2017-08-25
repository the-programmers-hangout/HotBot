package me.aberrantfox.aegeus.commandframework.commands

import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.io.File
import java.time.LocalDateTime

data class HelpFile(val commands: List<CommandDescriptor>)

data class CommandDescriptor(val name: String,
                             val description: String,
                             val structure: String,
                             val example: String,
                             val category: String)

enum class SelectionArgument { CommandName, CategoryName }

object HelpConf {
    val configuration: HelpFile

    init {
        val data = File("help.json").readText()
        val gson = Gson()

        configuration = gson.fromJson(data, HelpFile::class.java)
    }

    fun fetchArgumentType(value: String): SelectionArgument? {
        val isCategory = configuration.commands.any { it.category.toLowerCase() == value.toLowerCase() }

        if(isCategory) return SelectionArgument.CategoryName

        val isCommandName = configuration.commands.any { it.name.toLowerCase() == value.toLowerCase() }

        if(isCommandName) return SelectionArgument.CommandName

        return null
    }

    fun fetchCommandsInCategory(category: String): String =
            HelpConf.configuration.commands
                    .filter { it.category.toLowerCase() == category.toLowerCase() }
                    .map { it.name }
                    .reduceRight {a, b -> "$a, $b" }

    fun fetchCommandDescriptor(command: String) =
            HelpConf.configuration.commands.findLast { it.name.toLowerCase() == command.toLowerCase() }

    fun fetchCategories() =
            HelpConf.configuration.commands
                    .map { it.category }
                    .toSet()
                    .reduce { a, b ->  "$a, $b"}
}

@Command(ArgumentType.Manual)
fun help(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val user = event.author

    when (args.size) {
        0 -> sendPrivateMessage(user, getZeroArgMessage(config))
        1 -> {
            val selection = args[0] as String
            val argType = HelpConf.fetchArgumentType(selection)


            when (argType) {
                SelectionArgument.CommandName -> {
                    val descriptor = HelpConf.fetchCommandDescriptor(selection) ?: return
                    sendPrivateMessage(user, buildCommandHelpMessage(config, descriptor))
                }
                SelectionArgument.CategoryName -> {
                    val categories = HelpConf.fetchCommandsInCategory(selection)
                    sendPrivateMessage(user, categories)
                }
                else -> sendPrivateMessage(user, "Not a command or category... maybe try the default help command?")
            }
        }
        else -> {
            sendPrivateMessage(user, "Uhh... this command takes either 0 or 1 arguments.")
        }
    }

    event.message.delete().queue()
}

private fun sendPrivateMessage(user: User, msg: MessageEmbed) =
    user.openPrivateChannel().queue {
        it.sendMessage(msg).queue()
    }


private fun sendPrivateMessage(user: User, msg: String) =
    user.openPrivateChannel().queue {
        it.sendMessage(msg).queue()
    }

private fun buildCommandHelpMessage(config: Configuration, descriptor: CommandDescriptor) =
        EmbedBuilder().setTitle("${descriptor.category} - ${descriptor.name}")
                .setDescription(descriptor.description)
                .setColor(Color.CYAN)
                .addField("Example Command",
                        "${config.prefix}${descriptor.name} ${descriptor.example}",
                        false)
                .build()

private fun getZeroArgMessage(config: Configuration) =
        EmbedBuilder().setTitle("HotBot Help Menu")
                .setColor(Color.MAGENTA)
                .setDescription("This is the help menu. Use ${config.prefix}help <Command | Category>." +
                        "If you want to know what commands you have access to, use ${config.prefix}listAvailable")
                .setFooter("Bot by Fox, made with Kotlin", "http://i.imgur.com/SJPggeJ.png")
                .setThumbnail("http://i.imgur.com/DFoaG7k.png")
                .setTimestamp(LocalDateTime.now())
                .addField("Currently Available Categories",
                        HelpConf.fetchCategories(),
                        false)
                .build()