<%@ page import="java.sql.*,app.DBconnect" %>
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
    <title>View Rules</title>
    <link rel="stylesheet" href="./style.css">
</head>
<body>
    <h2>Existing Rules</h2>
    <%
        try (Connection con = DBconnect.connect();
             PreparedStatement psRules=con.prepareStatement("SELECT * FROM rules");
             ResultSet rsRules=psRules.executeQuery()) {
            while (rsRules.next()) {
                int ruleId=rsRules.getInt("id");
                String ruleName=rsRules.getString("name");
				String email=rsRules.getString("email");
    %>
    <h3>Rule: <%= ruleName %> (ID: <%= ruleId %>)- Email: <%= email %></h3>
    <table border="1" cellpadding="5" style="margin-bottom:20px;">
        <tr>
            <th>Field</th>
            <th>Operator</th>
            <th>Pattern</th>
            <th>Logic Operator</th>
        </tr>
        <%
            try (PreparedStatement psCond=con.prepareStatement("SELECT field, operator, pattern, logic_op FROM rule_conditions WHERE rule_id=? ORDER BY id ASC")) {
                psCond.setInt(1, ruleId);
                try (ResultSet rsCond = psCond.executeQuery()) {
                    while (rsCond.next()) {
        %>
        <tr>
            <td><%= rsCond.getString("field")%></td>
            <td><%= rsCond.getString("operator")%></td>
            <td><%= rsCond.getString("pattern")%></td>
            <td><%= rsCond.getString("logic_op")%></td>
        </tr>
        <%
                    }
                }
            }
        %>
    </table>
    <%
            }
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    %>
    <br>
    <a href="index.jsp">Back</a>
</body>
</html>
