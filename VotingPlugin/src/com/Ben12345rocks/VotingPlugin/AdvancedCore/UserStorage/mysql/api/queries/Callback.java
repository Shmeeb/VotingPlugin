package com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.mysql.api.queries;

public interface Callback<V extends Object, T extends Throwable> {

	public void call(V result, T thrown);

}
