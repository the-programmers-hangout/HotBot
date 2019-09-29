package me.aberrantfox.hotbot.extensions

import net.dv8tion.jda.api.entities.MessageChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO


fun MessageChannel.sendImageToChannel(url: String, name: String, failure: String) {
    try {
        val urlObject = URL(url)
        val image = ImageIO.read(urlObject)
        val os = ByteArrayOutputStream()

        ImageIO.write(image, "png", os)

        val ims = ByteArrayInputStream(os.toByteArray())
        sendFile(ims, name).queue()
    } catch (e: IOException) {
        sendMessage(failure).queue()
    }
}

