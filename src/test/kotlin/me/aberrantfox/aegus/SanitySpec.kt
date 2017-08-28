package me.aberrantfox.aegus

import org.amshove.kluent.shouldBeTrue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

//Need to add @Runwith(JUnitPlatform::class) when I figure out where the hell that class is dependency wise...
object SanitySpec: Spek ({
    describe("sanity") {
        it("should be sane") {
            true.shouldBeTrue()
        }   
    }
})