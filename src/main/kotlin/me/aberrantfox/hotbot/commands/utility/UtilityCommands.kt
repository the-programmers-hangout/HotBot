package me.aberrantfox.hotbot.commands.utility

import com.google.gson.Gson
import khttp.post
import me.aberrantfox.hotbot.arguments.HexColourArg
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.javautilities.UrlUtilities.sendImageToChannel
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import me.aberrantfox.kjdautils.internal.command.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.*
import org.joda.time.DateTime
import java.awt.Color
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToLong


data class Properties(val version: String, val author: String)

object Project {
    val properties: Properties

    init {
        val propFile = Configuration::class.java.getResource("/properties.json").readText()
        val gson = Gson()
        properties = gson.fromJson(propFile, Properties::class.java)
    }
}

val startTime = Date()

@CommandSet("utility")
fun utilCommands(messageService: MessageService, manager: PermissionService, config: Configuration, log: BotLogger) = commands {
    command("ping") {
        description = "Pong!"
        execute {
            it.respond("Responded in ${it.jda.ping}ms")
        }
    }

    command("botinfo") {
        description = "Display the bot information."
        execute {
            it.respond(embed {
                title(it.jda.selfUser.fullName())
                description(messageService.messages.botDescription)
                setColor(Color.red)
                setThumbnail(it.jda.selfUser.effectiveAvatarUrl)

                field {
                    name = "Creator"
                    value = "Fox#0001"
                    inline = false
                }
                field {
                    name = "Contributors"
                    value = "JoshTheWall#3698, Moe#9999, Sudonym#8623"
                }
                field {
                    name = "Technologies"
                    value = "Built with Kotlin us KUtils and JDA"
                    inline = false
                }
                field {
                    name = "Repository link"
                    value = "https://gitlab.com/AberrantFox/hotbot"
                    inline = false
                }
            })
        }
    }

    command("serverinfo") {
        description = "Display a message giving basic server information"
        execute {
            val guild = it.jda.getGuildById(config.serverInformation.guildid)
            val embed = produceServerInfoEmbed(guild, messageService)
            it.respond(embed)
        }
    }

    command("uptime") {
        description = "Displays how long you have kept me, HOTBOT, AWAKE!"
        execute {
            val milliseconds = Date().time - startTime.time

            it.respond(embed {
                setColor(Color.WHITE)
                setTitle("I have been running since")
                setDescription(startTime.toString())

                field {
                    name = "That's been"
                    value = timeToString(milliseconds)
                }
            })
        }
    }

    command("exit") {
        description = "Exit, saving configurations."
        execute {
            it.respond("Exiting")
            saveConfig(config)
            log.info("saved configurations")
            manager.save()
            log.info("saved permissions to database prior to shut down.")
            System.exit(0)
        }
    }

    command("kill") {
        description = "Exit, without saving configurations."
        execute {
            it.respond("Killing process, configurations will not be saved.")
            System.exit(0)
        }
    }

    command("saveconfigurations") {
        description = "Save the configuration of the bot. You may want to do this if you change the prefix."
        execute {
            saveConfig(config)
            it.respond("Configurations saved. I hope you know what you are doing...")
        }
    }

    command("version") {
        description = "Display the bot version -- this is updated via maven filtering."
        execute {
            it.respond("**Hotbot version**: ${Project.properties.version}")
        }
    }

    command("author") {
        description = "Display project authors -- this is updated via maven filtering."
        execute {
            it.respond("**Project author**: ${Project.properties.author}")
        }
    }

    command("echo") {
        description = "Echo a message to a channel"
        expect(arg(TextChannelArg, optional=true, default={ it.channel }), arg(SentenceArg("Message")))
        execute {
            val target = it.args[0] as TextChannel
            val message = it.args[1] as String
            val safeMessage = message.sanitiseMentions()
            target.sendMessage(safeMessage).queue()
        }
    }

    command("viewcreationdate") {
        description = "See when a user was created"
        expect(UserArg)
        execute {
            val target = it.args.component1() as User
            it.respond("${target.fullName()}'s account was made on ${target.creationTime}")
        }
    }

    command("uploadtext") {
        description = "Uploads the given block of text/code to hastebin and removes the invocation"
        expect(SentenceArg("Text"))
        execute {
            val text = it.args.component1() as String
            val response = post("https://hastebin.com/documents", data = text).jsonObject

            if (it.commandStruct.doubleInvocation)
                it.message.delete().queue()

            it.respond("${it.author.fullName()}'s paste: https://hastebin.com/" + response.getString("key"))
        }
    }

    command("colour") {
        description = "Shows an embed with the given hex colour code"
        expect(HexColourArg)
        execute {
            val colour = it.args.component1() as Int
            val hex = colour.toString(16).padStart(6, '0')
            val response = embed {
                setColor(colour)
                setTitle("Colour")
                setDescription("#$hex")
                setThumbnail("http://via.placeholder.com/40/$hex?text=%20&")
            }
            it.respond(response)
        }
    }

    command("whatpfp"){
        description = "Returns the reverse image url of a users profile picture."
        expect(UserArg)
        execute {
            val user = it.args.component1() as User
            val reverseSearchUrl = "<https://www.google.com/searchbyimage?&image_url=${user.effectiveAvatarUrl}>"

            val embed = embed {
                setTitle("${user.name}'s pfp")
                setColor(Color.BLUE)
                setImage(user.effectiveAvatarUrl)
                setDescription("[Reverse Search]($reverseSearchUrl)")
            }

            it.respond(embed)
        }
    }

    command ("latex"){
        description = "A command that will parse latex"
        expect(SentenceArg("LaTeX Text"))
        execute {
            val input = it.args.component1() as String
            val latex = URLEncoder.encode(input, "UTF-8")

            val url = "http://chart.apis.google.com/chart?cht=tx&chl=$latex"
            sendImageToChannel(url, "latex-processed.png", "Could not process latex", it.channel)
        }
    }


    command("selfmute") {
        description = "Need to study and want no distractions? Mute yourself! (Length defaults to 1 hour)"
        expect(arg(TimeStringArg, true, 3600.0))
        execute {
            val time = (it.args.component1() as Double).roundToLong() * 1000
            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            if(isMemberMuted(it.author.id, guild.id)) {
                it.respond("Nice try but you're already muted")
                return@execute
            }

            if(time > 1000*60*config.serverInformation.maxSelfmuteMinutes){
                it.respond("Sorry but you can't mute yourself for that long")
                return@execute
            }

            muteMember(guild, it.author, time, "No distractions for a while? Got it", config, it.author, log)
        }
    }

    command("remainingmute") {
        description="Return the remaining time of a mute"
        execute {
            try{
                val unmuteTime = getUnmuteRecord(it.author.id, config.serverInformation.guildid)- DateTime().millis
                it.respond(timeToString(unmuteTime))
            }catch (e: NoSuchElementException){
                it.respond("You aren't currently muted...")
            }

        }
    }
}

fun produceServerInfoEmbed(guild: Guild, messageService: MessageService) =
    embed {
        title(guild.name)
        setColor(Color.MAGENTA)
        description(messageService.messages.serverDescription)
        setFooter("Guild creation date: ${guild.creationTime.format(DateTimeFormatter.RFC_1123_DATE_TIME)}", "http://i.imgur.com/iwwEprG.png")
        setThumbnail("http://i.imgur.com/DFoaG7k.png")

        field {
            name = "Users"
            value = "${guild.members.filter { it.onlineStatus != OnlineStatus.OFFLINE }.size}/${guild.members.size}"
        }

        ifield {
            name = "Total Roles"
            value = guild.roles.size.toString()
        }

        ifield {
            name = "Owner"
            value = guild.owner.fullName()
        }

        ifield {
            name = "Region"
            value = guild.region.toString()
        }

        ifield {
            name = "Text Channels"
            value = guild.textChannelCache.size().toString()
        }

        ifield {
            name = "Voice Channels"
            value = guild.voiceChannels.size.toString()
        }
    }
