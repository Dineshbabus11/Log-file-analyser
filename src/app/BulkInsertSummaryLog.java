package app;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;


import java.io.IOException;

public class BulkInsertSummaryLog {

    private static final String INDEX_NAME = "path_c__users_arul_selvan_downloads_log_summary_txt";

    public static void main(String[] args) {
        RestHighLevelClient client = ESClient.getClient();

        int totalDocuments = 1_000_000;
        int batchSize = 1000;
        int batches = totalDocuments / batchSize;

        try {
            for (int batch = 0; batch < batches; batch++) {
                BulkRequest bulkRequest = new BulkRequest();

                for (int i = 0; i < batchSize; i++) {
                    int docId = batch * batchSize + i + 1;
                    String jsonString = "{"
                            + "\"date\":\"2025-09-12\","
                            + "\"fileName\":\"C:\\\\Users\\\\Arul selvan\\\\Downloads\\\\log\\\\summary.txt\","
                            + "\"code\":\"\","
                            + "\"level\":\"INFO\","
                            + "\"logger\":\"ADMangLogger\","
                            + "\"time\":\"15:24:37:473\","
                            + "\"message\":\"Sample log message " + docId + "\","
                            + "\"matchedRuleIds\":[1,2],"
                            + "\"matchedRuleNames\":[\"befor 2025\", \"default\"],"
                            + "\"timestamp\":\"2025-10-01T06:16:15.541Z\""
                            + "}";

                    IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                            .id(String.valueOf(docId))
                            .source(jsonString, XContentType.JSON);

                    bulkRequest.add(indexRequest);
                }

                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);

                if (bulkResponse.hasFailures()) {
                    System.err.println("Bulk insert failures at batch " + batch + ": " + bulkResponse.buildFailureMessage());
                } else {
                    System.out.println("Batch " + batch + " inserted successfully.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException ignore) {}
        }
    }
}
