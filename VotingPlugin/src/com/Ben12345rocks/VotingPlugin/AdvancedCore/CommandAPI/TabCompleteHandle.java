package com.Ben12345rocks.VotingPlugin.AdvancedCore.CommandAPI;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

public abstract class TabCompleteHandle {
	@Getter
	@Setter
	private String toReplace;

	@Getter
	@Setter
	private ArrayList<String> replace = new ArrayList<String>();

	public TabCompleteHandle(String toReplace) {
		this.toReplace = toReplace;
		reload();
	}

	public TabCompleteHandle(String toReplace, ArrayList<String> replace) {
		this.toReplace = toReplace;
		this.replace = replace;
	}

	public abstract void reload();

	public abstract void updateReplacements();
}
