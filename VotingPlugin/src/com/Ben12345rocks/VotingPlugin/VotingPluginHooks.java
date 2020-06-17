package com.Ben12345rocks.VotingPlugin;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.RewardHandler;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.Injected.RewardInject;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.InjectedRequirement.RequirementInject;
import com.Ben12345rocks.VotingPlugin.UserManager.UserManager;

public class VotingPluginHooks {
	private static VotingPluginHooks instance = new VotingPluginHooks();

	public static VotingPluginHooks getInstance() {
		return instance;
	}

	public void addCustomRequirement(RequirementInject inject) {
		RewardHandler.getInstance().addInjectedRequirements(inject);
	}

	public void addCustomReward(RewardInject inject) {
		RewardHandler.getInstance().addInjectedReward(inject);
	}

	public Main getMainClass() {
		return Main.plugin;
	}

	public UserManager getUserManager() {
		return UserManager.getInstance();
	}

}
