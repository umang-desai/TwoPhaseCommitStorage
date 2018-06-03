import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataStoreAccess {

	private Connection con;

	public DataStoreAccess(Connection con) throws IOException {
		this.con = con;
	}

	public DataStoreAccess() {

	}

	public String get(String key) throws SQLException {
		String sql = "SELECT value " + "FROM data " + "WHERE key=\"" + key
				+ "\"";
		//print(sql);
		return executeQuery(sql);
	}

	public void put(String key, String value) throws SQLException {
		String sql = "INSERT OR REPLACE INTO data (key,value) VALUES (?,?)";
		//print(sql);
		executeInsertQuery(sql, key, value);
	}

	public void del(String key) throws SQLException {
		String sql = "DELETE FROM data WHERE key=?";
		//print(sql);
		executeDeleteQuery(sql, key);
	}

	// ------------------------------------------------//

	private String executeQuery(String sql) throws SQLException {
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		String output = null;
		if (rs.next()) {
			output = rs.getString(1);
		}
		return output;
	}

	private void executeInsertQuery(String sql, String key, String value)
			throws SQLException {
		PreparedStatement pstmt = con.prepareStatement(sql);
		pstmt.setString(1, key);
		pstmt.setString(2, value);
		pstmt.executeUpdate();
	}

	private void executeDeleteQuery(String sql, String key) throws SQLException {
		PreparedStatement pstmt = con.prepareStatement(sql);
		pstmt.setString(1, key);
		pstmt.executeUpdate();
	}

	public static void main(String[] args) throws IOException, SQLException {
		String sqlUrl = "jdbc:sqlite:test.db";
		Connection con = DriverManager.getConnection(sqlUrl);
		System.out.println("Connection Established.");

		// Create table, key/value store.
		String createTable = "CREATE TABLE IF NOT EXISTS data "
				+ "(key text PRIMARY KEY,value text);";
		Statement stmt = con.createStatement();
		stmt.execute(createTable);

		// PUT values into table.
		DataStoreAccess st = new DataStoreAccess(con);
	//	st.put("Bansri", "Desai");
		// GET values from table.
		print(st.get("Bansri"));
		// DEL values from table
		//st.del("Bansri");

		print(st.get("Bansri"));

		con.close();
	}

	public static void print(String str) {
		System.out.println(str);
	}
}
