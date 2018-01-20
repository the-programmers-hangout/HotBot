package me.aberrantfox.hotbot.extensions


fun String.isSingleCharacter() = "^(.)\\1+\$".toRegex().matches(this)

fun String.isEmojiOnly() =
    if(this.contains(" ")) {
        val split = this.split(" ")
        split.all { it.wordIsEmoji() }
    } else {
        this.wordIsEmoji()
    }


fun String.wordIsEmoji() = this.startsWith(":") && this.endsWith(":")
    || "([\\\\u20a0-\\\\u32ff\\\\ud83c\\\\udc00-\\\\ud83d\\\\udeff\\\\udbb9\\\\udce5-\\\\udbb9\\\\udcee])".toRegex().matches(this)