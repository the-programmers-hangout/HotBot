package me.aberrantfox.aegeus.services

import net.dv8tion.jda.core.entities.Message
import java.util.*


data class FillableQueue<T>(val sizeLimit: Int = 50) {
    private val queue: Queue<T> = LinkedList()

    fun add(element: T) {
        if(queue.size == sizeLimit) {
            queue.remove()
        }
        queue.add(element)
    }

    fun getQueue() = this.queue
}

class QueueMap<in K,V> {
    private val map: WeakHashMap<K, FillableQueue<V>> = WeakHashMap()

    fun put(key: K, value: V): V {
        if (!map.containsKey(key)) {
            val queue = FillableQueue<V>()
            queue.add(value)

            map.put(key, FillableQueue(20))
            return value
        }

        map[key]?.add(value)
        return value
    }

    fun getIfExists(key: K): FillableQueue<V>? {
        if(map.containsKey(key)) {
            return map[key]
        }

        return null
    }
}

enum class InvocationType {
    Dm, Guild
}

object LastCommands {
    val queue: FillableQueue<Pair<InvocationType, Message>> = FillableQueue()
}

object GlobalSelfDeletions {
    val queue: FillableQueue<Message> = FillableQueue()
}