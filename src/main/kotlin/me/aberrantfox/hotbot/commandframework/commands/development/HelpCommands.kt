package me.aberrantfox.hotbot.commandframework.commands.development

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.jda.sendPrivateMessage
import me.aberrantfox.hotbot.services.CommandDescriptor
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.HelpConf
import me.aberrantfox.hotbot.services.SelectionArgument
import java.awt.Color
import java.time.LocalDateTime

@CommandSet
fun helpCommands() =
    commands {
        command("help") {
            expect(arg(ArgumentType.Word, optional = true, default = "help menu"))
            // "help menu" to avoid conflicts with any future commands
            execute {
                val author = it.author
                val config = it.config
                val selection = it.args.component1() as String

                if (selection == "help menu") {
                    author.sendPrivateMessage(getZeroArgMessage(config))
                    return@execute
                }

                val argType = HelpConf.fetchArgumentType(selection)
                when (argType) {
                    SelectionArgument.CommandName -> {
                        val descriptor = HelpConf.fetchCommandDescriptor(selection)
                        if (descriptor != null) {
                            author.sendPrivateMessage(buildCommandHelpMessage(config, descriptor))
                        } else {
                            author.sendPrivateMessage("A descriptor was null, please notify the bot owner.")
                        }
                    }
                    SelectionArgument.CategoryName -> {
                        val categories = HelpConf.fetchCommandsInCategory(selection)
                        author.sendPrivateMessage(buildCategoryDescription(selection.toLowerCase(), categories))
                    }
                    else -> author.sendPrivateMessage("Not a command or category... maybe try the default help command?")
                }
            }
        }
    }

private fun buildCommandHelpMessage(config: Configuration, descriptor: CommandDescriptor) =
    embed {
        title("${descriptor.category} - ${descriptor.name}")
        description(descriptor.description)
        setColor(Color.CYAN)

        field {
            name = "Argument structure"
            value = if(descriptor.structure.isNullOrBlank()) "This command takes no arguments" else descriptor.structure
        }

        field {
            name = "Example Command"
            value = "${config.serverInformation.prefix}${descriptor.name} ${descriptor.example}"
        }
    }

private fun buildCategoryDescription(commandName: String, commands: String) =
    embed {
        title("Category $commandName Overview")
        description(HelpConf.configuration.categoryDescriptions[commandName])
        field {
            name  = "Commands In this Category"
            value = commands
        }
    }

private fun getZeroArgMessage(config: Configuration) =
    embed {
        title("HotBot Help Menu")
        setColor(Color.MAGENTA)
        description("This is the help menu. Use ${config.serverInformation.prefix}help <Command | Category>." +
            "If you want to know what commands you have access to, use ${config.serverInformation.prefix}listAvailable")
        setFooter("Bot by Fox, made with Kotlin", "http://i.imgur.com/SJPggeJ.png")
        setThumbnail("http://i.imgur.com/DFoaG7k.png")
        setTimestamp(LocalDateTime.now())

        field {
            name = "Currently Available Categories"
            value = HelpConf.fetchCategories()
        }
    }