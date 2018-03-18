const echoListener = new EventListener() {
    onGuildMessageReceived: (event) => {
        if(event.author.isBot()) {
            return;
        }

        event.channel.sendMessage("Hi there, you said: ${event.message.contentRaw}").queue()
    }
}

jda.addEventListener(echoListener)