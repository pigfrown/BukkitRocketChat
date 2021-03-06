package com.protonmail.pigfrown.BukkitRocketChat;

import com.github.baloise.rocketchatrestclient.RocketChatClient;
import com.github.baloise.rocketchatrestclient.RocketChatRestApiV1Chat;
import com.github.baloise.rocketchatrestclient.RocketChatRestApiV1Channels;
import com.github.baloise.rocketchatrestclient.model.Channel;
import com.github.baloise.rocketchatrestclient.model.Message;
import com.github.baloise.rocketchatrestclient.model.Room;

import java.io.IOException;
import java.lang.RuntimeException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class RocketChat extends JavaPlugin implements Listener {
	public static Room targetRoom;
	public static RocketChatClient client;
	public static RocketChatRestApiV1Chat chat;
	
	public static boolean systemMessages;
	public static boolean deathMessages;
	public static boolean chatMessages;
	public static boolean playerJoinQuitMessages;
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		
		//register this class as event handler
		getServer().getPluginManager().registerEvents(this, this);
		
		//Get username/password and server address from config file
		FileConfiguration config = this.getConfig();
		String server = config.getString("server");
		String username = config.getString("username");
		String password = config.getString("password");
		String channelName = config.getString("channel");
		
		
		systemMessages = config.getBoolean("system_messages");
		deathMessages = config.getBoolean("death_messages");
		chatMessages = config.getBoolean("chat_messages");
		playerJoinQuitMessages = config.getBoolean("player_joinquit_messages");
		
		//Get a reference to the room we want to post in
		
		//TODO fix this try/catch so it works correctly
		try {
			client = new RocketChatClient(server, username, password);
		} catch (RuntimeException e) {
			getLogger().info("Server " + server + "is not a valid URL");
			return;
		}
		chat = client.getChatApi();
		RocketChatRestApiV1Channels channels = client.getChannelsApi();
		
		Channel [] allChannels = new Channel [] {};
		try {
			allChannels = channels.list();
		}
		catch (IOException e) {
			//Turn off all messaging if we aren't init properly
			getLogger().info("Error when requesting channel list, server or username/password probably incorrect");
			systemMessages = false;
			deathMessages = false;
			chatMessages = false;
			playerJoinQuitMessages = false;
			return;
		}

		boolean channelExists = false;
		Channel targetChannel = new Channel();
		//There must be a better way to do this? 
		for (int i = 0; i < allChannels.length; i++) {
			String name = allChannels[i].getName();
			if (name.equals(channelName)) {
				targetChannel = allChannels[i];
				channelExists = true;
				break;
			}
		}
		
		if (channelExists != true) {
			//Turn off all messaging if we can't init properly
			getLogger().info("Could not find the requested channel " + channelName + "on server " + server);
			systemMessages = false;
			deathMessages = false;
			chatMessages = false;
			playerJoinQuitMessages = false;
			return;
		}
		
		//Get the reference to the room
		targetRoom = new Room(targetChannel.getRoomId(), false);	
		
		if (systemMessages == true) {
			Message msg = new Message("Minecraft Server has started");
			sendMessage(msg);
			}
	}
	
	//Helper method to send a message to the configured RC Channel
	private void sendMessage(Message msg) {
		try {
			chat.postMessage(targetRoom, msg);
		} catch (IOException e) {
			getLogger().info("IOException when trying to post message : "+ msg.toString());
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (playerJoinQuitMessages != true) {
			return;
		}
		
		Message msg = new Message(event.getJoinMessage());
		sendMessage(msg);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (playerJoinQuitMessages != true) {
			return;
		}
		
		Message msg = new Message(event.getQuitMessage());
		sendMessage(msg);
	}
	
	@EventHandler
	public void onChatMessage(AsyncPlayerChatEvent event) {
		if (chatMessages != true) {
			return;
		}
		
		String playerName = event.getPlayer().getDisplayName();
		Message msg = new Message(playerName + ": " + event.getMessage());
		sendMessage(msg);
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (deathMessages != true) {
			return;
		}
		
		Message msg = new Message(event.getDeathMessage());
		sendMessage(msg);		
	}
	
	@Override
	public void onDisable() {
		if (systemMessages != true) {
			return;
		}
		Message msg = new Message("Minecraft Server has stopped");
		sendMessage(msg);
	
	}
	

}
