package app;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

class ParseMatchLog{
	
	public static LogEntry parseLogLine(String line){
		try{
			String[] parts=line.split("\\|");
			for(int i=0;i<parts.length;i++) {
				parts[i]=parts[i].trim();
				if(parts[i].startsWith("[") && parts[i].endsWith("]")){
					parts[i]=parts[i].substring(1,parts[i].length()- 1);
				}
			}
			String time = "", date = "", logger = "", level = "", code = "", message = "";
			for(String part:parts) {
				if(part.matches("\\d{2}:\\d{2}:\\d{2}:\\d{3}")) {
					time=part;
				} 
				else if(part.matches("\\d{4}-\\d{2}-\\d{2}")) {
					date = part;
				} 
				else if(part.equalsIgnoreCase("INFO") || part.equalsIgnoreCase("ERROR") || part.equalsIgnoreCase("WARNING")) {
					level = part;
				} 
				else if(part.matches("\\d+")) {
					code = part;
				} 
				else if(part.contains(":")) {
					message = part.substring(part.indexOf(":") + 1).trim();
				} 
				else {
					logger = part;
				}
			}
			if(time.isEmpty() || date.isEmpty() || logger.isEmpty()) {
				return null;
			}
			return new LogEntry(time, date, logger, level, code, message, null);
		} 
		catch(Exception e) {
			return null;
		}
	}
	
	public static boolean matchesRule(Connection con, int ruleId, LogEntry entry) throws SQLException {
		String sql = "SELECT field, pattern, operator, logic_op FROM rule_conditions WHERE rule_id=? ORDER BY exec_order ASC";
		try(PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, ruleId);
			try(ResultSet rs = ps.executeQuery()) {
				boolean result = false;
				boolean first = true;
				while(rs.next()) {
					String field = rs.getString("field");
					String pattern = rs.getString("pattern");
					String operator = rs.getString("operator");
					String logicOp = rs.getString("logic_op");
					String fieldValue = "";
					switch(field.toLowerCase()) {
						case "time": 
							fieldValue = entry.time;
							break;
						case "date": 
							fieldValue = entry.date; 
							break;
						case "logger": 
							fieldValue = entry.logger; 
							break;
						case "level": 
							fieldValue = entry.level; 
							break;
						case "code": 
							fieldValue = entry.code; 
							break;
						case "message": 
							fieldValue = entry.message; 
							break;
					}
					boolean match = false;
					if("date".equals(field) || "time".equals(field)) {
						switch(operator) {
							case "equals": 
								match = fieldValue.equals(pattern); 
								break;
							case "greater": 
								match = fieldValue.compareTo(pattern) > 0; 
								break;
							case "less": 
								match = fieldValue.compareTo(pattern) < 0; 
								break;
							case "greater_equal": 
								match = fieldValue.compareTo(pattern) >=0; 
								break;
							case "less_equal": 
								match = fieldValue.compareTo(pattern) <=0; 
								break;
						}
					} 
					else {
						match = Pattern.matches(pattern, fieldValue);
					}
					if(first) {
						result = match;
						first = false;
					} 
					else {
						if("AND".equalsIgnoreCase(logicOp)) {
							result = result && match;
						} 
						else if("OR".equalsIgnoreCase(logicOp)) {
							result = result || match;
						}
					}
				}
				return result;
			}
		}
	}
}