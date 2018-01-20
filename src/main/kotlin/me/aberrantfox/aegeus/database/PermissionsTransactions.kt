package me.aberrantfox.aegeus.database

import me.aberrantfox.aegeus.permissions.PermissionManager
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


fun savePermissions(manager: PermissionManager) =
    transaction {
        //TODO: change this to just update the rows...
        SchemaUtils.drop(CommandPermissions)
        SchemaUtils.create(CommandPermissions)

        manager.map.entries.forEach { entry ->
            entry.value.forEach { cmd ->
                CommandPermissions.insert {
                    it[roleID] = entry.key
                    it[commandName] = cmd
                }
            }
        }
    }

fun loadUpManager(manager: PermissionManager) =
    transaction {
        CommandPermissions.selectAll().map {
            manager.addPermission(
                it[CommandPermissions.roleID],
                it[CommandPermissions.commandName])
        }
    }