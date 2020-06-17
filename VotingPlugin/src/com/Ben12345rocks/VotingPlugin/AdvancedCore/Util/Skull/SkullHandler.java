package com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Skull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.NMSManager.NMSManager;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.NMSManager.ReflectionUtils;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Item.ItemBuilder;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.PluginUtils;

import lombok.Getter;

public class SkullHandler {

	private static SkullHandler instance = new SkullHandler();

	public static SkullHandler getInstance() {
		return instance;
	}

	@SuppressWarnings("rawtypes")
	private Class craftItemStack;
	@SuppressWarnings("rawtypes")
	private Class itemStack;
	@Getter
	private Method asNMSCopy;

	private Method asBukkitCopy;

	@Getter
	private ConcurrentHashMap<String, Object> skulls = new ConcurrentHashMap<String, Object>();

	Queue<String> skullsToLoad = new ConcurrentLinkedQueue<String>();

	private Timer timer = new Timer();

	private void add(String playerName) {
		if (skullsToLoad.contains(playerName) && getSkulls().containsKey(playerName)) {
			skullsToLoad.add(playerName);
		}
	}

	/*
	 * @SuppressWarnings("rawtypes")
	 * private Class gameProfile;
	 * @SuppressWarnings("rawtypes")
	 * private Class property;
	 * @SuppressWarnings("rawtypes")
	 * private Constructor gameProfileConstructor;
	 * @SuppressWarnings("rawtypes")
	 * private Constructor propertyConstructor;
	 * private Method gameProfileGetProperties;
	 * public ItemStack getHead(String url) {
	 * ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
	 * ItemMeta headMeta = head.getItemMeta();
	 * try {
	 * // GameProfile profile = new GameProfile(UUID.randomUUID(), null);
	 * Object profile = gameProfileConstructor.newInstance(UUID.randomUUID(), null);
	 * byte[] encodedData = Base64.getEncoder()
	 * .encode((String.format("{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}",
	 * url).getBytes()));
	 * gameProfileGetProperties.invoke(gameProfile, "textures",
	 * propertyConstructor.newInstance("textures", new String(encodedData)));
	 * Field profileField = null;
	 * profileField = headMeta.getClass().getDeclaredField("profile");
	 * profileField.setAccessible(true);
	 * profileField.set(headMeta, profile);
	 * } catch (IllegalArgumentException | IllegalAccessException |
	 * NoSuchFieldException | SecurityException
	 * | InvocationTargetException | InstantiationException e) {
	 * e.printStackTrace();
	 * }
	 * head.setItemMeta(headMeta);
	 * return head;
	 * }
	 */

	@SuppressWarnings("deprecation")
	public ItemStack getItemStack(String playerName) {
		if (hasSkull(playerName)) {
			try {
				return (ItemStack) asBukkitCopy.invoke(null, skulls.get(playerName));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

		} else {
			loadSkull(playerName);
		}
		return new ItemBuilder(Material.SKULL_ITEM).setSkullOwner(playerName).toItemStack();

	}

	@SuppressWarnings("deprecation")
	public ItemStack getSkull(String playerName) {

		return new ItemBuilder(Material.SKULL_ITEM).setSkullOwner(playerName).toItemStack();

	}

	public boolean hasSkull(String playerName) {
		if (skulls.containsKey(playerName)) {
			if (skulls.get(playerName) != null) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void load() {
			/*
			 * gameProfile =
			 * ReflectionUtils.getClassForName("com.mojang.authlib.GameProfile");
			 * property =
			 * ReflectionUtils.getClassForName("com.mojang.authlib.properties.Property");
			 * gameProfileConstructor = gameProfile.getConstructor(UUID.class,
			 * String.class);
			 * propertyConstructor = property.getConstructor(String.class, String.class);
			 * gameProfileGetProperties = gameProfile.getDeclaredMethod("getProperties");



			craftItemStack = ReflectionUtils.getClassForName(
					"org.bukkit.craftbukkit." + NMSManager.getInstance().getVersion() + "inventory.CraftItemStack");
			itemStack = NMSManager.getInstance().getNMSClass("ItemStack");

			asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
			asNMSCopy.setAccessible(true);
		} catch (SecurityException | NoSuchMethodException e) {
			e.printStackTrace();
		}

		try {
			asBukkitCopy = craftItemStack.getDeclaredMethod("asBukkitCopy", itemStack);
			asBukkitCopy.setAccessible(true);
		} catch (SecurityException | NoSuchMethodException e) {
			e.printStackTrace();
		}

		*/

		if (AdvancedCorePlugin.getInstance().getOptions().isPreloadSkulls()) {
			Bukkit.getScheduler().runTaskAsynchronously(AdvancedCorePlugin.getInstance(), new Runnable() {

				@Override
				public void run() {
					SkullThread.getInstance().getThread().startup();
				}
			});
		}

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				for (String str : skullsToLoad) {
					if (!getSkulls().containsKey(str)) {
						SkullThread.getInstance().getThread().load(str);
					}
				}
			}
		}, 6 * 10000, 20 * 1000);

	}

	public void loadSkull(Player player) {
		loadSkull(player.getName());
	}

	public void loadSkull(final String playerName) {
		if (AdvancedCorePlugin.getInstance().isEnabled() && AdvancedCorePlugin.getInstance().getOptions().isLoadSkulls()
				&& Runtime.getRuntime().freeMemory() > 500 && PluginUtils.getInstance().getMemory() > 2000) {
			if (Bukkit.isPrimaryThread()) {
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						add(playerName);
					}
				}, 0);
			} else {
				add(playerName);
			}
		}
	}

}
