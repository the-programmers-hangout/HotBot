package me.aberrantfox.hotbot.services


class LimitedList<T>(val limit: Int) : ArrayList<T>() {
    override fun add(element: T): Boolean {
        if(size == limit) {
            this.removeAt(0)
        }
        return super.add(element)
    }
}