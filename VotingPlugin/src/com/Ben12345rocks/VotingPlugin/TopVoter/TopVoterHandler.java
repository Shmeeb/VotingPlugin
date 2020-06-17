package com.Ben12345rocks.VotingPlugin.TopVoter;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.DateChangedEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.DayChangeEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.MonthChangeEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.PreDateChangedEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.WeekChangeEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UUID;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Messages.StringParser;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.ArrayUtils;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.YML.YMLFileHandler;
import com.Ben12345rocks.VotingPlugin.Main;
import com.Ben12345rocks.VotingPlugin.Config.Config;
import com.Ben12345rocks.VotingPlugin.Objects.User;
import com.Ben12345rocks.VotingPlugin.SpecialRewards.SpecialRewards;
import com.Ben12345rocks.VotingPlugin.UserManager.UserManager;

public class TopVoterHandler implements Listener {
	/** The instance. */
	static TopVoterHandler instance = new TopVoterHandler();

	/** The plugin. */
	static Main plugin = Main.plugin;

	/**
	 * Gets the single instance of TopVoter.
	 *
	 * @return single instance of TopVoter
	 */
	public static TopVoterHandler getInstance() {
		return instance;
	}

	/**
	 * Instantiates a new top voter.
	 */
	private TopVoterHandler() {
	}

	public ArrayList<String> getTopVoterBlackList() {
		return Config.getInstance().getBlackList();
	}

	/**
	 * Top voters weekly.
	 *
	 * @return the string[]
	 */

	public String[] getTopVotersWeekly() {
		ArrayList<String> msg = new ArrayList<String>();
		ArrayList<User> users = plugin.convertSet(plugin.getTopVoter(TopVoter.Weekly).keySet());
		for (int i = 0; i < users.size(); i++) {
			String line = Config.getInstance().getFormatCommandVoteTopLine().replace("%num%", "" + (i + 1))
					.replace("%player%", users.get(i).getPlayerName())
					.replace("%votes%", "" + plugin.getTopVoter(TopVoter.Weekly).get(users.get(i)));
			msg.add(line);
		}
		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	private HashMap<Integer, String> handlePlaces(Set<String> places) {
		HashMap<Integer, String> place = new HashMap<Integer, String>();
		for (String p : places) {
			String[] data = p.split("-");
			try {
				if (data.length > 1) {
					for (int i = Integer.parseInt(data[0]); i < Integer.parseInt(data[1]); i++) {
						place.put(i, p);
					}
				} else {
					place.put(Integer.parseInt(data[0]), p);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return place;
	}

	public void loadLastMonth() {
		if (Config.getInstance().isLastMonthGUI()) {
			plugin.getLastMonthTopVoter().clear();

			LinkedHashMap<User, Integer> totals = new LinkedHashMap<User, Integer>();
			for (String uuid : UserManager.getInstance().getAllUUIDs()) {
				User user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
				int total = user.getLastMonthTotal();
				if (total > 0) {
					totals.put(user, total);
				}
			}

			plugin.getLastMonthTopVoter().putAll(sortByValues(totals, false));

			plugin.debug("Loaded last month top voters");
		}

	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDateChanged(DateChangedEvent event) {
		plugin.setUpdate(true);
		plugin.update();
		loadLastMonth();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDayChange(DayChangeEvent event) {
		synchronized (Main.plugin) {
			for (String uuid : UserManager.getInstance().getAllUUIDs()) {
				User user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
				if (!user.voteStreakUpdatedToday(LocalDateTime.now().minusDays(1))) {
					if (user.getDayVoteStreak() != 0) {
						user.setDayVoteStreak(0);
					}
				}

				if (user.getHighestDailyTotal() < user.getTotal(TopVoter.Daily)) {
					user.setHighestDailyTotal(user.getTotal(TopVoter.Daily));
				}
				for (String shopIdent : Config.getInstance().getIdentifiers()) {
					if (Config.getInstance().getVoteShopResetDaily(shopIdent)) {
						user.setVoteShopIdentifierLimit(shopIdent, 0);
					}
				}
			}

			if (Config.getInstance().getStoreTopVotersDaily()) {
				plugin.getLogger().info("Saving TopVoters Daily");
				storeTopVoters(TopVoter.Daily);
			}

			try {
				if (Config.getInstance().isEnableDailyRewards()) {
					HashMap<Integer, String> places = handlePlaces(Config.getInstance().getDailyPossibleRewardPlaces());
					int i = 0;
					int lastTotal = -1;
					@SuppressWarnings("unchecked")
					LinkedHashMap<User, Integer> clone = (LinkedHashMap<User, Integer>) plugin
							.getTopVoter(TopVoter.Daily).clone();
					for (User user : clone.keySet()) {
						if (!Config.getInstance().getTopVoterIgnorePermission() || !user.isTopVoterIgnore()) {
							if (Config.getInstance().getTopVoterAwardsTies()) {
								if (user.getTotal(TopVoter.Daily) != lastTotal) {
									i++;
								}
							} else {
								i++;
							}
							if (places.containsKey(i)) {
								user.giveDailyTopVoterAward(i, places.get(i));
							}
						}
						lastTotal = user.getTotal(TopVoter.Daily);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			resetTotals(TopVoter.Daily);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onMonthChange(MonthChangeEvent event) {
		synchronized (Main.plugin) {
			for (String uuid : UserManager.getInstance().getAllUUIDs()) {
				User user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
				if (user.getTotal(TopVoter.Monthly) == 0 && user.getMonthVoteStreak() != 0) {
					user.setMonthVoteStreak(0);
				} else {
					if (!Config.getInstance().isVoteStreakRequirementUsePercentage() || user.hasPercentageTotal(
							TopVoter.Monthly, Config.getInstance().getVoteStreakRequirementMonth(),
							LocalDateTime.now().minusDays(1))) {
						user.addMonthVoteStreak();
						SpecialRewards.getInstance().checkVoteStreak(user, "Month");
					}
				}

				for (String shopIdent : Config.getInstance().getIdentifiers()) {
					if (Config.getInstance().getVoteShopResetMonthly(shopIdent)) {
						user.setVoteShopIdentifierLimit(shopIdent, 0);
					}
				}

				user.setLastMonthTotal(user.getTotal(TopVoter.Monthly));

				if (user.getHighestMonthlyTotal() < user.getTotal(TopVoter.Monthly)) {
					user.setHighestMonthlyTotal(user.getTotal(TopVoter.Monthly));
				}
			}

			if (Config.getInstance().getStoreTopVotersMonthly()) {
				plugin.getLogger().info("Saving TopVoters Monthly");
				storeTopVoters(TopVoter.Monthly);
			}

			try {
				if (Config.getInstance().isEnableMonthlyAwards()) {
					HashMap<Integer, String> places = handlePlaces(
							Config.getInstance().getMonthlyPossibleRewardPlaces());
					int i = 0;
					int lastTotal = -1;

					@SuppressWarnings("unchecked")
					LinkedHashMap<User, Integer> clone = (LinkedHashMap<User, Integer>) plugin
							.getTopVoter(TopVoter.Monthly).clone();
					for (User user : clone.keySet()) {

						if (!Config.getInstance().getTopVoterIgnorePermission() || !user.isTopVoterIgnore()) {
							if (Config.getInstance().getTopVoterAwardsTies()) {
								if (user.getTotal(TopVoter.Monthly) != lastTotal) {
									i++;
								}
							} else {
								i++;
							}
							if (places.containsKey(i)) {
								user.giveMonthlyTopVoterAward(i, places.get(i));
							}
						}
						lastTotal = user.getTotal(TopVoter.Monthly);
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			resetTotals(TopVoter.Monthly);

			if (Config.getInstance().getResetMilestonesMonthly()) {
				for (String uuid : UserManager.getInstance().getAllUUIDs()) {
					User user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
					user.setMilestoneCount(0);
					user.setHasGottenMilestone(new HashMap<String, Boolean>());
				}

			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPreDateChanged(PreDateChangedEvent event) {
		plugin.setUpdate(true);
		plugin.update();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWeekChange(WeekChangeEvent event) {
		synchronized (Main.plugin) {
			for (String uuid : UserManager.getInstance().getAllUUIDs()) {
				User user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
				if (user.getTotal(TopVoter.Weekly) == 0 && user.getWeekVoteStreak() != 0) {
					user.setWeekVoteStreak(0);
				} else {
					if (!Config.getInstance().isVoteStreakRequirementUsePercentage() || user.hasPercentageTotal(
							TopVoter.Weekly, Config.getInstance().getVoteStreakRequirementWeek(), null)) {
						user.addWeekVoteStreak();
						SpecialRewards.getInstance().checkVoteStreak(user, "Week");
					}
				}

				for (String shopIdent : Config.getInstance().getIdentifiers()) {
					if (Config.getInstance().getVoteShopResetWeekly(shopIdent)) {
						user.setVoteShopIdentifierLimit(shopIdent, 0);
					}
				}

				if (user.getHighestWeeklyTotal() < user.getTotal(TopVoter.Weekly)) {
					user.setHighestWeeklyTotal(user.getTotal(TopVoter.Weekly));
				}
			}

			if (Config.getInstance().getStoreTopVotersWeekly()) {
				plugin.getLogger().info("Saving TopVoters Weekly");
				storeTopVoters(TopVoter.Weekly);
			}

			try {
				if (Config.getInstance().isEnableWeeklyAwards()) {
					HashMap<Integer, String> places = handlePlaces(
							Config.getInstance().getWeeklyPossibleRewardPlaces());
					int i = 0;
					int lastTotal = -1;
					@SuppressWarnings("unchecked")
					LinkedHashMap<User, Integer> clone = (LinkedHashMap<User, Integer>) plugin
							.getTopVoter(TopVoter.Weekly).clone();
					for (User user : clone.keySet()) {
						if (!Config.getInstance().getTopVoterIgnorePermission() || !user.isTopVoterIgnore()) {
							if (Config.getInstance().getTopVoterAwardsTies()) {
								if (user.getTotal(TopVoter.Weekly) != lastTotal) {
									i++;
								}
							} else {
								i++;
							}
							if (places.containsKey(i)) {
								user.giveWeeklyTopVoterAward(i, places.get(i));
							}
						}
						lastTotal = user.getTotal(TopVoter.Weekly);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			resetTotals(TopVoter.Weekly);
		}
	}

	public void register() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void resetTotals(TopVoter topVoter) {
		for (String uuid : UserManager.getInstance().getAllUUIDs()) {
			User user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
			if (user.getTotal(topVoter) != 0) {
				user.resetTotals(topVoter);
			}
		}
	}

	public LinkedHashMap<User, Integer> sortByValues(LinkedHashMap<User, Integer> topVoterAllTime,
			final boolean order) {

		List<Entry<User, Integer>> list = new LinkedList<Entry<User, Integer>>(topVoterAllTime.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Entry<User, Integer>>() {
			@Override
			public int compare(Entry<User, Integer> o1, Entry<User, Integer> o2) {
				if (order) {
					return o1.getValue().compareTo(o2.getValue());
				} else {
					return o2.getValue().compareTo(o1.getValue());

				}
			}
		});

		// Maintaining insertion order with the help of LinkedList
		LinkedHashMap<User, Integer> sortedMap = new LinkedHashMap<User, Integer>();
		for (Entry<User, Integer> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

	public void storeTopVoters(TopVoter top) {
		LocalDateTime time = LocalDateTime.now().minusDays(1);
		String month = time.getMonth().toString();
		int year = time.getYear();
		int week = time.getDayOfYear();
		int day = time.getDayOfMonth();
		String fileName = "TopVoter" + File.separator + top.toString() + File.separator + year + "_" + month;

		if (top.equals(TopVoter.Daily)) {
			fileName += "_" + day;
		} else if (top.equals(TopVoter.Weekly)) {
			fileName += "_" + week;
		}
		fileName += ".yml";

		YMLFileHandler file = new YMLFileHandler(new File(plugin.getDataFolder(), fileName));
		file.setup();
		ArrayList<String> topVoters = new ArrayList<String>();
		int count = 1;
		for (Entry<User, Integer> entry : plugin.getTopVoter(top).entrySet()) {
			topVoters.add(count + ": " + entry.getKey().getPlayerName() + ": " + entry.getValue());
			count++;
		}
		file.getData().set(top.toString(), topVoters);
		file.saveData();
	}

	/**
	 * Top voter all time
	 *
	 * @param page
	 *            the page
	 * @return the string[]
	 */
	public String[] topVoterAllTime(int page) {
		int pagesize = Config.getInstance().getFormatPageSize();
		ArrayList<String> msg = new ArrayList<String>();
		ArrayList<String> topVoters = new ArrayList<String>();
		int count = 1;
		for (Entry<User, Integer> entry : plugin.getTopVoter(TopVoter.AllTime).entrySet()) {
			String line = Config.getInstance().getFormatCommandVoteTopLine();
			line = line.replace("%num%", "" + count);
			line = line.replace("%player%", entry.getKey().getPlayerName());
			line = line.replace("%votes%", "" + entry.getValue());
			topVoters.add(line);
			count++;
		}

		int pageSize = (topVoters.size() / pagesize);
		if ((topVoters.size() % pagesize) != 0) {
			pageSize++;
		}

		String title = Config.getInstance().getFormatCommandVoteTopTitle();
		title = title.replace("%page%", "" + page);
		title = title.replace("%maxpages%", "" + pageSize);
		title = title.replace("%Top%", "All Time");
		msg.add(StringParser.getInstance().colorize(title));

		for (int i = (page - 1) * pagesize; (i < topVoters.size()) && (i < (((page - 1) * pagesize) + 10)); i++) {
			msg.add(topVoters.get(i));
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	/**
	 * Top voter weekly.
	 *
	 * @param page
	 *            the page
	 * @return the string[]
	 */
	public String[] topVoterDaily(int page) {
		int pagesize = Config.getInstance().getFormatPageSize();
		ArrayList<String> msg = new ArrayList<String>();
		ArrayList<String> topVoters = new ArrayList<String>();
		int count = 1;
		for (Entry<User, Integer> entry : plugin.getTopVoter(TopVoter.Daily).entrySet()) {
			String line = Config.getInstance().getFormatCommandVoteTopLine();
			line = line.replace("%num%", "" + count);
			line = line.replace("%player%", entry.getKey().getPlayerName());
			line = line.replace("%votes%", "" + entry.getValue());
			topVoters.add(line);
			count++;
		}

		int pageSize = (topVoters.size() / pagesize);
		if ((topVoters.size() % pagesize) != 0) {
			pageSize++;
		}

		String title = Config.getInstance().getFormatCommandVoteTopTitle();
		title = title.replace("%page%", "" + page);
		title = title.replace("%maxpages%", "" + pageSize);
		title = title.replace("%Top%", "Daily");
		msg.add(StringParser.getInstance().colorize(title));

		for (int i = (page - 1) * pagesize; (i < topVoters.size()) && (i < (((page - 1) * pagesize) + 10)); i++) {
			msg.add(topVoters.get(i));
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	/**
	 * Top voter monthly
	 *
	 * @param page
	 *            the page
	 * @return the string[]
	 */
	public String[] topVoterMonthly(int page) {
		int pagesize = Config.getInstance().getFormatPageSize();
		ArrayList<String> msg = new ArrayList<String>();
		ArrayList<String> topVoters = new ArrayList<String>();
		int count = 1;
		for (Entry<User, Integer> entry : plugin.getTopVoter(TopVoter.Monthly).entrySet()) {
			String line = Config.getInstance().getFormatCommandVoteTopLine();
			line = line.replace("%num%", "" + count);
			line = line.replace("%player%", entry.getKey().getPlayerName());
			line = line.replace("%votes%", "" + entry.getValue());
			topVoters.add(line);
			count++;
		}

		int pageSize = (topVoters.size() / pagesize);
		if ((topVoters.size() % pagesize) != 0) {
			pageSize++;
		}

		String title = Config.getInstance().getFormatCommandVoteTopTitle();
		title = title.replace("%page%", "" + page);
		title = title.replace("%maxpages%", "" + pageSize);
		title = title.replace("%Top%", "Monthly");
		msg.add(StringParser.getInstance().colorize(title));

		for (int i = (page - 1) * pagesize; (i < topVoters.size()) && (i < (((page - 1) * pagesize) + 10)); i++) {
			msg.add(topVoters.get(i));
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	/**
	 * Top voters all time
	 *
	 * @return the string[]
	 */
	public String[] topVotersAllTime() {
		ArrayList<String> msg = new ArrayList<String>();
		List<Entry<User, Integer>> list = new LinkedList<Entry<User, Integer>>(
				plugin.getTopVoter(TopVoter.AllTime).entrySet());
		int i = 0;
		for (Entry<User, Integer> entry : list) {
			String line = "%num%: %player%, %votes%";
			line = line.replace("%num%", "" + (i + 1));
			try {
				line = line.replace("%player%", entry.getKey().getPlayerName());
			} catch (Exception ex) {
				Main.plugin.debug(ex);
			}
			line = line.replace("%votes%", "" + entry.getValue());

			msg.add(line);
			i++;
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	/**
	 * Top voters daily.
	 *
	 * @return the string[]
	 */

	public String[] topVotersDaily() {
		ArrayList<String> msg = new ArrayList<String>();
		ArrayList<User> users = plugin.convertSet(plugin.getTopVoter(TopVoter.Daily).keySet());
		for (int i = 0; i < users.size(); i++) {
			String line = "%num%: %player%, %votes%";
			line = line.replace("%num%", "" + (i + 1));
			try {
				line = line.replace("%player%", users.get(i).getPlayerName());
			} catch (Exception ex) {
				Main.plugin.debug(ex);
			}
			line = line.replace("%votes%", "" + plugin.getTopVoter(TopVoter.Monthly).get(users.get(i)));
			msg.add(line);
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	/**
	 * Top voters.
	 *
	 * @return the string[]
	 */
	public String[] topVotersMonthly() {
		ArrayList<String> msg = new ArrayList<String>();
		List<Entry<User, Integer>> list = new LinkedList<Entry<User, Integer>>(
				plugin.getTopVoter(TopVoter.Monthly).entrySet());
		int i = 0;
		for (Entry<User, Integer> entry : list) {
			String line = "%num%: %player%, %votes%";
			line = line.replace("%num%", "" + (i + 1));
			try {
				line = line.replace("%player%", entry.getKey().getPlayerName());
			} catch (Exception ex) {
				Main.plugin.debug(ex);
			}
			line = line.replace("%votes%", "" + entry.getValue());

			msg.add(line);
			i++;
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	/**
	 * Top voter weekly.
	 *
	 * @param page
	 *            the page
	 * @return the string[]
	 */
	public String[] topVoterWeekly(int page) {
		int pagesize = Config.getInstance().getFormatPageSize();
		ArrayList<String> msg = new ArrayList<String>();
		ArrayList<String> topVoters = new ArrayList<String>();
		int count = 1;
		for (Entry<User, Integer> entry : plugin.getTopVoter(TopVoter.Weekly).entrySet()) {
			String line = Config.getInstance().getFormatCommandVoteTopLine();
			line = line.replace("%num%", "" + count);
			line = line.replace("%player%", entry.getKey().getPlayerName());
			line = line.replace("%votes%", "" + entry.getValue());
			topVoters.add(line);
			count++;
		}

		int pageSize = (topVoters.size() / pagesize);
		if ((topVoters.size() % pagesize) != 0) {
			pageSize++;
		}

		String title = Config.getInstance().getFormatCommandVoteTopTitle();
		title = title.replace("%page%", "" + page);
		title = title.replace("%maxpages%", "" + pageSize);
		title = title.replace("%Top%", "Weekly");
		msg.add(StringParser.getInstance().colorize(title));

		for (int i = (page - 1) * pagesize; (i < topVoters.size()) && (i < (((page - 1) * pagesize) + 10)); i++) {
			msg.add(topVoters.get(i));
		}

		msg = ArrayUtils.getInstance().colorize(msg);
		return ArrayUtils.getInstance().convert(msg);
	}

	public synchronized void updateTopVoters(ArrayList<User> users1) {
		ArrayList<User> users = new ArrayList<User>();
		ArrayList<String> blackList = getTopVoterBlackList();
		for (User user : users1) {
			if (!blackList.contains(user.getPlayerName())) {
				if ((!Config.getInstance().getTopVoterIgnorePermission() || !user.isTopVoterIgnore())
						&& !user.isBanned()) {
					users.add(user);
				}
			}
		}

		plugin.debug("Number of users to check top voter: " + users.size());

		for (TopVoter top : TopVoter.values()) {
			plugin.getTopVoter(top).clear();
			if (Config.getInstance().getLoadTopVoter(top)) {
				for (User user : users) {
					int total = user.getTotal(top);
					if (total > 0) {
						plugin.getTopVoter(top).put(user, total);
					}
				}
				plugin.getTopVoter().put(top, sortByValues(plugin.getTopVoter(top), false));
				plugin.debug(top.toString() + " TopVoter loaded");
			}
		}

		plugin.debug("Updated TopVoter");
	}
}
