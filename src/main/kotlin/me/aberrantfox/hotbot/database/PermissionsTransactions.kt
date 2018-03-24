package me.aberrantfox.hotbot.database

import me.aberrantfox.hotbot.permissions.CommandName
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.permissions.RoleID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


fun savePermissions(manager: PermissionManager) =
        transaction {
            val rolePermissionSets = manager.map.entries

            rolePermissionSets.forEach { pair ->
                val role = pair.key
                val assignedPermissions = pair.value

                assignedPermissions.forEach { setPermission(it, role) }
            }
        }

fun setPermission(name: CommandName, roleID: RoleID) = if (hasPermission(name)) updatePermission(roleID, name) else insertPermission(roleID, name)

private fun updatePermission(roleID: RoleID, name: CommandName) =
        transaction {
            CommandPermissions.update({ CommandPermissions.commandName eq name }) {
                it[CommandPermissions.roleID] = roleID
            }
            Unit
        }

private fun insertPermission(roleID: RoleID, name: CommandName) =
        transaction {
            CommandPermissions.insert {
                it[CommandPermissions.roleID] = roleID
                it[commandName] = name
            }
            Unit
        }

fun hasPermission(name: String) =
        transaction {
            CommandPermissions.select {
                Op.build {
                    CommandPermissions.commandName eq name
                }
            }.count() == 1
        }

fun loadUpManager(manager: PermissionManager) =
        transaction {
            CommandPermissions.selectAll().map {
                manager.addPermission(
                        it[CommandPermissions.roleID],
                        it[CommandPermissions.commandName])
            }
        }