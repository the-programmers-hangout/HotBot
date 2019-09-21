package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.listeners.antispam.InviteWhitelist

class RecentInviteService(val whitelist: InviteWhitelist) {
    val cache = WeightTracker(6)

    fun value(id: String) = cache.map[id]!!

    fun trimmedMessage(data: String): String {
        var str = data
        whitelist.set.forEach { str = str.replace(it, "") }

        return str
    }
}