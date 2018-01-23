package me.aberrantfox.hotbot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class ResourceSection(val section: String, val items: MutableList<String>)

fun saveChannelResource(channel: String, section: String, info: String) =
        transaction {
            ChannelResources.insert {
                it[ChannelResources.channel] = channel
                it[ChannelResources.section] = section
                it[ChannelResources.info] = info
            }
        }

fun removeChannelResources(channel: String, ids: List<Int>) =
        transaction {
            ChannelResources.deleteWhere {
                Op.build {
                    (ChannelResources.channel eq channel) and
                    (ChannelResources.id inList ids)
                }
            }
        }

fun removeAllChannelResources(channel: String) =
        transaction {
            ChannelResources.deleteWhere {
                Op.build { ChannelResources.channel eq channel }
            }
        }

fun fetchChannelResources(channel: String, showIds: Boolean = false) =
        transaction {
            val sections: MutableMap<String, ResourceSection> = mutableMapOf()

            ChannelResources.select {
                Op.build { ChannelResources.channel eq channel }
            }.forEach {
                val name = it[ChannelResources.section]
                val key = name.toUpperCase()
                var info = it[ChannelResources.info]

                if (showIds) {
                    val id = it[ChannelResources.id]
                    info = "[#$id]: $info"
                }

                if (!sections.containsKey(key)) {
                    sections[key] = ResourceSection(name, mutableListOf(info))
                } else {
                    sections[key]?.items?.add(info)
                }
            }

            sections
        }

fun fetchChannelResourcesByIds(channel: String, ids: List<Int>) =
        transaction {
            ChannelResources.select {
                Op.build {
                    (ChannelResources.id inList ids) and
                    (ChannelResources.channel eq channel)
                }
            }
            .map { it[ChannelResources.id] }
            .toList()
        }

