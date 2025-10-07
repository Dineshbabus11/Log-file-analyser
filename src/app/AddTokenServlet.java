package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@WebServlet("/addToken")
public class AddTokenServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		SessionUtils.setNoCacheHeaders(res);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String indexName = req.getParameter("indexName");
        if (indexName == null || indexName.trim().isEmpty()) {
            res.sendRedirect("manageindextracking.jsp");
            return;
        }
        try (Connection con = DBconnect.connect()) {
            String tokenValue = generateToken(indexName);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiry = now.plusDays(10);
            PreparedStatement tokenInsert = con.prepareStatement(
                "INSERT INTO index_tokens(index_name, token_value, created_at, expires_at) VALUES (?, ?, ?, ?)" );
            tokenInsert.setString(1, indexName);
            tokenInsert.setString(2, tokenValue);
            tokenInsert.setTimestamp(3, Timestamp.valueOf(now));
            tokenInsert.setTimestamp(4, Timestamp.valueOf(expiry));
            tokenInsert.executeUpdate();
        } 
		catch (Exception e) {
            throw new ServletException("Error adding token", e);
        }
        res.sendRedirect("manageindextracking.jsp");
    }
    private String generateToken(String uniqueData) throws NoSuchAlgorithmException {
        String timestamp = Long.toString(System.currentTimeMillis());
        String salt = generateRandomSalt(16);

        String dataToHash = uniqueData + timestamp + salt;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(dataToHash.getBytes());

        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
    }
    private String generateRandomSalt(int length) {
        final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for(int i=0; i<length; i++){
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
