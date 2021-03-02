package net.carolinaphoenix.discordrolesbot;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DBEmojiRole {
	
	@Id
	@Column(columnDefinition = "VARCHAR(65536)", nullable = false)
	private String id;
	
	@Column(nullable = false)
	private String emoji;
	
	@Column(nullable = false)
	private long roleID;
	
	@Column(nullable = false)
	private long guildID;
	
	@Column(nullable = false)
	private long timestamp;

	public DBEmojiRole(String emoji, long roleID, long guildID) {
		this.id = guildID + ":" + emoji;
		this.emoji = emoji;
		this.roleID = roleID;
		this.guildID = guildID;
		this.timestamp = System.currentTimeMillis();
	}

	@SuppressWarnings("unused")
	private DBEmojiRole() {}

	public String getId() {
		return id;
	}

	public String getEmoji() {
		return emoji;
	}

	public long getRoleID() {
		return roleID;
	}

	public long getGuildID() {
		return guildID;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
