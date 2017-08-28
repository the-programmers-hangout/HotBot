package me.aberrantfox.aegus

import org.amshove.kluent.shouldBeTrue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object SanitySpec: Spek({
    describe("sanity") {
        it("should be sane") {
            true.shouldBeTrue()
        }   
    }
})