package com.Ben12345rocks.VotingPlugin.AdvancedCore.Rewards;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.User;

import lombok.Getter;
import lombok.Setter;

public abstract class RewardPlaceholderHandle {
	@Getter
	@Setter
	private String key;

	@Getter
	@Setter
	private boolean preProcess = false;

	public RewardPlaceholderHandle(String key) {
		this.key = key;
	}

	public abstract String getValue(Reward reward, User user);

	public RewardPlaceholderHandle preProcess() {
		preProcess = true;
		return this;
	}

}
