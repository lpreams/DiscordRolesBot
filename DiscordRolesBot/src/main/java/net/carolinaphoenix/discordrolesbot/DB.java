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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DB {

	private final static Logger LOGGER = LoggerFactory.getLogger(new Object() {}.getClass().getEnclosingClass());
	
	/*********************************************************************************************************/

	/**
	 * Will override existing without warning
	 * @param emoteID
	 * @param roleID
	 * @throws DBException on rollback
	 */
	public static void addEmoteRole(long emoteID, long roleID, long guildID) throws DBException {
		LOGGER.info("Opening session");
		Session session = openSession();
		try {
			DBEmoteRole emoteRole = new DBEmoteRole(emoteID, roleID, guildID);
			session.beginTransaction();
			try {
				session.merge(emoteRole);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException(e);
			}
			
		} finally {
			closeSession(session);
			LOGGER.info("Closed session");
		}
	}
	
	/**
	 * Throws DBException on null result
	 * @param emoteID
	 * @return
	 * @throws DBException on null result
	 */
	public static long getEmoteRole(long emoteID, long guildID) throws DBException {
		Session session = openSession();
		try {
			DBEmoteRole emoteRole = session.get(DBEmoteRole.class, guildID + ":" + emoteID);
			if (emoteRole == null) throw new DBException("EmoteRole not found: " + emoteID);
			return emoteRole.getRoleID();
		} finally {
			closeSession(session);
		}
	}
	
	/**
	 * Will override existing without warning
	 * @param emoteID
	 * @param roleID
	 * @throws DBException on rollback
	 */
	public static void addEmojiRole(String emoji, long roleID, long guildID) throws DBException {
		Session session = openSession();
		try {
			
			DBEmojiRole emojiRole = new DBEmojiRole(emoji, roleID, guildID);
			
			session.beginTransaction();
			try {
				session.merge(emojiRole);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException(e);
			}
			
		} finally {
			closeSession(session);
		}
	}
	
	/**
	 * Throws DBException on null result
	 * @param emoteID
	 * @return
	 * @throws DBException on null result
	 */
	public static long getEmojiRole(String emoji, long guildID) throws DBException {
		Session session = openSession();
		try {
			DBEmojiRole emojiRole = session.get(DBEmojiRole.class, guildID + ":" + emoji);
			if (emojiRole == null) throw new DBException("EmoteRole not found: " + emoji);
			return emojiRole.getRoleID();
		} finally {
			closeSession(session);
		}
	}
	
	public static class FlatRole {
		public final boolean isEmoji, isEmote;
		public final Long emoteID;
		public final String emoji;
		public final long roleID;
		public final long timestamp;
		private FlatRole(DBEmojiRole role) {
			this.isEmoji = true;
			this.isEmote = false;
			this.emoteID = null;
			this.emoji = role.getEmoji();
			this.roleID = role.getRoleID();
			this.timestamp = role.getTimestamp();
		}
		private FlatRole(DBEmoteRole role) {
			this.isEmoji = false;
			this.isEmote = true;
			this.emoteID = role.getEmoteID();
			this.emoji = null;
			this.roleID = role.getRoleID();
			this.timestamp = role.getTimestamp();
		}
	}
	
	public static List<FlatRole> getAllRoles(long guildID) {
		Session session = openSession();
		try {
			List<FlatRole> list = new ArrayList<>();
			list.addAll(session.createQuery("from DBEmojiRole r where r.guildID=" + guildID, DBEmojiRole.class).stream().map(FlatRole::new).collect(Collectors.toList()));
			list.addAll(session.createQuery("from DBEmoteRole r where r.guildID=" + guildID, DBEmoteRole.class).stream().map(FlatRole::new).collect(Collectors.toList()));
			Collections.sort(list, Comparator.comparing(r->r.timestamp));
			return list;
		} finally {
			closeSession(session);
		}
	}
	
	public static void addRoleMenu(long messageID, long outputID, long channelID, long guildID) throws DBException {
		Session session = openSession();
		try {
			DBRoleMenu menu = new DBRoleMenu(messageID, outputID, channelID, guildID);
			session.beginTransaction();
			try {
				session.merge(menu);
				session.getTransaction().commit();
			} catch (Exception e) {
				session.getTransaction().rollback();
				throw new DBException(e);
			}
		} finally {
			closeSession(session);
		}
	}
	
	public static class FlatRoleMenu {
		public final long guildID;
		public final long channelID;
		public final long messageID;
		public final long outputID;
		private FlatRoleMenu(DBRoleMenu menu) {
			this.guildID = menu.getGuildID();
			this.channelID = menu.getChannelID();
			this.messageID = menu.getMessageID();
			this.outputID = menu.getOutputID();
		}
	}
	
	public static FlatRoleMenu getRoleMenus(long guildID) {
		Session session = openSession();
		try {
			return new FlatRoleMenu(session.get(DBRoleMenu.class, guildID));
		} finally {
			closeSession(session);
		}
	}
	
	public static List<FlatRoleMenu> getAllRoleMenus() {
		Session session = openSession();
		try {
			return session.createQuery("from DBRoleMenu m", DBRoleMenu.class).stream().map(FlatRoleMenu::new).collect(Collectors.toList());
		} finally {
			closeSession(session);
		}
	}
	
	/*********************************************************************************************************/

	private static SessionFactory sessionFactory;
	
	// used to ensure no sessions are open when sessionfactory is closed
	private static ReentrantReadWriteLock sessionLock = new ReentrantReadWriteLock();

	static {

		Configuration configuration = new Configuration();

		configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
		configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		configuration.setProperty("hibernate.connection.url", "jdbc:h2:"+System.getProperty("user.home")+"/.discord_roles_bot");

		configuration.setProperty("hibernate.show_sql", "false");
		configuration.setProperty("hibernate.hbm2ddl.auto", "update");
		configuration.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");

		configuration.addAnnotatedClass(DBEmojiRole.class);
		configuration.addAnnotatedClass(DBEmoteRole.class);
		configuration.addAnnotatedClass(DBRoleMenu.class);

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
		try {
			sessionFactory = configuration.buildSessionFactory(builder.build());
		} catch (Throwable e) {
			LOGGER.error("Failed to configure database", e);
			e.printStackTrace();
			System.exit(1);
			throw new RuntimeException(e);
		}
	}

	public static void closeSessionFactory() {
		try {
			LOGGER.warn("Trying to close Session factory");
			sessionLock.writeLock().lock();
			LOGGER.warn("Closing Session factory");
			sessionFactory.close();
		} finally {
			sessionLock.writeLock().unlock();
			LOGGER.warn("Session closed");
		}
	}

	public static Session openSession() {
		sessionLock.readLock().lock();
		return sessionFactory.openSession();
	}

	public static void closeSession(Session session) {
		try {
			if (session.isOpen()) session.close();
		} finally {
			sessionLock.readLock().unlock();
		}
	}

	public static class DBException extends Exception {

		private static final long serialVersionUID = -3208322018886376574L;

		public DBException(String message) {
			super(message);
		}

		public DBException(Exception e) {
			super(e);
		}

		public DBException(String message, Exception e) {
			super(message, e);
		}
	}
	
	public static void ping() {}

}
