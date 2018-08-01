package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.database.ResourceSection
import me.aberrantfox.hotbot.database.fetchChannelResources
import me.aberrantfox.hotbot.database.removeAllChannelResources
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import java.awt.Color

class ChannelDeleteListener(val log: BotLogger) {
    @Subscribe
    fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        val resources = fetchChannelResources(event.channel.id, true)
        if (resources.isEmpty()) return

        removeAllChannelResources(event.channel.id)
        log.alert(buildDeleteResourcesEmbed(event.channel, resources))
    }
}

private fun buildDeleteResourcesEmbed(channel: MessageChannel, resources: Map<String, ResourceSection>) =
        embed {
            setColor(Color.RED)
            title("Deleted ${channel.name}'s resources")
            description("Deleted channel resources:")

            resources.forEach { _, section ->
                field {
                    name = section.section
                    value = section.items.joinToString("\n")
                    inline = false
                }
            }
        }
