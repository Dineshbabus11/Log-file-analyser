<%@ page import="java.util.*" %>
<!DOCTYPE html>
<html>
<head>
    <title>Live Logs - <%= request.getAttribute("path") %></title>
    <link rel="stylesheet" href="./style.css" />
    <script>
        function fetchLogs() {
            const indexName = "<%= request.getAttribute("path") %>";
            fetch('liveLogData?indexName=' + encodeURIComponent(indexName))
                .then(response => response.json())
                .then(data => {
                    const tbody = document.getElementById('logsBody');
                    tbody.innerHTML = '';
                    if (data.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="8">No logs found</td></tr>';
                        return;
                    }
                    data.forEach(log => {
                        const row = document.createElement('tr');
						['time', 'date', 'logger', 'level', 'code', 'message', 'matchedRuleNames'].forEach(k => {
							const td = document.createElement('td');
							td.textContent = log[k] ? log[k] : '-';
							row.appendChild(td);
						});
						tbody.appendChild(row); 	
                    });
                })
                .catch(console.error);
        }
        setInterval(fetchLogs, 3000);
        window.onload = fetchLogs;
    </script>
</head>
<body>
    <h1>Live Logs for <%= request.getAttribute("path") %></h1>
    <a href="manageMultiAppPaths.jsp">Back</a>
    <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
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
        <tbody id="logsBody">
            <tr><td colspan="8">Loading...</td></tr>
        </tbody>
    </table>
</body>
</html>
