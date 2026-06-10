package com.dashlander.console;

public class UploadAttempt {
    public String endpoint = "";
    public String url = "";
    public int status = 0;
    public String userAgent = "";
    public String body = "";
    public long elapsedMs = 0;

    public String preview() {
        if (body == null) return "";
        String clean = body.replace("\n", "\\n");
        return clean.length() > 260 ? clean.substring(0, 260) : clean;
    }

    public boolean blockedLooking() {
        if (body == null) return false;
        String lower = body.toLowerCase();
        return lower.contains("cloudflare") ||
                lower.contains("access denied") ||
                lower.contains("checking your browser") ||
                lower.contains("<!doctype html") ||
                lower.contains("error 1020");
    }
}
