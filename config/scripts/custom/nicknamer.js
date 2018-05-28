registerCommand({
    name: "nick",
    expect: [UserArg, SentenceArg],
    execute: (event) => {
        const guild = jda.getGuildById(config.serverInformation.guildid)
        const target = guild.getMember(event.args[0])
        const nick = event.args[1]
        const guildController = guild.getController()
        guildController.setNickname(target, nick).queue()
    },
    help: {
        name: "Nick",
        description: "Set a user's nickname",
        category: "Fun",
        structure: "++nick user nickname",
        example: "@moe moederator"
    }
})
