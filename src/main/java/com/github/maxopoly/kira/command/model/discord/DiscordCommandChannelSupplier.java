package com.github.maxopoly.kira.command.model.discord;

import net.civmc.kira.Kira;
import com.github.maxopoly.kira.user.KiraUser;
import com.github.maxopoly.kira.util.DiscordMessageSender;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class DiscordCommandChannelSupplier extends DiscordCommandSupplier {

	private long channelID;
	private long guildID;

	public DiscordCommandChannelSupplier(KiraUser user, long guildID, long channelID) {
		super(user);
		this.channelID = channelID;
		this.guildID = guildID;
	}

	@Override
	public long getChannelID() {
		return channelID;
	}

	@Override
	public void reportBack(String msg) {
		JDA jda = Kira.Companion.getInstance().getJda();
		Guild guild = jda.getGuildById(guildID);
		if (guild == null) {
			return;
		}
		TextChannel channel = guild.getTextChannelById(channelID);
		if (channel == null) {
			return;
		}
		DiscordMessageSender.sendTextChannelMessage(user, channel, msg);
	}

}
