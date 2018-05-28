package me.aberrantfox.hotbot.commands.utility

import khttp.get
import me.aberrantfox.hotbot.utility.randomInt
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.internal.command.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import java.net.URLEncoder

@CommandSet
fun xkcdCommands() = commands {
    command("xkcd") {
        expect(arg(IntegerArg, true, { randomInt(1, getAmount()) }))
        execute {
            val target = it.args.component1() as Int

            val link =
                    if (target <= getAmount() && target > 0) {
                        produceURL(target)
                    } else {
                        "Please enter a valid comic number between 1 and ${getAmount()}"
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
        expect(SentenceArg)
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