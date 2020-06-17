package com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.ValueRequest;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;

/**
 * The Enum InputMethod.
 */
public enum InputMethod {

	/** The anvil. */
	ANVIL,

	/** The chat. */
	CHAT,

	/** The book. */
	BOOK,

	/** The inventory. */
	INVENTORY,

	SIGN;

	/**
	 * Gets the method.
	 *
	 * @param method
	 *            the method
	 * @return the method
	 */
	public static InputMethod getMethod(String method) {
		for (InputMethod input : values()) {
			if (method.equalsIgnoreCase(input.toString())) {
				return input;
			}
		}
		try {
			return valueOf(AdvancedCorePlugin.getInstance().getOptions().getDefaultRequestMethod());
		} catch (Exception ex) {
			return CHAT;
		}

	}
}
