package com.github.maxopoly.kira.rabbit.input;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.json.JSONObject;

import net.civmc.kira.Kira;
import com.github.maxopoly.kira.rabbit.RabbitInputSupplier;
import com.github.maxopoly.kira.util.DiscordMessageSender;

import net.dv8tion.jda.api.entities.TextChannel;

public class ConsoleForwardMessage extends RabbitMessage {
	
	private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("[HH:mm:ss]");

	private Map<String, Long> forwards;
	
	public ConsoleForwardMessage(Map <String, Long> forwards) {
		super("consolelog");
		this.forwards = forwards;
	}

	@Override
	public void handle(JSONObject argument, RabbitInputSupplier supplier) {
		String key = argument.getString("consolekey");
		String msg = argument.getString("message");
		Long channelIdObj = forwards.get(key);
		if (channelIdObj == null) {
			Kira.Companion.getInstance().getLogger().warn("Unknown console key " + key);
			return;
		}
		TextChannel channel = Kira.Companion.getInstance().getGuild().getTextChannelById(channelIdObj);
		if (channel == null ) {
			Kira.Companion.getInstance().getLogger().warn("Unknown channel id " + channelIdObj);
			return;
		}
		String cleanedUp = String.format("**[%s] **`%s`", timeFormat.format(LocalTime.now()),
				msg.replaceAll("[^\\p{InBasic_Latin}\\p{InLatin-1Supplement}]", ""));
		DiscordMessageSender.sendTextChannelMessage(null, channel, cleanedUp);
	}

}
