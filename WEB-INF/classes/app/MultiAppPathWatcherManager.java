package app;

import java.util.concurrent.*;
import java.sql.*;

public class MultiAppPathWatcherManager {
    private static final ConcurrentHashMap<String, Future<?>> watchers = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void startTracking(String path, String indexName) {
        if (!watchers.containsKey(path)) {
            Future<?> f = executor.submit(new MultiAppPathWatcher(path, indexName));
            watchers.put(path, f);
			try (Connection con = DBconnect.connect();
				PreparedStatement ps = con.prepareStatement("INSERT INTO watched_paths(path, index_name) VALUES (?, ?) ON CONFLICT(path) DO NOTHING")) {
				ps.setString(1, path);
				ps.setString(2, indexName);
				ps.executeUpdate();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}

        }
    }
}
