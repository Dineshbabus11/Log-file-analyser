package app;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class TokenManager {

    public static void cleanupExpiredTokens() throws Exception {
        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM index_tokens WHERE expires_at <= CURRENT_TIMESTAMP")) {
            int deleted = ps.executeUpdate();
            System.out.println("Expired tokens cleaned up: " + deleted);
        }
    }
}
