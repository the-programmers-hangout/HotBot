package me.aberrantfox.hotbot.security

import me.aberrantfox.hotbot.services.DateTracker
import me.aberrantfox.hotbot.services.secondUnit
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotContainAny
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.joda.time.DateTime

object NewPlayerTrackerSpec : Spek({
    describe("New player tracker") {
        val timeLimit = 5 * secondUnit
        val newPlayers = DateTracker(5, secondUnit)

        beforeEachTest {
            newPlayers.clear()
        }

        it("should track on player join") {
            newPlayers.put("a", DateTime.now())
            newPlayers.put("b", DateTime.now())
            newPlayers.put("c", DateTime.now())
            newPlayers.put("d", DateTime.now())

            newPlayers.pastMins(1).keys shouldContainAll arrayOf("a", "b", "c", "d")
        }

        it("should not track players after time out") {
            newPlayers.put("e", DateTime.now())
            newPlayers.put("f", DateTime.now())
            newPlayers.put("g", DateTime.now())
            Thread.sleep(timeLimit.toLong() + 100)
            newPlayers.keyList() shouldNotContainAny arrayOf("e", "f", "g")
        }

        it("should not list players out of scope") {
            newPlayers.put("a", DateTime.now())
            newPlayers.pastMins(0).shouldBeEmpty()
        }
    }
})

