package me.aberrantfox.hotbot.commandframework.commands.utility

import khttp.get
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.utility.randomInt
import java.net.URLEncoder

@CommandSet fun xkcdCommands() = commands {
    command("xkcd") {
        expect(arg(ArgumentType.Integer, true, -1))
        execute {
            val target = it.args.component1() as Int
            val link = if (target == -1) {
                produceURL(randomInt(1, getAmount()))
            } else {
                if (target <= getAmount() && target > 0) {
                    produceURL(target)
                } else {
                    "Please enter a valid comic number between 1 and ${getAmount()}"
                }
            }
            it.respond(link)
        }
    }

    command("xkcd-latest") {
        execute {
            it.respond("https://xkcd.com/" + getAmount())
        }
    }

    command("xkcd-search") {
        expect(ArgumentType.Sentence)
        execute {
            val what = it.args.component1() as String
            it.respond(produceURL(search(what)))
        }
    }
}

private fun search(what: String): Int {
    val comicNumberParseRegex = "(?:\\S+\\s+){2}(\\S+)".toRegex()
    return comicNumberParseRegex.find(
            get("https://relevantxkcd.appspot.com/process?action=xkcd&query=" + URLEncoder.encode(
                    what, "UTF-8")).text)!!.groups[1]?.value!!.toInt()
}

private fun getAmount() =
        get("https://xkcd.com/info.0.json").jsonObject.getInt("num")

private fun produceURL(comicNumber: Int) =
        "http://xkcd.com/$comicNumber/"