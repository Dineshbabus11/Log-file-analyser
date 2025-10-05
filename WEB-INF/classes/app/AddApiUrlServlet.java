package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;

@WebServlet("/addApiUrl")
public class AddApiUrlServlet extends HttpServlet{
    protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException, IOException{
        String apiUrl=req.getParameter("newApiUrl").trim();
        if(apiUrl==null||apiUrl.isEmpty()){
            res.sendRedirect("AddApiUrl.jsp");
            return;
        }
        String indexName="api_"+apiUrl.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        try(Connection con=DBconnect.connect()){
            PreparedStatement ps=con.prepareStatement(
                "INSERT INTO watched_apis(api_url, index_name, enabled) VALUES (?, ?, TRUE) "+
                "ON CONFLICT(api_url) DO UPDATE SET enabled=TRUE");
            ps.setString(1,apiUrl);
            ps.setString(2,indexName);
            ps.executeUpdate();
        } 
		catch(Exception e){ 
			e.printStackTrace(); 
		}
        try{
            RestHighLevelClient client=ESClient.getClient();
            client.indices().delete(new DeleteIndexRequest(indexName),RequestOptions.DEFAULT);
        } 
		catch(Exception ignore){
			
		}
        try{
            RestHighLevelClient client=ESClient.getClient();
            client.indices().create(new CreateIndexRequest(indexName),RequestOptions.DEFAULT);
        } 
		catch(Exception e){ 
			e.printStackTrace(); 
		}
        ApiWatcherManager.startTracking(apiUrl,indexName);
        res.sendRedirect("manageApiUrls.jsp");
    }
}
