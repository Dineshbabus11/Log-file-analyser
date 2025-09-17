package app;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DBconnect
{
	public static final String URL = "jdbc:postgresql://localhost:5432/logdb";
	public static final String USER = "postgres";
	public static final String PASS = "1234";
	public static Connection connect() throws SQLException
	{
		return DriverManager.getConnection(URL, USER, PASS);
	}
}
