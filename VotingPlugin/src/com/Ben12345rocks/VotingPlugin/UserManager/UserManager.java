package com.Ben12345rocks.VotingPlugin.UserManager;

import java.util.ArrayList;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UUID;
import com.Ben12345rocks.VotingPlugin.Main;
import com.Ben12345rocks.VotingPlugin.Objects.User;

public class UserManager {
	/** The instance. */
	static UserManager instance = new UserManager();
	/** The plugin. */
	static Main plugin = Main.plugin;

	/**
	 * Gets the single instance of UserManager.
	 *
	 * @return single instance of UserManager
	 */
	public static UserManager getInstance() {
		return instance;
	}

	public UserManager() {
		super();
	}

	public ArrayList<String> getAllUUIDs() {
		return com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UserManager.getInstance().getAllUUIDs();
	}

	public User getVotingPluginUser(com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.User user) {
		return getVotingPluginUser(java.util.UUID.fromString(user.getUUID()));
	}

	public User getVotingPluginUser(java.util.UUID uuid) {
		return getVotingPluginUser(new UUID(uuid.toString()));
	}

	public User getVotingPluginUser(OfflinePlayer player) {
		return getVotingPluginUser(player.getName());
	}

	public User getVotingPluginUser(Player player) {
		return getVotingPluginUser(player.getName());
	}

	@SuppressWarnings("deprecation")
	public User getVotingPluginUser(String playerName) {
		return new User(com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UserManager.getInstance().getProperName(playerName));
	}

	@SuppressWarnings("deprecation")
	public User getVotingPluginUser(UUID uuid) {
		return new User(uuid);
	}
}
