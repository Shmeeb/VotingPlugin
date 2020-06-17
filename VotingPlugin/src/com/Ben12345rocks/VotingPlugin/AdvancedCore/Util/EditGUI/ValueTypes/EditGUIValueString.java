package com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.EditGUI.ValueTypes;

import org.bukkit.entity.Player;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Inventory.BInventory.ClickEvent;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Messages.StringParser;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.ArrayUtils;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.ValueRequest.ValueRequestBuilder;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.ValueRequest.Listeners.Listener;

public abstract class EditGUIValueString extends EditGUIValue {

	public EditGUIValueString(String key, Object value) {
		setKey(key);
		setCurrentValue(value);
	}

	@Override
	public void onClick(ClickEvent clickEvent) {
		if (getCurrentValue() == null) {
			setCurrentValue("");
		}
		new ValueRequestBuilder(new Listener<String>() {
			@Override
			public void onInput(Player player, String value) {
				setValue(player, value);
				player.sendMessage(StringParser.getInstance().colorize("&cSetting " + getKey() + " to " + value));
			}
		}, ArrayUtils.getInstance().convert(getOptions())).currentValue(getCurrentValue().toString())
				.allowCustomOption(true).request(clickEvent.getPlayer());
	}

	public abstract void setValue(Player player, String value);
}
