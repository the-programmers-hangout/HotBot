registerCommand({
    name: "whatpfp",
    description: "Returns the reverse image url of a users profile picture.",
    category: "moderation",
    expect: [UserArg],
    execute: (event) => {
        const userPfp = event.args[0].effectiveAvatarUrl
        const reverseUrl = "https://www.google.com/searchbyimage?image_url="
        const embed = new EmbedBuilder()
                           .setTitle("${event.args[0].getName()}'s pfp")
                           .setImage(userPfp)
                           .appendDescription("[Reverse Search](${reverseUrl}${userPfp})")
                           .build()
        event.respond(embed)
    }
})
