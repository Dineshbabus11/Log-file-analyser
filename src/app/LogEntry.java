package app;

import java.util.concurrent.atomic.AtomicInteger;

public class LogEntry {
    public static AtomicInteger globalId = new AtomicInteger(0);
    public int id;
    public String time;
    public String date;
    public String logger;
    public String level;
    public String code;
    public String message;
    public String matchedRuleNames;

    public LogEntry(String time, String date, String logger, String level, String code, String message, String matchedRuleNames) {
        this.id = globalId.incrementAndGet();
        this.time = time;
        this.date = date;
        this.logger = logger;
        this.level = level;
        this.code = code;
        this.message = message;
        this.matchedRuleNames = matchedRuleNames;
    }
}
