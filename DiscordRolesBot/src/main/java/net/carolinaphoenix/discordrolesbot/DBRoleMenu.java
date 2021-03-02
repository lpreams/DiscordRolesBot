package net.carolinaphoenix.discordrolesbot;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DBRoleMenu {
	
	@Column(nullable = false)
	private long messageID;
	
	@Column(nullable = false)
	private long outputID;
	
	@Column(nullable = false)
	private long channelID;
	
	@Id
	@Column(nullable = false)
	private long guildID;

	public DBRoleMenu(long messageID, long outputID, long channelID, long guildID) {
		this.messageID = messageID;
		this.outputID = outputID;
		this.channelID = channelID;
		this.guildID = guildID;
	}

	@SuppressWarnings("unused")
	private DBRoleMenu() {}

	public long getMessageID() {
		return messageID;
	}
	
	public long getOutputID() {
		return outputID;
	}
	
	public long getChannelID() {
		return channelID;
	}

	public long getGuildID() {
		return guildID;
	}
	
	
	
}
