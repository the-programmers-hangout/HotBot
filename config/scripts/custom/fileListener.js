(function(){
	var fileListener = new EventListener()
	{
		onGuildMessageReceived: function (event) {

			var msg = event.message;
			
			if (msg.author.isBot()) { return; }
			if (msg.member.isOwner()) { return; }
			if(msg.attachments.length <=0) { return; }
			var deletedMessage = false;
			
			for (var i = 0; i < msg.attachments.length; i++) {
				var attachment = msg.attachments[i];
				if (notAllowed(attachment.fileName)) {
					deletedMessage = true;
					msg.delete().queue();
					container.log.warning(msg.author.asMention + " just sent the file " +
					attachment.getFileName() + "\n" + attachment.getUrl())
				}
			}

			if (deletedMessage) {
				event.channel.sendMessage(
					"Please don't send that file type here " +
					msg.author.asMention + " use a service like " +
					"https://hastebin.com"
				).queue()
			}
		}
	};

	function notAllowed(fileName) {
		var regex = /^.*\.(jpg|jpeg|gif|png|mp4|webm)$/i;
		return (!regex.test(fileName))
	}

	jda.addEventListener(fileListener);
})();
