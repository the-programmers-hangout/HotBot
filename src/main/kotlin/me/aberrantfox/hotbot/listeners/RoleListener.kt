package me.aberrantfox.hotbot.listeners

import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.events.role.update.RoleUpdateNameEvent
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.commandframework.commands.RankContainer

class RoleListener(val configuration: Configuration) : ListenerAdapter() {
    override fun onRoleUpdateName(event: RoleUpdateNameEvent) {
        val oldName = event.oldName
        val newName = event.role.name

        // Update grantable role
        if (RankContainer.canUse(oldName)) {
            RankContainer.remove(oldName)
            RankContainer.add(newName)
        }
    }
}