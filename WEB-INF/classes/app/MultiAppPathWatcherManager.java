package app;

import java.util.concurrent.*;
import java.sql.*;

public class MultiAppPathWatcherManager {
    private static final ConcurrentHashMap<String, Future<?>> watchers = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static synchronized void startTracking(String path, String indexName) {
        if (!watchers.containsKey(path)) {
            Future<?> f=executor.submit(new MultiAppPathWatcher(path, indexName));
            watchers.put(path, f);
            try(Connection con=DBconnect.connect();
                 PreparedStatement ps=con.prepareStatement(
                     "INSERT INTO watched_paths(path, index_name, enabled) VALUES (?, ?, TRUE) "+
                     "ON CONFLICT(path) DO UPDATE SET enabled = TRUE")){
                ps.setString(1,path);
                ps.setString(2,indexName);
                ps.executeUpdate();
            } 
			catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public static synchronized void stopTracking(String path) {
        Future<?> f=watchers.get(path);
        if (f!=null && !f.isDone()) {
            f.cancel(true);
            watchers.remove(path);
        }
        try(Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement("UPDATE watched_paths SET enabled = FALSE WHERE path = ?")) {
            ps.setString(1, path);
            ps.executeUpdate();
        } 
		catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean isTracking(String path) {
        Future<?> f=watchers.get(path);
        return f!=null && !f.isDone();
    }
}
