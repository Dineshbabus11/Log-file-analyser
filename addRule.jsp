<%@ page import="javax.servlet.http.HttpSession" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
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
    <title>Add Rule</title>
    <link rel="stylesheet" href="./style.css">
    <script>
        function addCondition() {
            const container=document.getElementById("conditions");
            const div=document.createElement("div");
            div.className="condition";
            div.innerHTML=`<select name="field" onchange="updateInputType(this)">
                    <option value="date">Date</option>
                    <option value="time">Time</option>
                    <option value="logger">Logger</option>
                    <option value="level">Level</option>
                    <option value="code">Code</option>
                    <option value="message">Message</option>
                </select>
                <select name="operator">
                    <option value="equals">equals</option>
                    <option value="greater">greater than</option>
                    <option value="less">less than</option>
                    <option value="greater_equal">greater or equal</option>
                    <option value="less_equal">less or equal</option>
                </select>
                <input type="text" name="pattern" required placeholder="pattern" />
                <span class="logic-span"></span>
                <button type="button" onclick="removeCondition(this)">Remove</button>`;
            container.appendChild(div);
            updateLogic();
            updateInputType(div.querySelector('select[name=field]'));
        }
        function updateInputType(select) {
            var input=select.parentNode.querySelector('input[name=pattern]');
            if(select.value==='date'){
				input.type='date';
			} 
            else if(select.value==='time'){
				input.type='time';
			} 
            else{
				input.type='text';
			} 
        }
        function updateLogic() {
            const conditions = document.querySelectorAll('.condition');
            conditions.forEach((cond, i) => {
                if(i===0){
                    cond.querySelector('.logic-span').innerHTML = '';
                } 
				else{
                    cond.querySelector('.logic-span').innerHTML = `
                        <select name="logic_op">
                            <option value="AND">AND</option>
                            <option value="OR">OR</option>
                        </select>
                    `;
                }
            });
        }
        function removeCondition(button) {
            button.parentNode.remove();
            updateLogic();
        }
        window.onload = addCondition;
    </script>
</head>
<body>
    <h2>Add New Rule</h2>
    <form action="addRule" method="post" onsubmit="console.log('Email:', document.querySelector('input[name=email]').value)">
        <label>Rule Name:</label><br>
        <input type="text" name="name" required><br>
		<label>Email:</label><br>
		<input type="email" name="email" required><br>
        <div id="conditions"></div>
        <button type="button" onclick="addCondition()">Add Condition</button>
        <br>
        <input type="submit" value="Save Rule">
    </form>
</body>
</html>
