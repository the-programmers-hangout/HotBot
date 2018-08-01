registerCommand({
    name: "whatpfp",
    description: "Returns the reverse image url of a users profile picture.",
    category: "moderation",
    expect: [UserArg],
    execute: (event) => {
        const userPfp = event.args[0].effectiveAvatarUrl
        const reverseUrl = "https://www.google.com/searchbyimage?&image_url="
        event.respond("<${reverseUrl}${userPfp}>")
    }
})
