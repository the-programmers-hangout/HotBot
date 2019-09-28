package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.services.database.*
import me.aberrantfox.kjdautils.api.annotation.Service

@Service
class DatabaseService(config: Configuration,
                      val bans: BanRecordOperations,
                      val guildLeaves: GuildLeaveHistoryTransactions,
                      val historyCount: HistoryCountTransactions,
                      val infractions: InfractionTransactions,
                      val ignores: IgnoreTransactions,
                      val karma: KarmaOperations,
                      val mutes: MutedMemberTransactions,
                      val notes: NotesTransactions,
                      val reminders: ReminderTransactions,
                      val suggestions: SuggestionTransactions) {
    init {
        setupDatabaseSchema(config)
    }
}