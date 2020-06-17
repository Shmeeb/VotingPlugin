package com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Listeners.PlayerRewardEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.Injected.RewardInject;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.InjectedRequirement.RequirementInject;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.User;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Annotation.AnnotationHandler;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Item.ItemBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * The Class Reward.
 */
public class Reward {

	/** The plugin. */
	AdvancedCorePlugin plugin = AdvancedCorePlugin.getInstance();

	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private RewardFileData config;

	@Getter
	@Setter
	private boolean forceOffline;

	@Getter
	@Setter
	private boolean delayEnabled;

	@Getter
	@Setter
	private int delayHours;

	@Getter
	@Setter
	private int delayMinutes;

	@Getter
	@Setter
	private int delaySeconds;

	@Getter
	@Setter
	private boolean timedEnabled;

	@Getter
	@Setter
	private int timedHour;

	@Getter
	@Setter
	private int timedMinute;

	@Getter
	private RepeatHandle repeatHandle;

	@Getter
	@Setter
	private File file;

	/**
	 * Instantiates a new reward.
	 *
	 * @param file
	 *            the file
	 * @param reward
	 *            the reward
	 */
	public Reward(File file, String reward) {
		load(file, reward);
	}

	/**
	 * Instantiates a new reward.
	 *
	 * @param reward
	 *            the reward
	 */
	public Reward(String reward) {
		load(RewardHandler.getInstance().getDefaultFolder(), reward);
	}

	public Reward(String name, ConfigurationSection section) {
		load(name, section);
	}

	public boolean canGiveReward(User user, RewardOptions options) {
		for (RequirementInject inject : RewardHandler.getInstance().getInjectedRequirements()) {
			try {
				plugin.extraDebug(getRewardName() + ": Checking " + inject.getPath() + ":" + inject.getPriority());
				if (!inject.onRequirementRequest(this, user, getConfig().getConfigData(), options)) {
					return false;
				}
			} catch (Exception e) {
				plugin.debug("Failed to check requirement");
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public boolean checkDelayed(User user, HashMap<String, String> placeholders) {
		if (!isDelayEnabled()) {
			return false;
		}

		LocalDateTime time = LocalDateTime.now();
		time = time.plus(getDelayHours(), ChronoUnit.HOURS);
		time = time.plus(getDelayMinutes(), ChronoUnit.MINUTES);
		time = time.plus(getDelaySeconds(), ChronoUnit.SECONDS);
		checkRewardFile();
		user.addTimedReward(this, placeholders, time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

		plugin.debug("Giving reward " + name + " in " + getDelayHours() + " hours, " + getDelayMinutes() + " minutes, "
				+ getDelaySeconds() + " seconds (" + time.toString() + ")");
		return true;
	}

	public void checkRewardFile() {
		checkRewardFile(true);
	}

	public void checkRewardFile(boolean forceCheck) {
		if (!getConfig().hasRewardFile()) {
			if (!isRewardFileMatching()) {
				plugin.getLogger().warning("Detected a reward file " + getName()
						+ " edited when it should be edited where directly defined, overriding");
			}
			setRewardFile();
		} else if (forceCheck && !isRewardFileMatching()) {
			setRewardFile();
		}
	}

	public boolean checkTimed(User user, HashMap<String, String> placeholders) {
		if (!isTimedEnabled()) {
			return false;
		}

		LocalDateTime time = LocalDateTime.now();
		time = time.withHour(getTimedHour());
		time = time.withMinute(getTimedMinute());

		if (LocalDateTime.now().isAfter(time)) {
			time = time.plusDays(1);
		}
		checkRewardFile();
		user.addTimedReward(this, placeholders, time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

		plugin.debug("Giving reward " + name + " at " + time.toString());
		return true;
	}

	public ItemStack getItem() {
		return new ItemStack(Material.STONE);
	}

	public ItemStack getItemStack(User user, String item) {
		return new ItemBuilder(getConfig().getItemSection(item)).setSkullOwner(user.getOfflinePlayer())
				.toItemStack(user.getPlayer());
	}

	/**
	 * Gets the reward name.
	 *
	 * @return the reward name
	 */
	public String getRewardName() {
		return name;
	}

	public void giveInjectedRewards(User user, HashMap<String, String> placeholders) {

		for (final RewardInject inject : RewardHandler.getInstance().getInjectedRewards()) {
			boolean Addplaceholder = inject.isAddAsPlaceholder();
			try {
				Object obj = null;
				plugin.extraDebug(
						getRewardName() + ": Attempting to give " + inject.getPath() + ":" + inject.getPriority());
				if (!inject.isPostReward()) {
					if (inject.isSynchronize()) {
						synchronized (inject.getObject()) {
							obj = inject.onRewardRequest(this, user, getConfig().getConfigData(), placeholders);
						}
					} else {
						obj = inject.onRewardRequest(this, user, getConfig().getConfigData(), placeholders);
					}
					if (Addplaceholder && obj != null) {
						String placeholderName = inject.getPlaceholderName();
						String value = "";
						if (obj instanceof Boolean) {
							Boolean b = (Boolean) obj;
							value = b.toString();
						} else if (obj instanceof String) {
							String b = (String) obj;
							value = b;
						} else if (obj instanceof Double) {
							Double b = (Double) obj;
							value = b.toString();
						} else if (obj instanceof Integer) {
							Integer b = (Integer) obj;
							value = b.toString();
						}
						plugin.extraDebug("Adding placeholder " + placeholderName + ":" + value);
						placeholders.put(placeholderName, value);
					}
				} else {
					final Reward r = this;
					Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

						@Override
						public void run() {
							inject.onRewardRequest(r, user, getConfig().getConfigData(), placeholders);
						}
					});
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public void giveReward(User user, RewardOptions rewardOptions) {
		if (!AdvancedCorePlugin.getInstance().getOptions().isProcessRewards()) {
			AdvancedCorePlugin.getInstance().getLogger().warning("Processing rewards is disabled");
			return;
		}

		if (rewardOptions == null) {
			rewardOptions = new RewardOptions();
		}

		if (!rewardOptions.getPlaceholders().containsKey("ExecDate")) {
			rewardOptions.addPlaceholder("ExecDate", "" + System.currentTimeMillis());
		}

		PlayerRewardEvent event = new PlayerRewardEvent(this, user, rewardOptions);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			plugin.debug("Reward " + name + " was cancelled for " + user.getPlayerName());
			return;
		}

		if (rewardOptions.isCheckTimed()) {
			if (checkDelayed(user, rewardOptions.getPlaceholders())) {
				return;
			}

			if (checkTimed(user, rewardOptions.getPlaceholders())) {
				return;
			}
		}

		if (!rewardOptions.isOnlineSet()) {
			rewardOptions.setOnline(user.isOnline());
		}

		for (RewardPlaceholderHandle handle : RewardHandler.getInstance().getPlaceholders()) {
			if (handle.isPreProcess()) {
				rewardOptions.addPlaceholder(handle.getKey(), handle.getValue(this, user));
			}
		}

		// Check requirements
		boolean allowOffline = false;
		boolean canGive = true;
		if (!rewardOptions.isIgnoreRequirements()) {
			for (RequirementInject inject : RewardHandler.getInstance().getInjectedRequirements()) {
				try {
					plugin.extraDebug(getRewardName() + ": Checking requirement " + inject.getPath() + ":"
							+ inject.getPriority());
					if (!inject.onRequirementRequest(this, user, getConfig().getConfigData(), rewardOptions)) {
						plugin.debug(getRewardName() + ": Requirement failed " + inject.getPath() + ":"
								+ inject.isAllowReattempt());
						canGive = false;
						if (inject.isAllowReattempt()) {
							allowOffline = true;
						} else {
							return;
						}
					}
				} catch (Exception e) {
					plugin.debug("Failed to check requirement");
					e.printStackTrace();
					canGive = false;
				}
			}
		}

		// save reward for offline
		if (((!rewardOptions.isOnline() && !user.isOnline()) || allowOffline)
				&& (!isForceOffline() && !rewardOptions.isForceOffline())) {
			if (rewardOptions.isGiveOffline()) {
				checkRewardFile();
				user.addOfflineRewards(this, rewardOptions.getPlaceholders());
			}
			return;
		}

		// give reward
		if (canGive || isForceOffline() || rewardOptions.isForceOffline()) {
			plugin.debug("Attempting to give " + user.getPlayerName() + " reward " + name);
			giveRewardUser(user, rewardOptions.getPlaceholders(), rewardOptions);
		}
	}

	/**
	 * Give reward user.
	 *
	 * @param user
	 *            the user
	 * @param phs
	 *            placeholders
	 * @param rewardOptions
	 *            rewardOptions
	 */
	public void giveRewardUser(User user, HashMap<String, String> phs, RewardOptions rewardOptions) {

		Player player = user.getPlayer();
		if (player != null || isForceOffline()) {

			// placeholders
			if (phs == null) {
				phs = new HashMap<String, String>();
			}
			final String playerName = user.getPlayerName();
			phs.put("player", playerName);
			phs.put("@p", playerName);
			LocalDateTime ldt = LocalDateTime.now();
			Date date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
			phs.put("CurrentDate", "" + new SimpleDateFormat("EEE, d MMM yyyy HH:mm").format(date));
			phs.put("uuid", user.getUUID());

			for (RewardPlaceholderHandle handle : RewardHandler.getInstance().getPlaceholders()) {
				if (!handle.isPreProcess()) {
					phs.put(handle.getKey(), handle.getValue(this, user));
				}
			}

			final HashMap<String, String> placeholders = new HashMap<String, String>(phs);

			giveInjectedRewards(user, placeholders);

			plugin.debug("Gave " + user.getPlayerName() + " reward " + name);

			if (rewardOptions.isCheckRepeat()) {
				if (repeatHandle.isEnabled() && !repeatHandle.isRepeatOnStartup()) {
					repeatHandle.giveRepeat(user);
				}
			}
		}
	}

	public boolean isRewardFileMatching() {
		Reward reward = RewardHandler.getInstance().getReward(name);
		ConfigurationSection section = getConfig().getConfigData();
		if (reward.getConfig().getConfigData().getConfigurationSection("").getKeys(true).size() != 0) {
			if (reward.getConfig().getConfigData().getConfigurationSection("").getKeys(true)
					.size() != section.getKeys(true).size() + 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Load.
	 *
	 * @param folder
	 *            the folder
	 * @param reward
	 *            the reward
	 */
	public void load(File folder, String reward) {
		name = reward;
		if (folder.isDirectory()) {
			file = new File(folder, reward + ".yml");
		} else {
			file = folder;
		}
		config = new RewardFileData(this, folder);
		loadValues();
	}

	public void load(String name, ConfigurationSection section) {
		config = new RewardFileData(this, section);
		this.name = name;
		loadValues();
	}

	public void loadValues() {
		forceOffline = getConfig().getForceOffline();

		setDelayEnabled(getConfig().getDelayedEnabled());
		if (delayEnabled) {
			setDelayHours(getConfig().getDelayedHours());
			setDelayMinutes(getConfig().getDelayedMinutes());
			setDelaySeconds(getConfig().getDelayedSeconds());
		}

		setTimedEnabled(getConfig().getTimedEnabled());
		if (timedEnabled) {
			setTimedHour(getConfig().getTimedHour());
			setTimedMinute(getConfig().getTimedMinute());
		}

		repeatHandle = new RepeatHandle(this);

		new AnnotationHandler().load(getConfig().getConfigData(), this);
	}

	private void setRewardFile() {
		Reward reward = RewardHandler.getInstance().getReward(name);
		ConfigurationSection section = getConfig().getConfigData();
		reward.getConfig().setData(section);
		reward.getConfig().getFileData().options()
				.header("Directly defined reward file. ANY EDITS HERE CAN GET OVERRIDDEN!");
		reward.getConfig().setDirectlyDefinedReward(true);
		reward.getConfig().save(reward.getConfig().getFileData());
		RewardHandler.getInstance().updateReward(reward);
	}

	public void validate() {
		if (getName().equalsIgnoreCase("examplebasic") || getName().equalsIgnoreCase("exampleadvanced")) {
			return;
		}
		for (RequirementInject inject : RewardHandler.getInstance().getInjectedRequirements()) {
			inject.validate(this, getConfig().getConfigData());
		}
		for (RewardInject inject : RewardHandler.getInstance().getInjectedRewards()) {
			inject.validate(this, getConfig().getConfigData());
		}
	}

}
