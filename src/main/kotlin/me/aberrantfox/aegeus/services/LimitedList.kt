package me.aberrantfox.aegeus.services


class LimitedList<T>(val limit: Int) : ArrayList<T>() {
    override fun add(element: T): Boolean {
        println("Size: $size")
        println("Limit: $limit")
        if(size == limit) {
            this.removeAt(size - 1)
        }
        return super.add(element)
    }
}