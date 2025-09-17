package app;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
@WebServlet("/addRule")
public class AddRule extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String name=req.getParameter("name");
        String[] fields=req.getParameterValues("field");
        String[] patterns=req.getParameterValues("pattern");
        String[] operators=req.getParameterValues("operator");
        String[] logicOps=req.getParameterValues("logic_op");
        try(Connection con=DBconnect.connect();
            PreparedStatement psRule = con.prepareStatement("INSERT INTO rules (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            psRule.setString(1,name);
            psRule.executeUpdate();
            ResultSet keys=psRule.getGeneratedKeys();
            int ruleId=keys.next()?keys.getInt(1):-1;
            PreparedStatement psCond=con.prepareStatement("INSERT INTO rule_conditions (rule_id, field, pattern, operator, logic_op) VALUES (?, ?, ?, ?, ?)");
            for (int i=0;i<fields.length;i++) {
                String logicOp = (i == 0)?"NONE":logicOps[i - 1];
                psCond.setInt(1,ruleId);
                psCond.setString(2,fields[i]);
                psCond.setString(3,patterns[i]);
                psCond.setString(4,operators[i]);
                psCond.setString(5,logicOp);
                psCond.addBatch();
            }
            psCond.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
        res.sendRedirect("viewRules.jsp");
    }
}
