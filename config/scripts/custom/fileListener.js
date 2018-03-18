(function(){
	const fileListener = new EventListener()
	{
		onGuildMessageReceived: (event) => {
			const msg = event.message

			if (msg.author.isBot()) { 
				return 
			}
			
			// if (config.serverInformation.ownerID == msg.member.user.getId()) { 
			// 	return 
			// }

			// if(event.member.isOwner()) {
			// 	return
			// }
			
			if(msg.attachments.isEmpty()) { 
				return 
			}

			const containsIllegalAttachment = msg.attachments.stream().anyMatch((attachment) => notAllowed(attachment.fileName))
			const mention = event.author.asMention

			if(containsIllegalAttachment) {
				msg.delete().queue()
				container.log.warning("${mention} attempted to send an illegal file")
				const userResponse = "Please don't send that file type here ${mention} use a service like https://hastebin.com"
				event.channel.sendMessage(userResponse).queue()
			}
		}
	}

	

	function notAllowed(fileName) {
		const regex = /^.*\.(jpg|jpeg|gif|png|mp4|webm)$/i
		return (!regex.test(fileName))
	}

	jda.addEventListener(fileListener)
})()
