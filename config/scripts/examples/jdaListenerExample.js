var echoListener = new EventListener() {
    onGuildMessageReceived: function(event) {
        event.channel.sendMessage("Hi there, you said: " + event.message.contentRaw).queue()
    }
}

jda.addEventListener(echoListener)