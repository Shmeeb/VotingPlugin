package com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.ValueRequest.Listeners;

import org.bukkit.entity.Player;

public abstract class Listener<T> {

	public abstract void onInput(Player player, T value);

}
