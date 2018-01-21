package me.aberrantfox.hotbot.services


class LimitedList<T>(val limit: Int) : ArrayList<T>() {
    override fun add(element: T): Boolean {
        if(size == limit) {
            this.removeAt(size - 1)
        }
        return super.add(element)
    }
}