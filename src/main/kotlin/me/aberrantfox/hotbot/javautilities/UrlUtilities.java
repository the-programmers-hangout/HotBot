package me.aberrantfox.hotbot.javautilities;


import net.dv8tion.jda.api.entities.MessageChannel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

public class UrlUtilities {
    public static void sendImageToChannel(String url, String name, String failure, MessageChannel channel) {
        try {
            final URL urlObject  = new URL(url);
            final BufferedImage image = ImageIO.read(urlObject);
            final ByteArrayOutputStream os = new ByteArrayOutputStream();

            ImageIO.write(image, "png", os);

            final InputStream is = new ByteArrayInputStream(os.toByteArray());
            channel.sendFile(is,name).queue();
        } catch (IOException e) {
            channel.sendMessage(failure).queue();
        }
    }
}
