package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.utility.types.PersistentSet
import org.amshove.kluent.shouldContainAll
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.io.File
import java.nio.file.Files


object PersistentSetSpec : Spek({
    describe("Persistent set") {
        val location = "test-file"
        val testItems = arrayOf("item1", "item2", "item3")

        beforeGroup {
            val set = PersistentSet(location)
            testItems.forEach { set.add(it) }
        }

        given("file with some items") {
            it("should load them") {
                val set = PersistentSet(location)
                set shouldContainAll testItems
            }
        }

        afterGroup {
            val permsFile = File(location)
            Files.deleteIfExists(permsFile.toPath())
        }
    }
})