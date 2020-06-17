package com.Ben12345rocks.VotingPlugin.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.ArrayUtils;
import com.Ben12345rocks.VotingPlugin.Main;
import com.Ben12345rocks.VotingPlugin.Signs.SignHandler;

// TODO: Auto-generated Javadoc
/**
 * The Class ServerData.
 */
public class ServerData {

	/** The instance. */
	static ServerData instance = new ServerData();

	/** The plugin. */
	static Main plugin = Main.plugin;

	/**
	 * Gets the single instance of ServerData.
	 *
	 * @return single instance of ServerData
	 */
	public static ServerData getInstance() {
		return instance;
	}

	/**
	 * Instantiates a new server data.
	 */
	private ServerData() {
	}

	/**
	 * Instantiates a new server data.
	 *
	 * @param plugin
	 *            the plugin
	 */
	public ServerData(Main plugin) {
		ServerData.plugin = plugin;
	}

	public synchronized void addServiceSite(String site) {
		ArrayList<String> l = getServiceSites();
		if (!getServiceSites().contains(site)) {
			l.add(site);
		}
		setServiceSites(ArrayUtils.getInstance().removeDuplicates(l));
	}

	/**
	 * Adds the sign.
	 *
	 * @param location
	 *            the location
	 * @param data
	 *            the data
	 * @param position
	 *            the position
	 */
	public void addSign(Location location, String data, int position) {

		int count = nextSignNumber();

		getData().set("Signs." + count + ".World", location.getWorld().getName());
		getData().set("Signs." + count + ".X", location.getBlockX());
		getData().set("Signs." + count + ".Y", location.getBlockY());
		getData().set("Signs." + count + ".Z", location.getBlockZ());
		getData().set("Signs." + count + ".Data", data);
		getData().set("Signs." + count + ".Position", position);
		saveData();
		plugin.getSigns().add(new SignHandler("" + count, getSignLocation("" + count), getSignSkullLocation("" + count),
				getSignData("" + count), getSignPosition("" + count)));
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public ConfigurationSection getData() {
		ConfigurationSection data = com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData.getInstance().getData()
				.getConfigurationSection("VotingPlugin");
		if (data == null) {
			com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData.getInstance().getData().createSection("VotingPlugin");
			data = com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData.getInstance().getData()
					.getConfigurationSection("VotingPlugin");
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getServiceSites() {
		return (ArrayList<String>) getData().getList("ServiceSites", new ArrayList<String>());
	}

	/**
	 * Gets the sign data.
	 *
	 * @param sign
	 *            the sign
	 * @return the sign data
	 */
	public String getSignData(String sign) {
		return getData().getString("Signs." + sign + ".Data");
	}

	/**
	 * Gets the sign location.
	 *
	 * @param sign
	 *            the sign
	 * @return the sign location
	 */
	public Location getSignLocation(String sign) {
		return new Location(Bukkit.getWorld(getData().getString("Signs." + sign + ".World")),
				getData().getDouble("Signs." + sign + ".X"), getData().getDouble("Signs." + sign + ".Y"),
				getData().getDouble("Signs." + sign + ".Z"));
	}

	/**
	 * Gets the sign location.
	 *
	 * @param sign
	 *            the sign
	 * @return the sign location
	 */
	public Location getSignSkullLocation(String sign) {
		if (getData().getString("Signs." + sign + ".Skull.World", "").isEmpty()) {
			return null;
		}
		return new Location(Bukkit.getWorld(getData().getString("Signs." + sign + ".Skull.World")),
				getData().getDouble("Signs." + sign + ".Skull.X"), getData().getDouble("Signs." + sign + ".Skull.Y"),
				getData().getDouble("Signs." + sign + ".Skull.Z"));
	}

	/**
	 * Gets the sign position.
	 *
	 * @param sign
	 *            the sign
	 * @return the sign position
	 */
	public int getSignPosition(String sign) {
		return getData().getInt("Signs." + sign + ".Position");
	}

	/**
	 * Gets the signs.
	 *
	 * @return the signs
	 */
	public Set<String> getSigns() {
		try {
			return getData().getConfigurationSection("Signs").getKeys(false);
		} catch (Exception ex) {
			return new HashSet<String>();
		}
	}

	public int getVotePartyExtraRequired() {
		return getData().getInt("VotePartyExtraRequired", 0);
	}

	/**
	 * Next sign number.
	 *
	 * @return the int
	 */
	public int nextSignNumber() {
		Set<String> signs = getSigns();

		if (signs != null) {
			for (int i = 0; i < 100000; i++) {
				if (!signs.contains(Integer.toString(i))) {
					return i;
				}
			}
		}
		return 0;
	}

	/**
	 * Reload data.
	 */
	public void reloadData() {
		com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData.getInstance().reloadData();
	}

	/**
	 * Removes the sign.
	 *
	 * @param sign
	 *            the sign
	 */
	public void removeSign(String sign) {
		getData().set("Signs." + sign + ".World", null);
		getData().set("Signs." + sign + ".X", null);
		getData().set("Signs." + sign + ".Y", null);
		getData().set("Signs." + sign + ".Z", null);
		getData().set("Signs." + sign + ".Data", null);
		getData().set("Signs." + sign + ".Position", null);
		getData().set("Signs." + sign, null);
		getData().set("Signs." + sign + ".Skull.World", null);
		getData().set("Signs." + sign + ".Skull.X", null);
		getData().set("Signs." + sign + ".Skull.Y", null);
		getData().set("Signs." + sign + ".Skull.Z", null);
		saveData();
	}

	/**
	 * Save data.
	 */
	public synchronized void saveData() {
		com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData.getInstance().saveData();
	}

	public void setServiceSites(ArrayList<String> list) {
		getData().set("ServiceSites", list);
		saveData();
	}

	public void setSign(String count, Location location, Location skullLocation, String data, int position) {

		getData().set("Signs." + count + ".World", location.getWorld().getName());
		getData().set("Signs." + count + ".X", (int) location.getX());
		getData().set("Signs." + count + ".Y", (int) location.getY());
		getData().set("Signs." + count + ".Z", (int) location.getZ());
		getData().set("Signs." + count + ".Data", data);
		getData().set("Signs." + count + ".Position", position);
		if (skullLocation != null) {
			getData().set("Signs." + count + ".Skull.World", skullLocation.getWorld().getName());
			getData().set("Signs." + count + ".Skull.X", (int) skullLocation.getX());
			getData().set("Signs." + count + ".Skull.Y", (int) skullLocation.getY());
			getData().set("Signs." + count + ".Skull.Z", (int) skullLocation.getZ());
		}
		saveData();
	}

	public void setSkullLocation(String count, Location skullLocation) {
		if (skullLocation != null) {
			getData().set("Signs." + count + ".Skull.World", skullLocation.getWorld().getName());
			getData().set("Signs." + count + ".Skull.X", (int) skullLocation.getX());
			getData().set("Signs." + count + ".Skull.Y", (int) skullLocation.getY());
			getData().set("Signs." + count + ".Skull.Z", (int) skullLocation.getZ());
		}
	}

	/**
	 * Sets the version.
	 */
	public void setVersion() {
		getData().set("Version", Bukkit.getVersion());
		saveData();
	}

	public void setVotePartyExtraRequired(int value) {
		getData().set("VotePartyExtraRequired", value);
		saveData();
	}

	/**
	 * Update values.
	 */
	public void updateValues() {
		setVersion();
	}
}
