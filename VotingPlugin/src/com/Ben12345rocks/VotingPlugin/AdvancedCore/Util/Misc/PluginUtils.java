package com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;

public class PluginUtils {
	/** The instance. */
	static PluginUtils instance = new PluginUtils();

	public static PluginUtils getInstance() {
		return instance;
	}

	/** The plugin. */
	AdvancedCorePlugin plugin = AdvancedCorePlugin.getInstance();

	private PluginUtils() {
	}

	public long getFreeMemory() {
		return Runtime.getRuntime().freeMemory() / (1024 * 1024);
	}

	public long getMemory() {
		return Runtime.getRuntime().totalMemory() / (1024 * 1024);
	}

	public void registerCommands(JavaPlugin plugin, String commandText, CommandExecutor executor, TabCompleter tab) {
		plugin.getCommand(commandText).setExecutor(executor);
		if (tab != null) {
			plugin.getCommand(commandText).setTabCompleter(tab);
		}
	}

	public void registerEvents(Listener listener, JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(listener, plugin);
	}

}
