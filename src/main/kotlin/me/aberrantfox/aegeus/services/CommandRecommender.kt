package me.aberrantfox.aegeus.services

import org.apache.commons.text.similarity.LevenshteinDistance


object CommandRecommender {
    private val calc = LevenshteinDistance()
    private val possibilities: MutableList<String> = ArrayList()

    fun recommendCommand(input: String) = possibilities.minBy { calc.apply(input, it) }

    fun addPossibility(item: String) = possibilities.add(item)

    fun addAll(list: List<String>) = possibilities.addAll(list)

    fun removePossibility(item: String) = possibilities.removeAll { it == item.toLowerCase() }
}

