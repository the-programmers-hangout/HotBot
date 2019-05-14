package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.utility.types.LimitedList
import org.amshove.kluent.should
import org.amshove.kluent.shouldContainAll
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on


object LimitedListSpec : Spek({
    describe("Limited List") {
        val list = LimitedList<Int>(3)
        on("adding items with a limit of 3") {
            it("should retain only the latest three") {
                list.add(1)
                list.add(2)
                list.add(3)
                list.add(4)
                list.add(5)

                list shouldContainAll arrayOf(3, 4, 5)
                list should { size == 3 }
            }
        }
    }
})