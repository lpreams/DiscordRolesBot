/*
 * Copyright 2021 lpreams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
