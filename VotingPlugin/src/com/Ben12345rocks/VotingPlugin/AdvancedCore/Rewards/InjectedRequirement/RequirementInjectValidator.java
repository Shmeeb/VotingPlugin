package com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.InjectedRequirement;

import org.bukkit.configuration.ConfigurationSection;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.Reward;

public abstract class RequirementInjectValidator {

	public abstract void onValidate(Reward reward, RequirementInject inject, ConfigurationSection data);

	public void warning(Reward reward, RequirementInject inject, String str) {
		AdvancedCorePlugin.getInstance().getLogger()
				.warning("RequirementInject Validator: " + reward.getName() + ", Directly Defined: "
						+ reward.getConfig().isDirectlyDefinedReward() + " Path: " + inject.getPath() + " : " + str);
	}
}
