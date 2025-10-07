<%@ page import="javax.servlet.http.HttpSession" %>
<%@ page import="java.net.URLEncoder" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>

<!DOCTYPE html>
<html>
<head>
    <title>Index View - <%= request.getParameter("indexName") %></title>
    <link rel="stylesheet" href="./style.css" />
    <script>
        let currentPage = 1;
const pageSize = 50;

function buildTableHeader(keys) {
    const thead = document.querySelector('table thead');
    thead.innerHTML = '';

    const tr = document.createElement('tr');
    keys.forEach(key => {
        const th = document.createElement('th');
        th.textContent = key.charAt(0).toUpperCase() + key.slice(1);
        tr.appendChild(th);
    });
    thead.appendChild(tr);
}

function fetchLogs(page = 1) {
    const urlParams = new URLSearchParams(window.location.search);
    const indexName = urlParams.get("indexName");
    fetch('viewIndexData?indexName=' + encodeURIComponent(indexName) + '&page=' + page + '&size=' + pageSize)
        .then(response => response.json())
        .then(data => {
            currentPage = data.page;
            const keys = data.allKeys;
            buildTableHeader(keys);

            const tbody = document.getElementById('logsBody');
            tbody.innerHTML = '';
            if (!data.logs || data.logs.length === 0) {
                tbody.innerHTML = '<tr><td colspan="' + keys.length + '" style="text-align:center">No logs found</td></tr>';
                document.getElementById('pagination').style.display = 'none';
                return;
            }
            data.logs.forEach(log => {
                const row = document.createElement('tr');
                keys.forEach(key => {
                    const td = document.createElement('td');
                    let val = log[key];
                    if (Array.isArray(val)) {
                        val = val.join(", ");
                    }
                    td.textContent = val != null ? val : '-';
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

window.onload = () => {
    fetchLogs(1);
    setInterval(() => {
        fetchLogs(currentPage);
    }, 1000);
};
    </script>
</head>
<body>
    <h1>View Logs for: <%= request.getParameter("indexName") %></h1>

    <form action="manageindextracking.jsp" method="get" style="margin-bottom: 15px;">
        <button type="submit">Back to Manage Indices</button>
    </form>

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
            <tr><td colspan="7" style="text-align:center">Loading...</td></tr>
        </tbody>
    </table>

    <div id="pagination" style="display:none; margin-top: 10px;">
        <button id="prevBtn" onclick="prevPage()">Previous</button>
        <span id="pageInfo">Page 1</span>
        <button id="nextBtn" onclick="nextPage()">Next</button>
    </div>
</body>
</html>
