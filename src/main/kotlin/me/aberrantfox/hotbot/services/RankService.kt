package me.aberrantfox.hotbot.services

import me.aberrantfox.kjdautils.api.annotation.Data
import me.aberrantfox.kjdautils.internal.di.PersistenceService

@Data("config/rankconfig.json")
data class RankConfiguration(val acceptableRanks: HashSet<String> = HashSet())

class RankContainer(val config: RankConfiguration, val persistenceService: PersistenceService) {
    fun canUse(role: String) = config.acceptableRanks.contains(role.toLowerCase())

    fun add(role: String) {
        config.acceptableRanks.add(role.toLowerCase())
        persistenceService.save(config)
    }

    fun remove(role: String) {
        config.acceptableRanks.remove(role.toLowerCase())
        persistenceService.save(config)
    }

    fun stringList() =
            if (config.acceptableRanks.isNotEmpty()) {
                config.acceptableRanks.reduce { a, b -> "$a, $b" }
            } else {
                "None."
            }
}