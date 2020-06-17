package com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker;

import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.DateChangedEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.DayChangeEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.MonthChangeEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.PreDateChangedEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.WeekChangeEvent;

/**
 * The Class TimeChecker.
 */
public class TimeChecker {

	/** The instance. */
	static TimeChecker instance = new TimeChecker();

	/**
	 * Gets the single instance of TimeChecker.
	 *
	 * @return single instance of TimeChecker
	 */
	public static TimeChecker getInstance() {
		return instance;
	}

	private Timer timer = new Timer();

	/** The plugin. */
	AdvancedCorePlugin plugin = AdvancedCorePlugin.getInstance();

	private boolean timerLoaded = false;

	/**
	 * Instantiates a new time checker.
	 */
	private TimeChecker() {
	}

	public void forceChanged(TimeType time) {
		forceChanged(time, true);
	}

	public void forceChanged(final TimeType time, final boolean fake) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				plugin.debug("Executing time change events: " + time.toString());
				plugin.getLogger().info("Time change event: " + time.toString() + ", Fake: " + fake);
				PreDateChangedEvent preDateChanged = new PreDateChangedEvent(time);
				preDateChanged.setFake(fake);
				plugin.getServer().getPluginManager().callEvent(preDateChanged);
				if (time.equals(TimeType.DAY)) {
					DayChangeEvent dayChange = new DayChangeEvent();
					dayChange.setFake(fake);
					plugin.getServer().getPluginManager().callEvent(dayChange);
				} else if (time.equals(TimeType.WEEK)) {
					WeekChangeEvent weekChange = new WeekChangeEvent();
					weekChange.setFake(fake);
					plugin.getServer().getPluginManager().callEvent(weekChange);
				} else if (time.equals(TimeType.MONTH)) {
					MonthChangeEvent monthChange = new MonthChangeEvent();
					monthChange.setFake(fake);
					plugin.getServer().getPluginManager().callEvent(monthChange);
				}

				DateChangedEvent dateChanged = new DateChangedEvent(time);
				dateChanged.setFake(fake);
				plugin.getServer().getPluginManager().callEvent(dateChanged);

				plugin.debug("Finished executing time change events: " + time.toString());
			}
		});

	}

	public LocalDateTime getTime() {
		return LocalDateTime.now().plusHours(AdvancedCorePlugin.getInstance().getOptions().getTimeHourOffSet());
	}

	/**
	 * Checks for day changed.
	 *
	 * @return true, if successful
	 */
	public boolean hasDayChanged() {
		int prevDay = ServerData.getInstance().getPrevDay();
		int day = getTime().getDayOfMonth();

		if (prevDay == day) {
			return false;
		}
		ServerData.getInstance().setPrevDay(day);
		if (prevDay == -1) {
			return false;
		}
		return true;
	}

	/**
	 * Checks for month changed.
	 *
	 * @return true, if successful
	 */
	public boolean hasMonthChanged() {
		String prevMonth = ServerData.getInstance().getPrevMonth();
		String month = getTime().getMonth().toString();
		if (prevMonth.equals(month)) {
			return false;
		}
		ServerData.getInstance().setPrevMonth(month);
		return true;

	}

	public boolean hasTimeOffSet() {
		return AdvancedCorePlugin.getInstance().getOptions().getTimeHourOffSet() != 0;
	}

	/**
	 * Checks for week changed.
	 *
	 * @return true, if successful
	 */
	public boolean hasWeekChanged() {
		int prevDate = ServerData.getInstance().getPrevWeekDay();
		LocalDateTime date = getTime();
		TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear();
		int weekNumber = date.get(woy);
		if (weekNumber == prevDate) {
			return false;
		}
		ServerData.getInstance().setPrevWeekDay(weekNumber);
		if (prevDate == -1) {
			return false;
		}
		return true;
	}

	public void loadTimer(int minutes) {
		if (!timerLoaded) {
			timerLoaded = true;
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					if (plugin != null) {
						update();
					} else {
						cancel();
						timerLoaded = false;
					}

				}
			}, 60 * 1000, minutes * 60 * 1000);
		} else {
			AdvancedCorePlugin.getInstance().debug("Timer is already loaded");
		}
	}

	/**
	 * Update.
	 */
	public void update() {
		if (plugin == null) {
			return;
		}
		if (hasTimeOffSet()) {
			plugin.extraDebug("TimeHourOffSet: " + getTime().getHour() + ":" + getTime().getMinute());
		}

		boolean dayChanged = false;
		boolean weekChanged = false;
		boolean monthChanged = false;
		if (hasDayChanged()) {
			plugin.debug("Day changed");
			dayChanged = true;
		}
		if (hasWeekChanged()) {
			plugin.debug("Week Changed");
			weekChanged = true;
		}
		if (hasMonthChanged()) {
			plugin.debug("Month Changed");
			monthChanged = true;
		}

		if (dayChanged) {
			forceChanged(TimeType.DAY, false);
		}
		if (weekChanged) {
			forceChanged(TimeType.WEEK, false);
		}
		if (monthChanged) {
			forceChanged(TimeType.MONTH, false);
		}

	}
}