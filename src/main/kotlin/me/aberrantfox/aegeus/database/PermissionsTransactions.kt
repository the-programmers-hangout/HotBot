package me.aberrantfox.aegeus.database

import me.aberrantfox.aegeus.permissions.PermissionManager
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


fun savePermissions(manager: PermissionManager) =
    transaction {
        manager.map.keys.forEach { r ->
            manager.map[r]!!.forEach { c ->
                CommandPermissions.insert {
                    it[roleID] = r
                    it[commandName] = c
                }
            }
        }
    }

fun loadUpManager(manager: PermissionManager) =
    transaction {
        CommandPermissions.selectAll().map {
            manager.addPermission(
                it[CommandPermissions.commandName],
                it[CommandPermissions.roleID])
        }
    }