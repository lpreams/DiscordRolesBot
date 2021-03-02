package net.carolinaphoenix.discordrolesbot;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.carolinaphoenix.discordrolesbot.DB.DBException;
import net.carolinaphoenix.discordrolesbot.DB.FlatRole;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Discord bot that allows administrators to configure user-settable roles, then prints a pretty menu for your users to use to assign themselves roles
 * 
 * @author lpreams
 * @version 0.1
 */
public class DiscordRolesBot extends ListenerAdapter {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(new Object() {}.getClass().getEnclosingClass());
	
	private static JDA jda;
	
	private ConcurrentHashMap<String, Long> reactMap = new ConcurrentHashMap<>(); // guildID:messageID, roleID
	private ConcurrentHashMap<Long, long[]> roleMenus = new ConcurrentHashMap<>(); // guildID, [channelID, messageID(menu), messageID(output)]
	
	{
		DB.ping();
		DB.getAllRoleMenus().forEach(menu->roleMenus.put(menu.guildID, new long[] {menu.channelID, menu.messageID, menu.outputID}));
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		
		if (event.getAuthor().isBot()) return;
		
		if (!isAdmin(event.getMember())) return; // this bot only responds to messages from Guild Administrators
				
		if (event.getMessage().getContentStripped().toLowerCase().startsWith("?addrole ")) addRole(event);
		else if (event.getMessage().getContentStripped().toLowerCase().startsWith("?rolesmenu")) rolesMenu(event);

	}
	
	private void addRole(MessageReceivedEvent event) {
		
		String msg = event.getMessage().getContentStripped();
		final Role role;
		{
			String roleName = msg.substring("?addrole ".length());
			List<Role> list = event.getGuild().getRolesByName(roleName, true);
			if (list.size() == 0) list = event.getGuild().getRolesByName(roleName, false);
			if (list.size() == 0) {
				event.getChannel().sendMessage("Unable to find role '" + roleName + "'. Are you sure it exists?").submit();
				return;
			}
			if (list.size() > 1) {
				String asdf = "I'm not sure which one of these you mean, be more specific: " + list.stream().map(Role::getName).collect(Collectors.joining(", "));
				event.getChannel().sendMessage(asdf).submit();
				return;
			}
			role = list.get(0);
		}
		
		event.getChannel().sendMessage(event.getMember().getAsMention() + " react to this message to set the emote for role " + role.getName())
			.submit()
			.thenAccept(response -> {
				reactMap.put(response.getGuild().getIdLong() + ":" + response.getIdLong(), role.getIdLong());
			});
	}
	
	private Message getMessageById(Guild guild, long channelID, long messageID) throws InterruptedException, ExecutionException {
		return guild.getTextChannelById(channelID).retrieveMessageById(messageID).submit().get();
	}
	
	private String getRolesMenuText(Guild guild) {
		StringBuilder sb = new StringBuilder();
		sb.append("React to this message to be assigned roles.\n\n");
		
		List<FlatRole> roles = DB.getAllRoles(guild.getIdLong());
		
		sb.append(roles.stream().map(fr->{
			StringBuilder s = new StringBuilder();
			if (fr.isEmoji) s.append(fr.emoji);
			else s.append(guild.getEmoteById(fr.emoteID).getAsMention());
			
			Role role = guild.getRoleById(fr.roleID);
			
			s.append(" " + role.getName());
				
			return s.toString();
		}).collect(Collectors.joining("\n\n")));
		
		return sb.toString();
	}
	
	private void updateRolesMenuReacts(Message message, List<FlatRole> roles) {
		
		message.clearReactions().submit();
		
		for (FlatRole role : roles) {
			if (role.isEmoji) message.addReaction(role.emoji).submit();
			else message.addReaction(message.getGuild().getEmoteById(role.emoteID)).submit();
		}
	}
	
	private void rolesMenu(MessageReceivedEvent event) {
				
		long guildID = event.getGuild().getIdLong();
		
		if (roleMenus.containsKey(guildID)) {
			long[] oldMessage = roleMenus.get(guildID);
			long oldChannelID = oldMessage[0];
			long oldMessageID = oldMessage[1];
			long oldOutputID = oldMessage[2];
			try {
				getMessageById(event.getGuild(), oldChannelID, oldMessageID).delete().submit();
				getMessageById(event.getGuild(), oldChannelID, oldOutputID).delete().submit();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		List<FlatRole> roles = DB.getAllRoles(guildID);
		long channelID = event.getChannel().getIdLong();
				
		event.getChannel().sendMessage(getRolesMenuText(event.getGuild()))
		.submit()
		.thenAccept(menuMessage -> {
			event.getChannel().sendMessage("Seriously, all you have to do is click!")
			.submit()
			.thenAccept(outputMessage ->{
				roleMenus.put(guildID, new long[] {channelID, menuMessage.getIdLong(), outputMessage.getIdLong()});
				try {
					DB.addRoleMenu(menuMessage.getIdLong(), outputMessage.getIdLong(), channelID, guildID);
				} catch (DBException e) {
					menuMessage.delete().submit();
					event.getChannel().sendMessage(event.getMember().getAsMention() + " " + e).submit();
					return;
				}
				
				updateRolesMenuReacts(menuMessage, roles);
			});
		});
		
		
	}
	
	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
		Long guildID = event.getGuild().getIdLong();
		Long messageID = event.getMessageIdLong();
		long channelID = event.getChannel().getIdLong();
		
		Message menuMessage;
		try {
			menuMessage = event.retrieveMessage().submit().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			event.getChannel().sendMessage(e.toString());
			return;
		}
		
		if (reactMap.containsKey(guildID + ":" + messageID)) { // admin is setting the emoji for a role
			handleAdminReact(event, menuMessage);
		} else if (roleMenus.containsKey(guildID) && roleMenuEquals(new long[] {channelID, messageID}, roleMenus.get(guildID))) { // user is requesting a role
			handleMenuReact(event, menuMessage);
		}
	}
	
	private boolean roleMenuEquals(long[] a, long[] b) {
		if (a == null || b == null) return false;
		if (a.length < 2 || b.length < 2) return false;
		return a[0] == b[0] && a[1] == b[1];
	}
	
	private void handleMenuReact(GuildMessageReactionAddEvent event, Message menuMessage) {
		
		if (event.getMember().getUser().isBot()) return;
				
		ReactionEmote re = event.getReactionEmote();
		long guildID = event.getGuild().getIdLong();
				
		long roleID;
		try {
			roleID = re.isEmoji()?DB.getEmojiRole(re.getEmoji(), guildID):DB.getEmoteRole(re.getEmote().getIdLong(), guildID);
		} catch (DBException e) {
			e.printStackTrace();
			event.getChannel().sendMessage(e.toString()).submit();
			return;
		}
		
		this.removeReaction(event, menuMessage);
		
		Role role = event.getGuild().getRoleById(roleID);
		
		Member member = event.getMember();
		
		Message outputMessage;
		try {
			outputMessage = getMessageById(event.getGuild(), menuMessage.getChannel().getIdLong(), roleMenus.get(event.getGuild().getIdLong())[2]);
		} catch (InterruptedException | ExecutionException e) {
			outputMessage = null;
		}
				
		if (member.getRoles().contains(role)) {
			event.getGuild().removeRoleFromMember(member, role).submit();
			if (outputMessage != null) outputMessage.editMessage("Removed role " + role.getName() + " from " + member.getEffectiveName()).submit();
		}else {
			event.getGuild().addRoleToMember(member, role).submit();
			if (outputMessage != null) outputMessage.editMessage("Added role " + role.getName() + " to " + member.getEffectiveName()).submit();
		}
		
		
	}
	
	private void handleAdminReact(GuildMessageReactionAddEvent event, Message message) {
		long messageID = event.getMessageIdLong();
		long guildID = event.getGuild().getIdLong();
		ReactionEmote re = event.getReactionEmote();

		removeReaction(event, message);

		// need to remove the reaction either way, so do that before checking adminship
		if (!isAdmin(event.getMember())) {
			return;
		}

		Role role = event.getGuild().getRoleById(reactMap.get(guildID + ":" + messageID));

		try {
			if (re.isEmoji()) {
				DB.addEmojiRole(re.getEmoji(), role.getIdLong(), guildID);
			} else {
				DB.addEmoteRole(re.getEmote().getIdLong(), role.getIdLong(), guildID);
			}
		} catch (DBException e) {
			event.getChannel().sendMessage(event.getMember().getAsMention() + " " + e).submit();
			return;
		}

		message.editMessage("Added role " + role.getName() + " - " + (re.isEmoji() ? re.getEmoji() : re.getEmote().getAsMention())).submit();

		// if role menu exists, update it with new roles
		if (roleMenus.containsKey(event.getGuild().getIdLong())) {
			
			long[] arr = roleMenus.get(event.getGuild().getIdLong());
			long channelID = arr[0];
			long menuID = arr[1];
			
			Message menu;
			try {
				menu = getMessageById(event.getGuild(), channelID, menuID);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return; // not much we can do here
			}
			
			menu.editMessage(getRolesMenuText(menu.getGuild())).submit();
			updateRolesMenuReacts(menu, DB.getAllRoles(menu.getGuild().getIdLong()));
		}
	}
	
	private void removeReaction(GuildMessageReactionAddEvent event, Message message) {
		ReactionEmote re = event.getReactionEmote();
		if (re.isEmoji()) message.removeReaction(re.getEmoji(), event.getUser()).submit();
		else message.removeReaction(re.getEmote(), event.getUser()).submit();
	}
	
	private static boolean isAdmin(Member member) {
		return member.getRoles().stream().anyMatch(role->role.hasPermission(Permission.ADMINISTRATOR));
	}
	
    public static void main(String[] args) throws InterruptedException {
    	
    	if (args.length != 1 || args[0].length() == 0) {
    		System.err.println("Must specify a single command line argument containing Discord token");
    		System.exit(1);
    	}
    	
    	try {
    	
    	jda = JDABuilder.createDefault(args[0].trim())
    	.addEventListeners(new DiscordRolesBot())
    	.build();
    	
    	jda.awaitReady();
    	LOGGER.info("Finished building JDA");
    	
    	} catch (LoginException e) {
    		LOGGER.error("LoginException: " + e);
    		throw new RuntimeException(e);
    	}
    }
}