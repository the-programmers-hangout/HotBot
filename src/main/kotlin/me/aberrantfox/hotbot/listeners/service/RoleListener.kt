package me.aberrantfox.hotbot.listeners.service

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.RankContainer
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent

class RoleListener(val configuration: Configuration, val rankContainer: RankContainer) {
    @Subscribe
    fun onRoleUpdateName(event: RoleUpdateNameEvent) {
        val oldName = event.oldName
        val newName = event.role.name

        if (rankContainer.canUse(oldName)) {
            rankContainer.remove(oldName)
            rankContainer.add(newName)
        }
    }
}