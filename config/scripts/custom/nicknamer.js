registerCommand({
    name: "nick",
    expect: [argType.User, argType.Sentence],
    execute: (event) => {
        const target = event.guild.getMember(event.args[0])
        const nick = event.args[1]
        const guildController = event.guild.getController()
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
