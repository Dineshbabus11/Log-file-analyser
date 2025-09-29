package app;

import java.sql.*;
import java.util.*;

public class SimpleRulesCache {
    private static Map<Integer, CachedRule> rulesCache = new HashMap<>();

    public static void reload() {
        Map<Integer, CachedRule> tempCache = new HashMap<>();
        try (Connection con = DBconnect.connect()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name FROM rules");
            while (rs.next()) {
                int ruleId = rs.getInt("id");
                String ruleName = rs.getString("name");
                CachedRule rule = new CachedRule(ruleId, ruleName);
                PreparedStatement psCond = con.prepareStatement("SELECT * FROM rule_conditions WHERE rule_id=? ORDER BY id ASC");
                psCond.setInt(1, ruleId);
                ResultSet rsCond = psCond.executeQuery();
                while (rsCond.next()) {
                    rule.conditions.add(new RuleCondition(rsCond.getString("field"),rsCond.getString("pattern"),
                        rsCond.getString("operator"),rsCond.getString("logic_op")));
                }
                tempCache.put(ruleId, rule);
            }
            rulesCache = tempCache;
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Collection<CachedRule> getRules() {
        return rulesCache.values();
    }

    public static class CachedRule {
        public int id;
        public String name;
        public List<RuleCondition> conditions = new ArrayList<>();
        public CachedRule(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class RuleCondition {
        public String field, pattern, operator, logicOp;

        public RuleCondition(String field, String pattern, String operator, String logicOp) {
            this.field = field;
            this.pattern = pattern;
            this.operator = operator;
            this.logicOp = logicOp;
        }
    }
}
