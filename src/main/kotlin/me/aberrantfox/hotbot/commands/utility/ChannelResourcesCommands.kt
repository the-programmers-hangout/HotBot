package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.database.*
import me.aberrantfox.kjdautils.api.dsl.CommandEvent
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import java.awt.Color
import net.dv8tion.jda.core.entities.MessageChannel as Channel

@CommandSet("resources")
fun channelResourceCommands() = commands {
    command("addresource") {
        expect(SentenceArg)
        execute {
            val input = it.args[0] as String
            val split = input.split("|")

            if (split.size != 2) {
                it.respond("Must specify both section and info. example: `section|info`")
                return@execute
            }

            val (section, info) = split

            if (section.length < 3 || info.length < 3) {
                it.respond("Minimum length for `section` and `info` is 3 characters.")
                return@execute
            }

            saveChannelResource(it.channel.id, section, info)
            it.respond("Added resource: `$info` to: `$section`")
        }
    }

    command("listresources") {
        execute {
            handleListResources(it)
        }
    }

    command("listresourcesids") {
        execute {
            handleListResources(it, true)
        }
    }

    command("removeresource") {
        expect(SplitterArg)
        execute {
            val ids: List<Int>

            try {
                ids = (it.args[0] as List<*>).map { it.toString().toInt() }
            } catch (e: NumberFormatException) {
                it.respond("Invalid channel resource id(s) provided.")
                return@execute
            }

            val invalidIds: List<String>
            val foundIds = fetchChannelResourcesByIds(it.channel.id, ids)
            if (ids.size != foundIds.size) {
                invalidIds = ids.filter { !foundIds.contains(it) }.map { it.toString() }
                val list = invalidIds.joinToString(", ")
                it.respond("No such channel resource id(s): $list")
                return@execute
            }

            removeChannelResources(it.channel.id, foundIds)
            it.respond("Removed ${foundIds.size} resource entries.")
        }
    }

    command("resetresources") {
        execute {
            val resourcesCount = fetchChannelResources(it.channel.id).size
            val channelMention = "<#${it.channel.id}>"

            if (resourcesCount == 0) {
                it.respond("Channel $channelMention has no resources.")
                return@execute
            }

            removeAllChannelResources(it.channel.id)
            it.respond("All channel resources removed ($resourcesCount).")
        }
    }
}

private fun handleListResources(ce: CommandEvent, showIds: Boolean = false) {
    val resources = fetchChannelResources(ce.channel.id, showIds)

    if (resources.isEmpty()) {
        ce.respond("No resources found for <#${ce.channel.id}>.")
        return
    }

    ce.respond(buildResourcesEmbed(ce.channel, resources))
}

private fun buildResourcesEmbed(channel: Channel, resources: Map<String, ResourceSection>) =
    embed {
        setColor(Color.decode("#177BBC"))
        title("${channel.name.toUpperCase()} RESOURCES")
        description("Current channel resources:\n\n")

        resources.forEach { _, rs ->
            val sb = StringBuilder()

            rs.items.forEach { info -> sb.append("$info\n") }

            field {
                name = rs.section
                value = sb.toString()
                inline = false
            }
        }
    }
