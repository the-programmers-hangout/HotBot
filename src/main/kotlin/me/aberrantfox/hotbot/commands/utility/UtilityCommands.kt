package me.aberrantfox.hotbot.commands.utility

import com.google.gson.Gson
import khttp.post
import me.aberrantfox.hotbot.database.getUnmuteRecord
import me.aberrantfox.hotbot.javautilities.UrlUtilities.sendImageToChannel
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.timeToString
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import me.aberrantfox.kjdautils.internal.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.*
import net.dv8tion.jda.api.entities.*
import org.joda.time.DateTime
import java.awt.Color
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToLong
import kotlin.system.exitProcess

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

const val uploadTextBaseUrl: String = "https://hasteb.in"

@CommandSet("utility")
fun utilCommands(messageService: MessageService, manager: PermissionService, config: Configuration,
                 log: BotLogger, muteService: MuteService) = commands {
    command("ping") {
        description = "Pong!"
        execute {
            it.discord.jda.restPing.queue { ping ->
                it.respond("Responded in ${ping}ms")
            }
        }
    }

    command("botinfo") {
        description = "Display the bot information."
        execute {
            it.respond(embed {
                title = it.discord.jda.selfUser.fullName()
                description = messageService.messages.botDescription
                color = Color.red
                thumbnail = it.discord.jda.selfUser.effectiveAvatarUrl

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
        requiresGuild = true
        execute {
            val embed = produceServerInfoEmbed(it.guild!!, messageService)
            it.respond(embed)
        }
    }

    command("uptime") {
        description = "Displays how long you have kept me, HOTBOT, AWAKE!"
        execute {
            val milliseconds = Date().time - startTime.time

            it.respond(embed {
                color = Color.WHITE
                title = "I have been running since"
                description = startTime.toString()

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
            exitProcess(0)
        }
    }

    command("kill") {
        description = "Exit, without saving configurations."
        execute {
            it.respond("Killing process, configurations will not be saved.")
            exitProcess(0)
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
            it.respond("${target.fullName()}'s account was made on ${target.timeCreated}")
        }
    }

    command("uploadtext") {
        description = "Uploads the given block of text/code to hastebin and removes the invocation"
        expect(SentenceArg("Text"))
        execute {
            val text = it.args.component1() as String
            try {
                val response = post("${uploadTextBaseUrl}/documents", data = text, timeout = 5.0).jsonObject

                if (it.commandStruct.doubleInvocation)
                    it.message.delete().queue()

                it.respond("${it.author.fullName()}'s paste: ${uploadTextBaseUrl}/${response.getString("key")}")
            } catch (exception: Exception) {
                it.respond("Uploading failed. Please try again or report this to staff.")
            }
        }
    }

    command("colour") {
        description = "Shows an embed with the given hex colour code"
        expect(HexColorArg)
        execute {
            val colour = it.args.component1() as Int
            val hex = colour.toString(16).padStart(6, '0')
            val response = embed {
                color = Color.decode("#$hex")
                title ="Colour"
                description = "#$hex"
                thumbnail = "http://via.placeholder.com/40/$hex?text=%20&"
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

            val embed = EmbedBuilder(embed {
                title = "${user.name}'s pfp"
                color = Color.BLUE
                description = "[Reverse Search]($reverseSearchUrl)"
            }) .setImage(user.effectiveAvatarUrl).build()

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
        requiresGuild = true
        expect(arg(TimeStringArg, true, 3600.0))
        execute {
            val time = (it.args.component1() as Double).roundToLong() * 1000
            val member = it.author.toMember(it.guild!!)!!

            if(muteService.checkMuteState(member) != MuteService.MuteState.None) {
                it.respond("Nice try but you're already muted")
                return@execute
            }

            if(time > 1000*60*config.serverInformation.maxSelfmuteMinutes) {
                it.respond("Sorry but you can't mute yourself for that long")
                return@execute
            } else if (time <= 0) {
                it.respond("Sorry, the laws of physics disallow muting for non-positive durations.")
                return@execute
            }

            muteService.muteMember(member, time, "No distractions for a while? Got it", it.author)
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

fun produceServerInfoEmbed(guild: Guild, messageService: MessageService) = with(guild) {
    EmbedBuilder(embed {
        title = name
        description = messageService.messages.serverDescription
        thumbnail = jda.selfUser.effectiveAvatarUrl
        color = Color.MAGENTA

        val onlineMembers = members.filter { it.onlineStatus != OnlineStatus.OFFLINE }.size

        addField("Users", "$onlineMembers/${members.size}")
        addInlineField("Total Roles", roles.size.toString())
        addInlineField("Owner", owner?.fullName())
        addInlineField("Region", region.toString())
        addInlineField("Text Channels", textChannelCache.size().toString())
        addInlineField("Voice Channels", voiceChannels.size.toString())

    }).setFooter("Guild creation date: ${timeCreated.format(DateTimeFormatter.RFC_1123_DATE_TIME)}", iconUrl).build()
}
