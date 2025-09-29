package app;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;


public class ESClient{
    private static RestHighLevelClient client;

    public static RestHighLevelClient getClient(){
        if (client==null){
            client=new RestHighLevelClient(RestClient.builder(new HttpHost("localhost",9200,"http")));
        }
        return client;
    }

    public static void closeClient(){
        try{
            if (client != null){
                client.close();
            }
        } 
		catch (Exception e){
            e.printStackTrace();
        }
    }
	
	
}

