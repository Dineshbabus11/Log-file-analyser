package app;

import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.util.*;
import java.awt.Color;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.core.TimeValue;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

@WebServlet("/ExportLogsPdf")
public class ExportLogsPdf extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (!SessionUtils.checkLogin(req, res)) {
			return;
		}
        String indexName = req.getParameter("indexName");
        if (indexName == null || indexName.trim().isEmpty()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "indexName required");
            return;
        }
        RestHighLevelClient client = ESClient.getClient();
        boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        if (!exists) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Index not found");
            return;
        }
        final int BATCH_SIZE = 5000;
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(new MatchAllQueryBuilder()).size(BATCH_SIZE);
        searchRequest.source(sourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(3));
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        String scrollId = searchResponse.getScrollId();
        SearchHit[] hits = searchResponse.getHits().getHits();
        if (hits == null || hits.length == 0) {
            res.sendError(HttpServletResponse.SC_NO_CONTENT, "No logs found in index");
            return;
        }
        Map<String, Object> firstSource = hits[0].getSourceAsMap();
        java.util.List<String> columnKeys = new ArrayList<>(firstSource.keySet());
        res.setContentType("application/pdf");
        res.setHeader("Content-Disposition", "attachment; filename=\"logs.pdf\"");
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(doc, res.getOutputStream());
            doc.open();
            doc.add(new Paragraph("Logs for Index: " + indexName));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(columnKeys.size());
            table.setWidthPercentage(100);
            for (String col : columnKeys) {
                PdfPCell headerCell = new PdfPCell(new Phrase(col));
                headerCell.setBackgroundColor(new Color(200, 200, 200));
                table.addCell(headerCell);
            }
            long totalCount = 0;
            while (hits != null && hits.length > 0) {
                for (SearchHit hit : hits) {
                    Map<String, Object> log = hit.getSourceAsMap();
                    for (String field : columnKeys) {
                        Object value = log.get(field);
                        if (value instanceof java.util.List) {
                            java.util.List<?> valList = (java.util.List<?>) value;
                            StringBuilder sb = new StringBuilder();
                            for(int i=0;i<valList.size();i++){
                                sb.append(String.valueOf(valList.get(i)));
                                if (i<valList.size()-1) {
                                    sb.append(", ");
                                }
                            }
                            table.addCell(sb.toString());
                        } 
						else{
                            table.addCell(value != null ? value.toString() : "-");
                        }
                    }
                    totalCount++;
                    if(totalCount%5000==0){
                        doc.add(table);
                        table=new PdfPTable(columnKeys.size());
                        table.setWidthPercentage(100);
                    }
                }
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(3));
                SearchResponse scrollResponse=client.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId=scrollResponse.getScrollId();
                hits = scrollResponse.getHits().getHits();
            }
            if(table.size()>0){
                doc.add(table);
            }
            if(scrollId != null){
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(scrollId);
                client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            }
            doc.add(new Paragraph("\nTotal Entries Exported: " + totalCount));
        } 
		catch (DocumentException e) {
            throw new ServletException("Error while generating PDF", e);
        } 
		finally {
            doc.close();
        }
    }
}
