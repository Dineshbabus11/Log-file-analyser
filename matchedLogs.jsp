<%@ page import="app.LogEntry" %>
<%@ page import="javax.servlet.http.HttpSession" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>

<!DOCTYPE html>
<html>
<head>
    <title>Matched Log Entries</title>
    <link rel="stylesheet" href="style.css"/>
</head>
<body>
<h2>Matched Log Entries</h2>
<table border="1" cellpadding="5" cellspacing="0" id="matchedTable" style="border-collapse:collapse;">
    <thead>
        <tr>
            <th>Time</th>
            <th>Date</th>
            <th>Logger</th>
            <th>Level</th>
            <th>Code</th>
            <th>Message</th>
            <th>Matched Rules</th>
        </tr>
    </thead>
    <tbody id="matchedBody">
    </tbody>
</table>
<script>
let lastReceivedId = 0;

function fetchMatchedLogs() {
    fetch('matchedLogsJson?lastId=' + lastReceivedId)
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('matchedBody');
			if (data.length === 0) {
                if (tbody.children.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center">No data</td></tr>`;
                }
                return;
            }
			if(tbody.children.length===1&&tbody.children[0].children[0].textContent==='No data') {
                tbody.innerHTML = '';
            }
            data.forEach(log => {
				console.log('Log entry properties:', log.time, log.date, log.logger, log.level, log.code, log.message, log.matchedRuleNames);
                const row = document.createElement('tr');
                ['time','date','logger','level','code','message','matchedRuleNames'].forEach(k => {
                    const td = document.createElement('td');
                    td.textContent = log[k] ? log[k] : '-';
                    row.appendChild(td);
                });
                tbody.appendChild(row); 	

                if(log.id>lastReceivedId){
                    lastReceivedId = log.id;
                }
            });
        })
        .catch(error => console.error('Error fetching matched logs:', error));
}
setInterval(fetchMatchedLogs, 1000);

</script>
</body>
</html>
