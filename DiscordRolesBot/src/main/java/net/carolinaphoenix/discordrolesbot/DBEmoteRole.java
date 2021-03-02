package net.carolinaphoenix.discordrolesbot;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class DBEmoteRole {
	
	@Id
	@Column(columnDefinition = "VARCHAR(65536)", nullable = false)
	private String id;
	
	@Column(nullable = false)
	private long emoteID;
	
	@Column(nullable = false)
	private long roleID;
	
	@Column(nullable = false)
	private long guildID;
	
	@Column(nullable = false)
	private long timestamp;

	public DBEmoteRole(long emoteID, long roleID, long guildID) {
		this.id = guildID + ":" + emoteID;
		this.emoteID = emoteID;
		this.roleID = roleID;
		this.guildID = guildID;
		this.timestamp = System.currentTimeMillis();
	}

	@SuppressWarnings("unused")
	private DBEmoteRole() {}

	public String getId() {
		return id;
	}

	public long getEmoteID() {
		return emoteID;
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
