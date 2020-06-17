package com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.ConfigurationSection;

import com.Ben12345rocks.VotingPlugin.AdvancedCore.AdvancedCorePlugin;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Data.ServerData;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UUID;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserManager.UserManager;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.mysql.api.queries.Query;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.sql.Column;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.sql.DataType;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.ArrayUtils;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.CompatibleCacheBuilder;
import com.Ben12345rocks.VotingPlugin.AdvancedCore.Util.Misc.PlayerUtils;
import com.google.common.cache.CacheLoader;

import lombok.Getter;

public class MySQL {
	private com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.mysql.api.MySQL mysql;

	private List<String> columns = Collections.synchronizedList(new ArrayList<String>());

	// private HashMap<String, ArrayList<Column>> table;

	ConcurrentMap<String, ArrayList<Column>> table = CompatibleCacheBuilder.newBuilder().concurrencyLevel(6)
			.build(new CacheLoader<String, ArrayList<Column>>() {

				@Override
				public ArrayList<Column> load(String key) {
					return getExactQuery(new Column("uuid", key, DataType.STRING));
				}
			});

	// ConcurrentMap<String, ArrayList<Column>> table = new
	// ConcurrentHashMap<String, ArrayList<Column>>();

	@Getter
	private ConcurrentLinkedQueue<String> query = new ConcurrentLinkedQueue<String>();

	private String name;

	private Set<String> uuids = ConcurrentHashMap.newKeySet();

	private Set<String> names = ConcurrentHashMap.newKeySet();

	private boolean useBatchUpdates = true;

	private int maxSize = 0;

	private Object object1 = new Object();

	private Object object2 = new Object();

	private Object object3 = new Object();

	private Object object4 = new Object();

	private List<String> intColumns;

	public MySQL(String tableName, ConfigurationSection section) {
		intColumns = Collections.synchronizedList(ServerData.getInstance().getIntColumns());

		String tablePrefix = section.getString("Prefix");
		String hostName = section.getString("Host");
		int port = section.getInt("Port");
		String user = section.getString("Username");
		String pass = section.getString("Password");
		String database = section.getString("Database");
		long lifeTime = section.getLong("MaxLifeTime", -1);
		int maxThreads = section.getInt("MaxConnections", 1);
		if (maxThreads < 1) {
			maxThreads = 1;
		}
		boolean useSSL = section.getBoolean("UseSSL", false);
		this.maxSize = section.getInt("MaxSize", -1);
		if (!section.getString("Name", "").isEmpty()) {
			tableName = section.getString("Name", "");
		}

		String str = section.getString("Line", "");

		if (maxSize >= 0) {
			table = CompatibleCacheBuilder.newBuilder().concurrencyLevel(6).expireAfterAccess(20, TimeUnit.MINUTES)
					.maximumSize(maxSize).build(new CacheLoader<String, ArrayList<Column>>() {

						@Override
						public ArrayList<Column> load(String key) {
							return getExactQuery(new Column("uuid", key, DataType.STRING));
						}
					});
		}

		name = tableName;
		if (tablePrefix != null) {
			name = tablePrefix + tableName;
		}
		mysql = new com.Ben12345rocks.VotingPlugin.AdvancedCore.UserStorage.mysql.api.MySQL(maxThreads);
		if (!mysql.connect(hostName, "" + port, user, pass, database, useSSL, lifeTime, str)) {
			AdvancedCorePlugin.getInstance().getLogger().warning("Failed to connect to MySQL");
		}
		try {
			Query q = new Query(mysql, "USE " + database + ";");
			q.executeUpdateAsync();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String sql = "CREATE TABLE IF NOT EXISTS " + getName() + " (";
		sql += "uuid VARCHAR(37),";
		sql += "PRIMARY KEY ( uuid )";
		sql += ");";
		Query query;
		try {
			query = new Query(mysql, sql);

			query.executeUpdateAsync();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		loadData();

		// tempoary to improve performance from old tables
		// addToQue("ALTER TABLE " + getName() + " MODIFY uuid VARCHAR(37);");
		alterColumnType("uuid", "VARCHAR(37)");

		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				updateBatch();
			}

		}, 10 * 1000, 500);

		AdvancedCorePlugin.getInstance().debug("UseBatchUpdates: " + isUseBatchUpdates());

	}

	public void addColumn(String column, DataType dataType) {
		synchronized (object3) {
			String sql = "ALTER TABLE " + getName() + " ADD COLUMN " + column + " text" + ";";

			AdvancedCorePlugin.getInstance().debug("Adding column: " + column + " Current columns: "
					+ ArrayUtils.getInstance().makeStringList((ArrayList<String>) getColumns()));
			try {
				Query query = new Query(mysql, sql);
				query.executeUpdate();

				getColumns().add(column);

				Column col = new Column(column, dataType);
				for (Entry<String, ArrayList<Column>> entry : table.entrySet()) {
					entry.getValue().add(col);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public void addToQue(String query) {
		// if (!this.query.contains(query)) {
		this.query.add(query);
		// }
	}

	public void alterColumnType(final String column, final String newType) {
		checkColumn(column, DataType.STRING);
		AdvancedCorePlugin.getInstance().debug("Altering column " + column + " to " + newType);
		if (newType.contains("INT")) {
			addToQue(
					"UPDATE " + getName() + " SET " + column + " = '0' where trim(coalesce(" + column + ", '')) = '';");
			if (!intColumns.contains(column)) {
				intColumns.add(column);
				ServerData.getInstance().setIntColumns(intColumns);
			}
		}
		addToQue("ALTER TABLE " + getName() + " MODIFY " + column + " " + newType + ";");
	}

	public void checkColumn(String column, DataType dataType) {
		synchronized (object4) {
			if (!ArrayUtils.getInstance().containsIgnoreCase((ArrayList<String>) getColumns(), column)) {
				if (!ArrayUtils.getInstance().containsIgnoreCase(getColumnsQueury(), column)) {
					addColumn(column, dataType);
				}
			}
		}
	}

	public void clearCache() {
		AdvancedCorePlugin.getInstance().debug("Clearing cache");
		table.clear();
		clearCacheBasic();
	}

	public void clearCache(String uuid) {
		table.remove(uuid);
	}

	public void clearCacheBasic() {
		AdvancedCorePlugin.getInstance().debug("Clearing cache basic");
		columns.clear();
		columns.addAll(getColumnsQueury());
		uuids.clear();
		uuids.addAll(getUuidsQuery());
		names.clear();
		names.addAll(getNamesQuery());
	}

	public void close() {
		mysql.disconnect();
	}

	public boolean containsKey(String uuid) {
		if (table.containsKey(uuid)) {
			return true;
		}
		return false;
	}

	public boolean containsKeyQuery(String index) {
		String sql = "SELECT uuid FROM " + getName() + ";";
		try {
			Query query = new Query(mysql, sql);

			ResultSet rs = query.executeQuery();
			while (rs.next()) {
				if (rs.getString("uuid").equals(index)) {
					return true;
				}
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public boolean containsUUID(String uuid) {
		if (table.containsKey(uuid) || uuids.contains(uuid)) {
			return true;
		}
		return false;
	}

	public void deletePlayer(String uuid) {
		String q = "DELETE FROM " + getName() + " WHERE uuid='" + uuid + "';";
		uuids.remove(uuid);
		names.remove(PlayerUtils.getInstance().getPlayerName(UserManager.getInstance().getUser(new UUID(uuid)), uuid));
		this.query.add(q);
		removePlayer(uuid);
		clearCacheBasic();

	}

	public List<String> getColumns() {
		if (columns == null || columns.size() == 0) {
			loadData();
		}
		return columns;
	}

	public ArrayList<String> getColumnsQueury() {
		ArrayList<String> columns = new ArrayList<String>();
		try {
			Query query = new Query(mysql, "SELECT * FROM " + getName() + ";");

			ResultSet rs = query.executeQuery();

			ResultSetMetaData metadata = rs.getMetaData();
			int columnCount = 0;
			if (metadata != null) {
				columnCount = metadata.getColumnCount();

				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					columns.add(columnName);
				}
				return columns;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return columns;

	}

	public ArrayList<Column> getExact(String uuid) {
		// AdvancedCorePlugin.getInstance().debug("Get Exact: " + uuid);
		loadPlayerIfNeeded(uuid);
		// AdvancedCorePlugin.getInstance().debug("test one: " + uuid);
		return table.get(uuid);
	}

	public ArrayList<Column> getExactQuery(Column column) {
		ArrayList<Column> result = new ArrayList<>();
		String query = "SELECT * FROM " + getName() + " WHERE `" + column.getName() + "`=?" + ";";

		try {
			Query sql = new Query(mysql, query);

			sql.setParameter(1, column.getValue().toString());

			ResultSet rs = sql.executeQuery();
			rs.next();
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				String columnName = rs.getMetaData().getColumnLabel(i);
				Column rCol = null;
				if (intColumns.contains(columnName)) {
					rCol = new Column(columnName, DataType.INTEGER);
				} else {
					rCol = new Column(columnName, DataType.STRING);
				}
				// System.out.println(i + " " +
				// rs.getMetaData().getColumnLabel(i));
				rCol.setValue(rs.getString(i));
				// System.out.println(rCol.getValue());
				result.add(rCol);
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		for (String col : getColumns()) {
			result.add(new Column(col, DataType.STRING));
		}
		return result;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public String getName() {
		return name;
	}

	public Set<String> getNames() {
		if (names == null || names.size() == 0) {
			names.clear();
			names.addAll(getNamesQuery());
			return names;
		}
		return names;
	}

	public ArrayList<String> getNamesQuery() {
		ArrayList<String> uuids = new ArrayList<String>();

		checkColumn("PlayerName", DataType.STRING);
		ArrayList<Column> rows = getRowsNameQuery();
		if (rows != null) {
			for (Column c : rows) {
				if (c.getValue() != null) {
					uuids.add((String) c.getValue());
				}
			}
		}

		return uuids;
	}

	public ArrayList<Column> getRowsNameQuery() {
		ArrayList<Column> result = new ArrayList<Column>();
		String sql = "SELECT PlayerName FROM " + getName() + ";";

		try {
			Query query = new Query(mysql, sql);
			ResultSet rs = query.executeQuery();

			while (rs.next()) {
				Column rCol = new Column("PlayerName", rs.getString("PlayerName"), DataType.STRING);
				result.add(rCol);
			}
		} catch (SQLException e) {
		}

		return result;
	}

	public ArrayList<Column> getRowsQuery() {
		ArrayList<Column> result = new ArrayList<Column>();
		String sql = "SELECT uuid FROM " + getName() + ";";

		try {
			Query query = new Query(mysql, sql);
			ResultSet rs = query.executeQuery();

			while (rs.next()) {
				Column rCol = new Column("uuid", rs.getString("uuid"), DataType.STRING);
				result.add(rCol);
			}
		} catch (SQLException e) {
			return null;
		}

		return result;
	}

	public Set<String> getUuids() {
		if (uuids == null || uuids.size() == 0) {
			uuids.clear();
			uuids.addAll(getUuidsQuery());
			return uuids;
		}
		return uuids;
	}

	public ArrayList<String> getUuidsQuery() {
		ArrayList<String> uuids = new ArrayList<String>();

		ArrayList<Column> rows = getRowsQuery();
		for (Column c : rows) {
			if (c.getValue() != null) {
				uuids.add((String) c.getValue());
			}
		}

		return uuids;
	}

	public void insert(String index, String column, Object value, DataType dataType) {
		insertQuery(index, column, value, dataType);

	}

	public void insertQuery(String index, String column, Object value, DataType dataType) {
		uuids.add(index);
		String query = "INSERT " + getName() + " ";

		query += "set uuid='" + index + "', ";
		query += column + "='" + value.toString() + "';";
		// AdvancedCorePlugin.getInstance().extraDebug(query);

		try {
			new Query(mysql, query).executeUpdate();
			names.add(
					PlayerUtils.getInstance().getPlayerName(UserManager.getInstance().getUser(new UUID(index)), index));
		} catch (Exception e) {
			AdvancedCorePlugin.getInstance().debug(e);
			AdvancedCorePlugin.getInstance().debug("Failed to insert player " + index);
		}

	}

	public boolean isIntColumn(String key) {
		return intColumns.contains(key);
	}

	public boolean isUseBatchUpdates() {
		return useBatchUpdates;
	}

	public void loadData() {
		columns = getColumnsQueury();

		try {
			Connection con = mysql.getConnectionManager().getConnection();
			useBatchUpdates = con.getMetaData().supportsBatchUpdates();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadPlayer(String uuid) {
		table.put(uuid, getExactQuery(new Column("uuid", uuid, DataType.STRING)));
	}

	public void loadPlayerIfNeeded(String uuid) {
		if (!containsKey(uuid)) {
			// AdvancedCorePlugin.getInstance().debug("Caching " + uuid);
			synchronized (object1) {
				loadPlayer(uuid);
			}
		}
	}

	public void playerJoin(String uuid) {
		if (AdvancedCorePlugin.getInstance().getOptions().isClearCacheOnJoin()) {
			removePlayer(uuid);
		}
	}

	public void removePlayer(String uuid) {
		table.remove(uuid);
	}

	public void update(String index, String column, Object value, DataType dataType) {
		if (value == null) {
			AdvancedCorePlugin.getInstance().extraDebug("Mysql value null: " + column);
			return;
		}
		checkColumn(column, dataType);
		synchronized (object2) {
			if (getUuids().contains(index) || containsKeyQuery(index)) {
				for (Column col : getExact(index)) {
					if (col.getName().equals(column)) {
						col.setValue(value);
					}
				}

				String query = "UPDATE " + getName() + " SET ";

				if (dataType == DataType.STRING) {
					query += column + "='" + value.toString() + "'";
				} else {
					query += column + "=" + value;

				}
				query += " WHERE `uuid`=";
				query += "'" + index + "';";

				addToQue(query);
			} else {
				insert(index, column, value, dataType);
			}
		}

	}

	public void updateBatch() {
		if (query.size() > 0) {
			AdvancedCorePlugin.getInstance().extraDebug("Query Size: " + query.size());
			String sql = "";
			while (query.size() > 0) {
				String text = query.poll();
				if (!text.endsWith(";")) {
					text += ";";
				}
				sql += text;
			}

			try {
				if (useBatchUpdates) {
					Connection conn = mysql.getConnectionManager().getConnection();
					Statement st = conn.createStatement();
					for (String str : sql.split(";")) {
						st.addBatch(str);
					}
					st.executeBatch();
					st.close();
					conn.close();
				} else {
					for (String text : sql.split(";")) {
						try {
							Query query = new Query(mysql, text);
							query.executeUpdateAsync();
						} catch (SQLException e) {
							AdvancedCorePlugin.getInstance().getLogger().severe("Error occoured while executing sql: "
									+ e.toString() + ", turn debug on to see full stacktrace");
							AdvancedCorePlugin.getInstance().debug(e);
						}
					}
				}
			} catch (SQLException e1) {
				AdvancedCorePlugin.getInstance().extraDebug("Failed to send query: " + sql);
				e1.printStackTrace();
			}
		}

	}

	public void updateBatchShutdown() {
		if (query.size() > 0) {
			AdvancedCorePlugin.getInstance().extraDebug("Query Size: " + query.size());
			String sql = "";
			while (query.size() > 0) {
				String text = query.poll();
				if (!text.endsWith(";")) {
					text += ";";
				}
				sql += text;
			}

			try {
				if (useBatchUpdates) {
					Connection conn = mysql.getConnectionManager().getConnection();
					Statement st = conn.createStatement();
					for (String str : sql.split(";")) {
						st.addBatch(str);
					}
					st.executeBatch();
					st.close();
					conn.close();
				} else {
					for (String text : sql.split(";")) {
						try {
							Query query = new Query(mysql, text);
							query.executeUpdate();
						} catch (SQLException e) {
							AdvancedCorePlugin.getInstance().getLogger().severe("Error occoured while executing sql: "
									+ e.toString() + ", turn debug on to see full stacktrace");
							AdvancedCorePlugin.getInstance().debug(e);
						}
					}
				}
			} catch (SQLException e1) {
				AdvancedCorePlugin.getInstance().extraDebug("Failed to send query: " + sql);
				e1.printStackTrace();
			}

			try {
				mysql.getThreadPool().awaitTermination(3, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
