package app;

import javax.servlet.*;
import java.sql.*;
import javax.servlet.annotation.*;
import java.util.*;

@WebListener
public class StartupListener implements ServletContextListener {
	private Timer timer;
    public void contextInitialized(ServletContextEvent sce) {
        try(Connection con = DBconnect.connect()) {
            try(PreparedStatement ps = con.prepareStatement(
                    "SELECT path, index_name FROM watched_paths WHERE enabled = TRUE");
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    String path = rs.getString("path");
                    String indexName = rs.getString("index_name");
                    MultiAppPathWatcherManager.startTracking(path, indexName);
                }
            }
            
        } 
		catch(Exception e) {
            e.printStackTrace();
        }
		
		timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    TokenManager.cleanupExpiredTokens();
                } 
				catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 60 * 60 * 1000);
    }
}
