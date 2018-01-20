package me.aberrantfox.hotbot.services


data class PersonalQueue(var why: String = "Raid", val who: HashSet<String> = HashSet())

typealias UserID = String

object BanQueue {
    private val banQueue = mutableMapOf<UserID, PersonalQueue>()

    fun queueBan(moderatorID: UserID, targetID: UserID) {
        if( !(banQueue.containsKey(moderatorID)) ) banQueue[moderatorID] = PersonalQueue()

        banQueue[moderatorID]?.who?.add(targetID)
    }

    fun removeBan(moderatorID: UserID, targetID: UserID) = banQueue[moderatorID]?.who?.remove(targetID)

    fun getBans(moderatorID: UserID): HashSet<String>? = banQueue[moderatorID]?.who

    fun clearBans(moderatorID: UserID) = banQueue[moderatorID]?.who?.clear()

    fun setReason(moderatorID: UserID, reason: String) {
        banQueue[moderatorID]?.why = reason
    }

    fun getReason(moderatorID: UserID) = banQueue[moderatorID]?.why
}