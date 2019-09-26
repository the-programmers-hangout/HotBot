package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.database.setupDatabaseSchema
import me.aberrantfox.kjdautils.api.annotation.Service

@Service
class DatabaseService(config: Configuration) {
    init {
        setupDatabaseSchema(config)
    }
}