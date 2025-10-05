<%@ page import="java.util.*" %>
<%@ page import="java.net.URLEncoder" %>
<!DOCTYPE html>
<html>
<head>
    <title>Live Logs - <%= request.getAttribute("apiIndex") %></title>
    <link rel="stylesheet" href="./style.css"/>
    <script>
        let currentPage = 1;
        const pageSize = 50;

        function fetchLogs(page = 1) {
            const indexName = "<%= request.getAttribute("apiIndex") %>";
            fetch('liveLogDataAPI?indexName=' + encodeURIComponent(indexName) +
                  '&page=' + page + '&size=' + pageSize)
                .then(response => response.json())
                .then(data => {
                    currentPage = data.page;
                    const tbody = document.getElementById('logsBody');
                    tbody.innerHTML = '';
                    if (!data.logs || data.logs.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="7">No logs found</td></tr>';
                        document.getElementById('pagination').style.display = 'none';
                        return;
                    }
                    data.logs.forEach(log => {
                        const row = document.createElement('tr');
                        ['time', 'date', 'logger', 'level', 'code', 'message', 'matchedRuleNames'].forEach(k => {
                            const td = document.createElement('td');
                            td.textContent = log[k] ? log[k] : '-';
                            row.appendChild(td);
                        });
                        tbody.appendChild(row);
                    });
                    const pagination = document.getElementById('pagination');
                    pagination.style.display = 'block';

                    const totalPages = Math.ceil(data.totalHits / pageSize);
                    document.getElementById('pageInfo').textContent = 'Page ' + currentPage + ' of ' + totalPages;
                    document.getElementById('prevBtn').disabled = currentPage <= 1;
                    document.getElementById('nextBtn').disabled = currentPage >= totalPages;
                })
                .catch(console.error);
        }

        function prevPage() {
            if (currentPage > 1) {
                fetchLogs(currentPage - 1);
            }
        }

        function nextPage() {
            fetchLogs(currentPage + 1);
        }

        setInterval(() => fetchLogs(currentPage), 3000);
        window.onload = () => fetchLogs(1);
    </script>
</head>
<body>
    <h1>Live Logs for <%= request.getAttribute("apiIndex") %></h1>
    <form action="ExportLogsPdf" method="get" style="margin-bottom: 15px;">
        <input type="hidden" name="indexName" value="<%= URLEncoder.encode((String)request.getAttribute("apiIndex"), "UTF-8") %>" />
        <button type="submit">Export PDF</button>
    </form>
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
            <tr><td colspan="7">Loading...</td></tr>
        </tbody>
    </table>
    <div id="pagination" style="display:none; margin-top: 10px;">
        <button id="prevBtn" onclick="prevPage()">Previous</button>
        <span id="pageInfo"></span>
        <button id="nextBtn" onclick="nextPage()">Next</button>
    </div>
</body>
</html>
