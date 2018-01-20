package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.CommandSet
import me.aberrantfox.aegeus.dsls.command.commands
import me.aberrantfox.aegeus.dsls.embed.embed
import me.aberrantfox.aegeus.extensions.sendPrivateMessage
import me.aberrantfox.aegeus.services.CommandDescriptor
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.HelpConf
import me.aberrantfox.aegeus.services.SelectionArgument
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color
import java.time.LocalDateTime

@CommandSet
fun helpCommands() =
    commands {
        command("help") {
            expect(ArgumentType.Manual)
            execute {
                val (args, config, _, _, author) = it

                if (args.isEmpty()) {
                    author.sendPrivateMessage(getZeroArgMessage(config))
                } else if (args.size == 1) {
                    val selection = args[0] as String
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
                } else {
                    author.sendPrivateMessage("Uhh... this command takes either 0 or 1 arguments.")
                }
            }
        }
    }

private fun buildCommandHelpMessage(config: Configuration, descriptor: CommandDescriptor) =
    embed {
        title("${descriptor.category} - ${descriptor.name}")
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