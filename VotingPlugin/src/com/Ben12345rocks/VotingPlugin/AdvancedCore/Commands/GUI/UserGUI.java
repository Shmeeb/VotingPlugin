package com.Ben12345rocks.VotingPlugin.AdvancedCore.Commands.GUI;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.Reward;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.RewardHandler;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.RewardOptions;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.User;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UserManager;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.EditGUI.EditGUI;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.EditGUI.EditGUIButton;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.EditGUI.ValueTypes.EditGUIValueString;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Inventory.BInventory;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Inventory.BInventory.ClickEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Inventory.BInventoryButton;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Item.ItemBuilder;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.ArrayUtils;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.PlayerUtils;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.ValueRequest.ValueRequest;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.ValueRequest.Listeners.StringListener;

/**
 * The Class UserGUI.
 */
public class UserGUI {

	/** The instance. */
	static UserGUI instance = new UserGUI();

	/**
	 * Gets the single instance of UserGUI.
	 *
	 * @return single instance of UserGUI
	 */
	public static UserGUI getInstance() {
		return instance;
	}

	AdvancedCorePlugin plugin = AdvancedCorePlugin.getInstance();

	/** The plugin buttons. */
	private HashMap<Plugin, BInventoryButton> extraButtons = new HashMap<Plugin, BInventoryButton>();

	/**
	 * Instantiates a new user GUI.
	 */
	private UserGUI() {
	}

	/**
	 * Adds the plugin button.
	 *
	 * @param plugin
	 *            the plugin
	 * @param inv
	 *            the inv
	 */
	public synchronized void addPluginButton(Plugin plugin, BInventoryButton inv) {
		extraButtons.put(plugin, inv);
	}

	/**
	 * Gets the current player.
	 *
	 * @param player
	 *            the player
	 * @return the current player
	 */
	public String getCurrentPlayer(Player player) {
		return (String) PlayerUtils.getInstance().getPlayerMeta(player, "UserGUI");
	}

	/**
	 * Open user GUI.
	 *
	 * @param player
	 *            the player
	 * @param playerName
	 *            the player name
	 */
	public void openUserGUI(Player player, final String playerName) {
		if (!player.hasPermission("AdvancedCore.UserEdit")) {
			player.sendMessage("Not enough permissions");
			return;
		}
		BInventory inv = new BInventory("UserGUI: " + playerName);
		inv.addData("player", playerName);
		inv.addButton(new BInventoryButton("Give Reward File", new String[] {}, new ItemStack(Material.STONE)) {

			@Override
			public void onClick(ClickEvent clickEvent) {
				ArrayList<String> rewards = new ArrayList<String>();
				for (Reward reward : RewardHandler.getInstance().getRewards()) {
					rewards.add(reward.getRewardName());
				}

				new ValueRequest().requestString(clickEvent.getPlayer(), "", ArrayUtils.getInstance().convert(rewards),
						true, new StringListener() {

							@Override
							public void onInput(Player player, String value) {
								User user = UserManager.getInstance()
										.getUser(UserGUI.getInstance().getCurrentPlayer(player));
								RewardHandler.getInstance().giveReward(user, value, new RewardOptions());
								player.sendMessage("Given " + user.getPlayerName() + " reward file " + value);

							}
						});

			}
		});

		inv.addButton(new BInventoryButton(new ItemBuilder(Material.WRITTEN_BOOK).setName("Edit Data")) {

			@Override
			public void onClick(ClickEvent clickEvent) {
				Player player = clickEvent.getPlayer();
				EditGUI inv = new EditGUI("Edit Data, click to change");
				final User user = UserManager.getInstance().getUser(playerName);
				for (final String key : user.getData().getKeys()) {
					String value = user.getData().getString(key);
					inv.addButton(new EditGUIButton(new ItemBuilder(Material.STONE).setName(key + " = " + value),
							new EditGUIValueString(key, value) {

								@Override
								public void setValue(Player player, String value) {
									user.getData().setString(key, value);
									openUserGUI(player, playerName);
								}
							}));
				}

				inv.openInventory(player);

			}
		});

		inv.addButton(new BInventoryButton(new ItemBuilder(Material.PAPER).setName("&cView player data")) {

			@Override
			public void onClick(ClickEvent clickEvent) {
				Player player = clickEvent.getPlayer();
				User user = UserManager.getInstance().getUser(player);
				for (String key : user.getData().getKeys()) {
					user.sendMessage("&c&l" + key + " &c" + user.getData().getString(key));
				}
			}
		});

		for (BInventoryButton button : extraButtons.values()) {
			inv.addButton(button);
		}

		inv.openInventory(player);
	}

	/**
	 * Open users GUI.
	 *
	 * @param player
	 *            the player
	 */
	public void openUsersGUI(Player player) {
		if (!player.hasPermission("AdvancedCore.UserEdit")) {
			player.sendMessage("Not enough permissions");
			return;
		}

		ArrayList<String> players = new ArrayList<String>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			players.add(p.getName());
		}
		new ValueRequest().requestString(player, "", ArrayUtils.getInstance().convert(players), true,
				new StringListener() {

					@Override
					public void onInput(Player player, String value) {
						setCurrentPlayer(player, value);
						openUserGUI(player, value);
					}
				});
	}

	/**
	 * Sets the current player.
	 *
	 * @param player
	 *            the player
	 * @param playerName
	 *            the player name
	 */
	private void setCurrentPlayer(Player player, String playerName) {
		PlayerUtils.getInstance().setPlayerMeta(player, "UserGUI", playerName);
	}
}
