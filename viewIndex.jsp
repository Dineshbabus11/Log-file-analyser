<%@ page import="javax.servlet.http.HttpSession" %>
<%
  if (session == null || session.getAttribute("username") == null) {
    response.sendRedirect(request.getContextPath() + "/login.jsp");
    return;
  }
%>
<%
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setDateHeader("Expires", 0);
%>

<!DOCTYPE html>
<html>
<head>
  <title>Simple Layout Logs Viewer</title>
  <style>
  @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@100..900&display=swap');



*{
    font-family: "Outfit", sans-serif;
}
    .container {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 10px;
      font-family: Arial, sans-serif;
    }
    .top-row {
      display: flex;
      gap: 20px;
    }
    .columns-section {
      flex: 1;
      border: 1px solid #000;
      padding: 5px;
      min-height: 100px;
      overflow-y: auto;
    }
    .columns-section h2 {
      margin: 0 0 5px 0;
      font-size: 1.1em;
    }
    .table-section {
      border: 1px solid #000;
      padding: 5px;
      overflow-x: auto;
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th, td {
      border: 1px solid #000;
      padding: 5px;
      text-align: left;
      white-space: nowrap;
    }
    .column-item {
      display: flex;
      justify-content: space-between;
      margin: 3px 0;
    }
    button {
      padding: 2px 6px;
      font-size: 0.9em;
      cursor: pointer;
    }
    button:disabled {
      cursor: default;
    }
    #paginationControls {
      margin-top: 8px;
    }
    #paginationControls button {
      margin-right: 8px;
    }
  </style>
</head>
<body>
<h1>Simple Logs Viewer</h1>
<a href="manageindextracking.jsp">Back to manage page</a>
<div class="container">
  <div class="top-row">
    <div class="columns-section" id="availableColumnsContainer">
      <h2>Available Columns</h2>
      <div id="availableColumns"></div>
    </div>
    <div class="columns-section" id="selectedColumnsContainer">
      <h2>Selected Columns</h2>
      <div id="addedColumns"></div>
    </div>
  </div>
  <div class="table-section">
    <table>
      <thead>
        <tr id="tableHeader"></tr>
      </thead>
      <tbody id="tableBody"></tbody>
    </table>
    <div id="paginationControls">
      <button id="prevPage">Prev</button>
      <span id="pageInfo"></span>
      <button id="nextPage">Next</button>
    </div>
  </div>
</div>
<script>
  let currentPage = 1;
  const pageSize = 50;
  let totalHits = 0;
  let allKeys = [];
  let addedColumns = [];
  const refreshIntervalMs = 1000;
  function fetchData(page) {
    const params = new URLSearchParams(window.location.search);
    const indexName = params.get('indexName');
    fetch("viewIndexData?indexName=" + encodeURIComponent(indexName) + "&page=" + page + "&size=" + pageSize)
      .then(response => response.json())
      .then(data => {
        if (data.error) {
          alert("Error: " + data.error);
          return;
        }
        allKeys = data.allKeys;
        totalHits = data.totalHits;
        currentPage = data.page;
        if (addedColumns.length === 0 && allKeys.length > 0) {
          addedColumns = allKeys.slice(0, 3);
        }
        renderAvailableColumns();
        renderSelectedColumns();
        renderLogsTable(data.logs);
        renderPaginationControls();
      })
      .catch(err => console.error('Error fetching data:', err));
  }
  function renderAvailableColumns() {
    const container = document.getElementById('availableColumns');
    container.innerHTML = '';
    allKeys.forEach(col => {
      if (!addedColumns.includes(col)) {
        const div = document.createElement('div');
        div.className = 'column-item';
        div.textContent = col;
        const btn = document.createElement('button');
        btn.textContent = 'Add';
        btn.onclick = () => {
          addedColumns.push(col);
          renderAvailableColumns();
          renderSelectedColumns();
          fetchData(currentPage);
        };
        div.appendChild(btn);
        container.appendChild(div);
      }
    });
  }
  function renderSelectedColumns() {
    const container = document.getElementById('addedColumns');
    container.innerHTML = '';
    addedColumns.forEach(col => {
      const div = document.createElement('div');
      div.className = 'column-item';
      div.textContent = col;
      const btn = document.createElement('button');
      btn.textContent = 'Remove';
      btn.onclick = () => {
        addedColumns = addedColumns.filter(c => c !== col);
        renderAvailableColumns();
        renderSelectedColumns();
        fetchData(currentPage);
      };
      div.appendChild(btn);
      container.appendChild(div);
    });
  }
  function renderLogsTable(logs) {
    const thead = document.getElementById('tableHeader');
    thead.innerHTML = '';
    addedColumns.forEach(col => {
      const th = document.createElement('th');
      th.textContent = col;
      thead.appendChild(th);
    });
    const tbody = document.getElementById('tableBody');
    tbody.innerHTML = '';
    logs.forEach(log => {
      const tr = document.createElement('tr');
      addedColumns.forEach(col => {
        const td = document.createElement('td');
        let val = log[col];
        if (val === undefined || val === null || val === '') {
			val = '-';
		}
        else if (Array.isArray(val)) {
			val = val.join(', ');
		}
        td.textContent = val;
        tr.appendChild(td);
      });
      tbody.appendChild(tr);
    });
  }
  function renderPaginationControls() {
    const totalPages = Math.max(1, Math.ceil(totalHits / pageSize));
    document.getElementById('pageInfo').textContent = "Page " + currentPage + " of " + totalPages;
    document.getElementById('prevPage').disabled = currentPage <= 1;
    document.getElementById('nextPage').disabled = currentPage >= totalPages;
  }
  document.getElementById('prevPage').addEventListener('click', () => {
    if (currentPage > 1) {
      fetchData(currentPage - 1);
    }
  });
  document.getElementById('nextPage').addEventListener('click', () => {
    const totalPages = Math.max(1, Math.ceil(totalHits / pageSize));
    if (currentPage < totalPages) {
      fetchData(currentPage + 1);
    }
  });
  window.onload = () => {
    fetchData(1);
    setInterval(() => {
      fetchData(currentPage);
    }, refreshIntervalMs);
  };
</script>
</body>
</html>
