package app;
import java.util.concurrent.*;
import java.sql.*;

public class ApiWatcherManager{
    private static final ConcurrentHashMap<String, Future<?>> watchers=new ConcurrentHashMap<>();
    private static final ExecutorService executor=Executors.newCachedThreadPool();

    public static synchronized void startTracking(String apiUrl,String indexName){
        if (!watchers.containsKey(apiUrl)){
            Future<?> f=executor.submit(new ApiWatcher(apiUrl,indexName));
            watchers.put(apiUrl,f);
            try(Connection con=DBconnect.connect();
                 PreparedStatement ps=con.prepareStatement(
                     "INSERT INTO watched_apis(api_url, index_name, enabled) VALUES (?, ?, TRUE) "+
                     "ON CONFLICT(api_url) DO UPDATE SET enabled = TRUE")){
                ps.setString(1,apiUrl);
                ps.setString(2,indexName);
                ps.executeUpdate();
            } 
			catch(Exception e){ 
				e.printStackTrace(); 
			}
        }
    }
    public static synchronized void stopTracking(String apiUrl){
        Future<?> f=watchers.get(apiUrl);
        if(f!=null && !f.isDone()){
            f.cancel(true);
            watchers.remove(apiUrl);
        }
        try(Connection con=DBconnect.connect();
            PreparedStatement ps=con.prepareStatement("UPDATE watched_apis SET enabled = FALSE WHERE api_url = ?")) {
            ps.setString(1,apiUrl);
            ps.executeUpdate();
        } 
		catch(Exception e){ 
			e.printStackTrace(); 
		}
    }
}
