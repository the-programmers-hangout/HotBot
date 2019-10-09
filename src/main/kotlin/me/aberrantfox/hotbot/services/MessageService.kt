package me.aberrantfox.hotbot.services


import me.aberrantfox.kjdautils.api.annotation.Data


@Data("config/responses.json")
data class Messages(var onJoin: ArrayList<String> = ArrayList(),
                    var names: ArrayList<String> = ArrayList(),
                    var serverDescription: String = "Insert Server Description here!",
                    var botDescription: String = "A neat bot for administrating servers.",
                    var gagResponse: String = "You've been muted temporarily so that a mod can handle something.",
                    var welcomeDescription: String = "This will be displayed underneath the greeting.",
                    var karmaMessage: String = "Well done %mention%, you have earned a karma point.")
