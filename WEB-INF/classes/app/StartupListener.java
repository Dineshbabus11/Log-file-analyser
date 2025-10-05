package app;

import javax.servlet.*;
import java.sql.*;
import javax.servlet.annotation.*;

@WebListener
public class StartupListener implements ServletContextListener {
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
            try(PreparedStatement ps = con.prepareStatement(
                    "SELECT api_url, api_key, index_name FROM watched_apis WHERE enabled = TRUE");
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    String apiUrl = rs.getString("api_url");
                    String apiKey = rs.getString("api_key");
                    String indexName = rs.getString("index_name");
                    ApiWatcherManager.startTracking(apiUrl, apiKey, indexName);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
