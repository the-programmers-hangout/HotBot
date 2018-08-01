registerCommand({
    name: "nick",
    description: "Set a user's nickname",
    category: "moderation",
    expect: [LowerUserArg, SentenceArg],
    execute: (event) => {
        const guild = jda.getGuildById(config.serverInformation.guildid)
        const target = guild.getMember(event.args[0])
        const nick = event.args[1]
        const guildController = guild.getController()
        guildController.setNickname(target, nick).queue()
    }
})
