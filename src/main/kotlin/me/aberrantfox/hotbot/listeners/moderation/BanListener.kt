package me.aberrantfox.hotbot.listeners.moderation

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.DatabaseService
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import java.util.Timer
import kotlin.concurrent.schedule

class BanListener(val databaseService: DatabaseService) {
    @Subscribe
    fun onGuildBan(event: GuildBanEvent) {
        Timer().schedule(5 * 1000) {
            databaseService.guildLeaves.markLastRecordAsBan(event.user.id, event.guild.id)
        }
    }
}