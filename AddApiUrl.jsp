<html>
<head>
    <title>Add API URL</title>
    <link rel="stylesheet" href="./style.css">
</head>
<body>
    <h1>API URL Tracking</h1>
    <form action="addApiUrl" method="post">
        <label for="newApiUrl">API URL:</label><br/>
        <input type="text" id="newApiUrl" name="newApiUrl" placeholder="Enter log API URL" required style="width:400px;" /><br/><br/>
		<label for="apiKey">API Key:</label><br/>
        <input type="text" id="apiKey" name="apiKey" placeholder="Enter API Key for this URL" required style="width:400px;" /><br/><br/>
        <input type="submit" value="Add API URL" />
    </form>
    <hr/>
    <form action="manageApiUrls.jsp" method="get">
        <button type="submit">Go to Manage APIs</button>
    </form>
    <form action="index.html">
        <button type="submit">Back to main page</button>
    </form>
</body>
</html>
