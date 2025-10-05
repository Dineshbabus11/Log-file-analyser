package app;

import app.LogEntry;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.nio.file.*;
import java.util.regex.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;


@WebServlet("/uploadPath")
public class UploadPath extends HttpServlet {
    public static HashMap<String, Long> map = new HashMap<>();
    public static ExecutorService executor;

    public void init() throws ServletException {
        super.init();
        executor = Executors.newSingleThreadExecutor();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html");
        String dir = req.getParameter("path");
        PrintWriter out = res.getWriter();
        if (dir == null || dir.trim().isEmpty()) {
            out.println("Provide valid file path!");
            return;
        }
        executor.submit(() -> startWatch(dir.trim()));
        res.sendRedirect("matchedLogs");
        out.close();
    }

    public static void startWatch(String dir) {
        try {
            WatchService ws = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(dir);
            path.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            File folder = new File(dir);
            for (File f : folder.listFiles()) {
                if(f.isFile()){
                    long lines=Files.lines(f.toPath()).count();
                    map.put(f.getAbsolutePath(),lines);
                }
            }

            while (true){
                WatchKey wk = ws.take();
                for (WatchEvent<?> i : wk.pollEvents()) {
                    Path file = path.resolve((Path) i.context());
                    if (i.kind() == StandardWatchEventKinds.ENTRY_MODIFY || i.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        readFile(file.toFile());
                    }
                }
                wk.reset();
            }
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void readFile(File file) {
        try {
            long lastLine = map.getOrDefault(file.getAbsolutePath(), 0L);
            long currLine = 0;
            try (LineNumberReader lnr = new LineNumberReader(new FileReader(file))) {
                String line;
                while ((line = lnr.readLine()) != null) {
                    currLine = lnr.getLineNumber();
                    if (currLine > lastLine) {
                        LogEntry logEntry = ParseMatchLog.parseLogLine(line);
                        if (logEntry != null) {
                            try (Connection con = DBconnect.connect()) {
                                if (matchesAnyRule(con, logEntry)) {
                                    indexLogEntryToES(file.getName(), logEntry, con);
                                }
                            } 
							catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            map.put(file.getAbsolutePath(), currLine);
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void indexLogEntryToES(String fileName, LogEntry logEntry, Connection con) throws SQLException, IOException {
        StringBuilder matchedRulesBuilder = new StringBuilder();
        List<Integer> matchedRuleIds = new ArrayList<>();
        List<String> matchedRuleNames = new ArrayList<>();

        try (PreparedStatement psRules = con.prepareStatement("SELECT id, name FROM rules");
             ResultSet rsRules = psRules.executeQuery()) {
            while (rsRules.next()) {
                int ruleId = rsRules.getInt("id");
                String ruleName = rsRules.getString("name");
                if (ParseMatchLog.matchesRule(con, ruleId, logEntry)) {
                    matchedRuleIds.add(ruleId);
                    matchedRuleNames.add(ruleName);
                    if (matchedRulesBuilder.length() > 0) {
                        matchedRulesBuilder.append(", ");
                    }
                    matchedRulesBuilder.append(ruleName);
                }
            }
        }
        logEntry.matchedRuleNames = matchedRulesBuilder.toString();
        RestHighLevelClient client = ESClient.getClient();
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("id", logEntry.id);
        jsonMap.put("filename", fileName);
        jsonMap.put("time", logEntry.time);
        jsonMap.put("date", logEntry.date);
        jsonMap.put("logger", logEntry.logger);
        jsonMap.put("level", logEntry.level);
        jsonMap.put("code", logEntry.code);
        jsonMap.put("message", logEntry.message);
        jsonMap.put("matchedRuleIds", matchedRuleIds);
        jsonMap.put("matchedRuleNames", matchedRuleNames);
		jsonMap.put("timestamp", new java.util.Date());

        IndexRequest request = new IndexRequest("logs").source(jsonMap, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    private static boolean matchesAnyRule(Connection con, LogEntry entry) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM rules")) {
            while (rs.next()) {
                int ruleId = rs.getInt("id");
                if (ParseMatchLog.matchesRule(con, ruleId, entry)) {
                    return true;
                }
            }
        }
        return false;
    }
	
	public static void clearOldLogs(){
		try{
			RestHighLevelClient client=ESClient.getClient();
			DeleteByQueryRequest request=new DeleteByQueryRequest("logs");
			request.setQuery(QueryBuilders.matchAllQuery());
			client.deleteByQuery(request, RequestOptions.DEFAULT);
		}
		catch(Exception e){
			System.out.println(e);
		}
	}

    public void destroy() {
        executor.shutdownNow();
        super.destroy();
    }
}
