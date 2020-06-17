package com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.InjectedRequirement;

import java.util.ArrayList;

import org.bukkit.configuration.ConfigurationSection;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.Reward;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards.RewardOptions;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.User;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.ArrayUtils;

import lombok.Getter;
import lombok.Setter;

public abstract class RequirementInjectStringList extends RequirementInject {

	@Getter
	@Setter
	private ArrayList<String> defaultValue = new ArrayList<String>();

	public RequirementInjectStringList(String path) {
		super(path);
	}

	public RequirementInjectStringList(String path, ArrayList<String> defaultValue) {
		super(path);
		this.defaultValue = defaultValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onRequirementRequest(Reward reward, User user, ConfigurationSection data,
			RewardOptions rewardOptions) {
		if (data.isList(getPath()) || (isAlwaysForce() && data.contains(getPath()))) {
			ArrayList<String> value = (ArrayList<String>) data.getList(getPath(), getDefaultValue());
			AdvancedCorePlugin.getInstance().extraDebug(reward.getRewardName() + ": Checking " + getPath() + ", value: "
					+ ArrayUtils.getInstance().makeStringList(value));
			return onRequirementsRequest(reward, user, value, rewardOptions);

		}
		return true;
	}

	public abstract boolean onRequirementsRequest(Reward reward, User user, ArrayList<String> num,
			RewardOptions rewardOptions);

}
