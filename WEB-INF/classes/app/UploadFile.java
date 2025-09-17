package app;

import java.io.*;
import java.sql.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

@WebServlet("/uploadLog")
@MultipartConfig
public class UploadFile extends HttpServlet {
    private static final int PAGE_SIZE = 10;

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Part filePart = req.getPart("logFile");
        String fileName = null;

        if (filePart != null) {
            fileName = filePart.getSubmittedFileName();
            String folderPath = getServletContext().getRealPath("") + File.separator + "folder";
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdir();
            File file = new File(folder, fileName);
            filePart.write(file.getAbsolutePath());
        } else {
            fileName = req.getParameter("fileName");
            if (fileName == null || fileName.isEmpty()) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "No fileName provided");
                return;
            }
        }

        int totalLines = 0, errorCount = 0, warningCount = 0, infoCount = 0;

        try (Connection con = DBconnect.connect()) {
            if (filePart != null) {
                try (PreparedStatement psDel = con.prepareStatement("DELETE FROM log")) {
                    psDel.executeUpdate();
                }

                String folderPath = getServletContext().getRealPath("") + File.separator + "folder";
                File logFile = new File(folderPath, fileName);

                try (BufferedReader br = new BufferedReader(new FileReader(logFile));
                     PreparedStatement psInsert = con.prepareStatement(
                         "INSERT INTO log (filename, log_time, log_date, logger, level, code, message) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                    String line;
                    while ((line = br.readLine()) != null) {
                        totalLines++;
                        LogEntry logEntry = parseLogLine(line);
                        if (logEntry == null){
							continue;
						}

                        if (matchesAnyRule(con, logEntry)) {
                            switch (logEntry.level.toUpperCase()) {
                                case "ERROR": 
									errorCount++; 
									break;
                                case "WARNING": 
									warningCount++; 
									break;
                                case "INFO": 
									infoCount++; 
									break;
                            }
                            psInsert.setString(1, fileName);
                            psInsert.setString(2, logEntry.time);
                            psInsert.setString(3, logEntry.date);
                            psInsert.setString(4, logEntry.logger);
                            psInsert.setString(5, logEntry.level);
                            psInsert.setString(6, logEntry.code);
                            psInsert.setString(7, logEntry.message);
                            psInsert.addBatch();
                        }
                    }
                    psInsert.executeBatch();
                }
            }

            int page = 1;
            String pageParam = req.getParameter("page");
            if (pageParam != null) {
                try {
                    page = Integer.parseInt(pageParam);
                    if (page < 1) page = 1;
                } catch (NumberFormatException ignored) {}
            }
            int offset = (page - 1) * PAGE_SIZE;

            int totalRows = 0;
            try (PreparedStatement psCount = con.prepareStatement("SELECT COUNT(*) FROM log WHERE filename = ?")) {
                psCount.setString(1, fileName);
                try (ResultSet rsCount = psCount.executeQuery()) {
                    if (rsCount.next()) totalRows = rsCount.getInt(1);
                }
            }
            int totalPages = (int) Math.ceil((double) totalRows / PAGE_SIZE);

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT log_time, log_date, logger, level, code, message FROM log WHERE filename = ? ORDER BY id LIMIT ? OFFSET ?")) {
                ps.setString(1, fileName);
                ps.setInt(2, PAGE_SIZE);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    req.setAttribute("logResultSet", rs);
                    req.setAttribute("fileName", fileName);
                    req.setAttribute("totalLines", totalRows);
                    req.setAttribute("errorCount", errorCount);
                    req.setAttribute("warningCount", warningCount);
                    req.setAttribute("infoCount", infoCount);
                    req.setAttribute("currentPage", page);
                    req.setAttribute("totalPages", totalPages);
                    RequestDispatcher rd = req.getRequestDispatcher("results.jsp");
                    rd.forward(req, res);
                }
            }
        } 
		catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public static class LogEntry {
        String time, date, logger, level, code, message;
        LogEntry(String time, String date, String logger, String level, String code, String message) {
            this.time = time; 
			this.date = date; 
			this.logger = logger;
            this.level = level; 
			this.code = code; 
			this.message = message;
        }
    }

    public LogEntry parseLogLine(String line) {
        try {
            String[] parts = line.split("\\|");
            for (int i=0;i<parts.length;i++) {
                parts[i]=parts[i].trim();
                if (parts[i].startsWith("[")&&parts[i].endsWith("]")){
                    parts[i] = parts[i].substring(1, parts[i].length() - 1);
				}
            }
            String time = "", date = "", logger = "", level = "", code = "", message = "";
            for (String part : parts) {
                if (part.matches("\\d{2}:\\d{2}:\\d{2}:\\d{3}")){
					time = part;
				} 
                else if (part.matches("\\d{4}-\\d{2}-\\d{2}")){
					date = part;
				} 
                else if (part.equalsIgnoreCase("INFO") || part.equalsIgnoreCase("ERROR") || part.equalsIgnoreCase("WARNING")){
					level = part;
				}
                else if (part.matches("\\d+")){
					code = part;
				} 
                else if (part.contains(":")){
					message = part.substring(part.indexOf(":") + 1).trim();
				} 
                else{
					logger = part;
				} 
            }
            if (time.isEmpty() || date.isEmpty() || logger.isEmpty()){
				return null;
			} 
            return new LogEntry(time, date, logger, level, code, message);
        } 
		catch (Exception e) {
            return null;
        }
    }

    public boolean matchesAnyRule(Connection con, LogEntry entry) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM rules")) {
            while (rs.next()) {
                int ruleId = rs.getInt("id");
                if (matchesRule(con, ruleId, entry)){
					return true;
				} 
            }
        }
        return false;
    }

    public boolean matchesRule(Connection con, int ruleId, LogEntry entry) throws SQLException {
        ConditionNode root=buildNode(con,ruleId);
		for(ConditionNode i:root.children){
			if(evaluate(i,entry)){
				return true;
			}
		}
		return false;
    }
	
	public ConditionNode buildNode(Connection con,int ruleId) throws SQLException{
		String sql = "SELECT * FROM rule_conditions WHERE rule_id=? ORDER BY id ASC";
		HashMap<Integer,ConditionNode> map=new HashMap<>();
		ConditionNode root=new ConditionNode();
		root.id=0;
		map.put(0,root);
		try (PreparedStatement ps = con.prepareStatement(sql)){
			ps.setInt(1, ruleId);
			ResultSet rs=ps.executeQuery();
			while(rs.next()){
				ConditionNode node=new ConditionNode();
				node.id = rs.getInt("id");
                int parentId = rs.getObject("parent_id")==null?0:rs.getInt("parent_id");
                node.field = rs.getString("field");
                node.pattern = rs.getString("pattern");
                node.operator = rs.getString("operator");
                node.logicOp = rs.getString("logic_op");
                nodeMap.put(node.id, node);
                nodeMap.get(parentId).children.add(node);
			}
		}
		return root;
	}
	
	public boolean evaluate(ConditionNode node,LogEntry entry){
		if(node.isGroup()){
			String logic=node.logicOp==null?"AND":node.logicOp.toUpperCase();
			if("AND".equals(logic)){
				for(ConditionNode i:node.children){
					if(!evaluate(i,entry)){
						return false;
					}
				}
				return true;
			}
			else if("OR".equals(logic)){
				for(ConditionNode i:node.children){
					if(evaluate(i,entry)){
						return true;
					}
				}
				return false;
			}
			return true;
		}
		else{
			return evaluateSingle(node,entry);
		}
	}
	
	public boolean evaluateSingle(ConditionNode node,LogEntry entry){
		String val="";
		switch(node.field.toLowerCase()){
			case "time": 
				val = entry.time; 
				break;
            case "date": 
				val = entry.date; 
				break;
            case "logger": 
				val = entry.logger; 
				break;
            case "level": 
				val = entry.level; 
				break;
            case "code": 
				val = entry.code; 
				break;
            case "message": 
				val = entry.message; 
				break;
		}
		switch((node.operator!=null)?node.operator.toLowerCase():""){
			case "equals": 
				return val.equals(node.pattern);
            case "greater": 
				return val.compareTo(node.pattern)>0;
            case "less": 
				return val.compareTo(node.pattern)<0;
            case "greater_equal": 
				return val.compareTo(node.pattern)>=0;
            case "less_equal": 
				return val.compareTo(node.pattern)<=0;
            case "regex": 
				return Pattern.matches(node.pattern,val);
            default: 
				return false;
		}
	}
	
}
