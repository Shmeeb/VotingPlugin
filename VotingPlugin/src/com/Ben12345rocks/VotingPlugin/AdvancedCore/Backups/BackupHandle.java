package com.Ben12345rocks.VotingPlugin.AdvancedCore.Backups;

import java.io.File;
import java.time.LocalDateTime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.TimeChecker;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.TimeType;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.TimeChecker.Events.DateChangedEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.MiscUtils;

public class BackupHandle implements Listener {
	private static BackupHandle instance = new BackupHandle();

	public static BackupHandle getInstance() {
		return instance;
	}

	public BackupHandle() {
	}

	public void checkOldBackups() {
		for (File file : new File(AdvancedCorePlugin.getInstance().getDataFolder(), "Backups").listFiles()) {
			long lastModified = file.lastModified();
			if (LocalDateTime.now().minusDays(5).isAfter(MiscUtils.getInstance().getTime(lastModified))) {
				file.delete();
				AdvancedCorePlugin.getInstance().debug("Deleting old backup: " + file.getName());
			}
		}
	}

	@EventHandler
	public void onDateChange(DateChangedEvent e) {
		if (!e.getTimeType().equals(TimeType.DAY)) {
			return;
		}

		if (!AdvancedCorePlugin.getInstance().getOptions().isCreateBackups()) {
			return;
		}

		LocalDateTime now = TimeChecker.getInstance().getTime();
		ZipCreator.getInstance().create(AdvancedCorePlugin.getInstance().getDataFolder(),
				new File(AdvancedCorePlugin.getInstance().getDataFolder(), "Backups" + File.separator + "Backup-"
						+ now.getYear() + "_" + now.getMonth() + "_" + now.getDayOfMonth() + ".zip"));

		checkOldBackups();
	}
}
