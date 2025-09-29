package app;

import javax.servlet.*;
import java.sql.*;
import javax.servlet.annotation.*;

@WebListener
public class StartupListener implements ServletContextListener{
	public void contextInitialized(ServletContextEvent sce) {
        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement("SELECT path, index_name FROM watched_paths");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String path = rs.getString("path");
                String indexName = rs.getString("index_name");
                MultiAppPathWatcherManager.startTracking(path,indexName);
            }
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    }
}