<%@ page import="java.util.List" %>
<%@ page import="app.LogEntry" %>
<!DOCTYPE html>
<html>
<head>
  <title>Log Analysis Results</title>
  <link rel="stylesheet" href="./style.css"/>
</head>
<body>
<h2>Analysis of: ${fileName}</h2>
<form action="ruleLogs" method="get">
    <input type="submit" value="View Logs By Rule" />
</form>
<p>Total Lines: ${totalLines}</p>
<p>Errors: ${errorCount}</p>
<p>Warnings: ${warningCount}</p>
<p>Info Logs: ${infoCount}</p>

<h2>Elasticsearch Log Entries</h2>
<table border="1" cellpadding="5" style="border-collapse: collapse;">
<tr>
  <th>Time</th>
  <th>Date</th>
  <th>Logger</th>
  <th>Level</th>
  <th>Code</th>
  <th>Message</th>
  <th>Matched Rules</th>
</tr>
<%
  List<LogEntry> esLogs=(List<LogEntry>) request.getAttribute("esLogEntries");
  if(esLogs != null){
    for(LogEntry le:esLogs){
%>
<tr>
  <td><%=le.time%></td>
  <td><%=le.date%></td>
  <td><%=le.logger%></td>
  <td><%=le.level%></td>
  <td><%=le.code%></td>
  <td><%=le.message%></td>
  <td><%=le.matchedRuleNames!=null?le.matchedRuleNames:""%></td>
</tr>
<%
    }
  } 
  else{
%>
<tr>
  <td colspan="7">No log entries found.</td>
</tr>
<%
  }
%>
</table>
<p>ES log entries found: <%=(esLogs!=null?esLogs.size():0)%></p>

<div style="margin-top:20px;">
  <%
    Integer currentPageObj=(Integer)request.getAttribute("currentPage");
    Integer totalPagesObj=(Integer)request.getAttribute("totalPages");
    int currentPage=(currentPageObj!=null)?currentPageObj:1;
    int totalPages=(totalPagesObj!=null)?totalPagesObj:1;
    String filename=(String)request.getAttribute("fileName");
    String baseUrl="uploadLog?fileName="+java.net.URLEncoder.encode(filename,"UTF-8");

    if(currentPage>1){
  %>
    <a href="<%=baseUrl%>&page=<%=currentPage-1 %>">Previous</a>
  <%
    }
  %>
  Page <%=currentPage%>of<%=totalPages%>
  <%
    if(currentPage<totalPages){
  %>
    <a href="<%=baseUrl%>&page=<%=currentPage+1 %>">Next</a>
  <%
    }
  %>
</div>

<h3>Filter by Message Regex</h3>
<form action="filterMessage" method="post">
  <input type="hidden" name="fileName" value="${fileName}"/>
  <label for="msgPattern">Regex Pattern:</label>
  <input type="text" id="msgPattern" name="msgPattern" />
  <input type="submit" value="Filter" />
</form>

</body>
</html>
