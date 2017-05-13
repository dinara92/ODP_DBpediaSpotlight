package db_utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;


public class ODPDatabase {

	String driver = "com.mysql.jdbc.Driver";
	public String url = "jdbc:mysql://localhost:3306/odp_3";

	String user = "root";
	String password = "newpass";

	Connection conn = null;
	Statement stmt = null;

	public boolean connectDB(String url) {

		try {
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);

		} catch (ClassNotFoundException e) {
			System.err.println("LoginDB1 Class : " + e.getMessage());
			return false;
		} catch (SQLException e) {
			System.err.println("LoginDB2 Class : " + e.getMessage());
			return false;
		}
		return true;
	}

	
	public void closeDB() throws SQLException {
		if (conn != null) {
			conn.close();
		}

	}
	public String getDoc(int i) throws SQLException{
		 
		String query = "select sid, title, description from SITE where sid = " + i + ";";

		stmt = conn.createStatement();

		// execute the query, and get a java resultset
		ResultSet rs = stmt.executeQuery(query);
		Document tmp = null;
		String tmp_text = "";
		// iterate through the java resultset
		while (rs.next()) {
			int id = Integer.parseInt(rs.getString("sid"));
			tmp = new Document(id, rs.getString("title"), rs.getString("description"));
			tmp_text = tmp.title + " " + tmp.description;
		}
		stmt.close();
		 
		return tmp_text;
		
	}
	public Integer getCateg(int i) throws SQLException{
		 
		String query = "select cid from SITE where sid = " + i + ";";

		stmt = conn.createStatement();

		// execute the query, and get a java resultset
		ResultSet rs = stmt.executeQuery(query);
		int cid = 0;
		// iterate through the java resultset
		while (rs.next()) {
			cid = Integer.parseInt(rs.getString("cid"));
			
		}
		stmt.close();
		 
		return cid;
		
	}
	public void putDoc(int i, String text) throws SQLException{
		 
		String query = "update SITE set dbpedia_enriched = " + "\'" + text + "\'" + " where sid = " + i + ";";

		stmt = conn.createStatement();

		// execute the query, and get a java resultset
		stmt.executeUpdate(query);
		stmt.close();
		 
		
	}
	public HashSet<Integer> getNonWorldCateg() throws SQLException{
		 
		String query = "select cid from CATEGORY where cname_full like \'%Top/World%\'" + ";";
		stmt = conn.createStatement();

		// execute the query, and get a java resultset
		ResultSet rs = stmt.executeQuery(query);
		HashSet<Integer> cid_hash = new HashSet<Integer>();
		// iterate through the java resultset
		while (rs.next()) {
			cid_hash.add(Integer.parseInt(rs.getString("cid")));
		}
		stmt.close();
		 
		return cid_hash;
		
	}
	
}
