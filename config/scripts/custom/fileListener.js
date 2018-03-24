(function(){
	const fileListener = new EventListener()
	{
		onGuildMessageReceived: (event) => {
			const msg = event.message;

			if (msg.author.isBot()) { 
				return 
			}
			
			if (config.serverInformation.ownerID == msg.member.user.getId()) { 
				return 
			}

			if(event.member.isOwner()) {
				return
			}
			
			if(msg.attachments.isEmpty()) { 
				return 
			}

			const containsIllegalAttachment = msg.attachments.stream().anyMatch((attachment) => notAllowed(attachment.fileName))
			const user = event.author.asMention
			if(containsIllegalAttachment) {
				
				const fileNames = []
				msg.attachments.stream().forEach(attachment => fileNames.push(attachment.fileName))
				
				const channel = event.channel.asMention
				 
				msg.delete().queue()
				container.log.warning("${user} attempted to send the illegal file(s) ${fileNames} in ${channel}")
				const userResponse = "Please don't send that file type here ${user} use a service like https://hastebin.com"
				event.channel.sendMessage(userResponse).queue()
			}
		}
	}

	const regex = /^.*\.(jpg|jpeg|gif|png|mp4|webm)$/i

	function notAllowed(fileName) {	
		return (!regex.test(fileName))
	}

	jda.addEventListener(fileListener)
})()
