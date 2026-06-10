package com.dashlander.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BoomlingsClient {
    private static final String[] BASES = new String[] {
            "https://www.boomlings.com/database/",
            "http://www.boomlings.com/database/",
            "https://boomlings.com/database/",
            "http://boomlings.com/database/"
    };

    private static final String[] USER_AGENTS = new String[] {
            "GeometryDash/2.2",
            "",
            null
    };

    public UploadResult upload(Map<String, String> payload, String password) throws Exception {
        Map<String, String> withAuth = new LinkedHashMap<>(payload);
        withAuth.put("gjp2", GdCrypto.gjp2(password));
        withAuth.put("gjp", GdCrypto.oldGjp(password));

        UploadResult result = postWithFallback("uploadGJLevel21.php", withAuth);
        if (!result.success && result.error.isEmpty()) {
            result.error = "Upload failed. No positive level ID returned.";
        }
        return result;
    }

    public LevelVerifyResult verifyLevelId(String levelId) throws Exception {
        LevelVerifyResult out = new LevelVerifyResult();
        out.levelId = levelId;

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("gameVersion", "22");
        payload.put("binaryVersion", "48");
        payload.put("gdw", "0");
        payload.put("type", "26");
        payload.put("str", levelId);
        payload.put("diff", "-");
        payload.put("len", "-");
        payload.put("page", "0");
        payload.put("total", "0");
        payload.put("uncompleted", "0");
        payload.put("onlyCompleted", "0");
        payload.put("featured", "0");
        payload.put("original", "0");
        payload.put("twoPlayer", "0");
        payload.put("coins", "0");
        payload.put("epic", "0");
        payload.put("secret", "Wmfd2893gb7");

        UploadResult result = postWithFallback("getGJLevels21.php", payload);
        out.attempts.addAll(result.attempts);

        for (UploadAttempt attempt : result.attempts) {
            String body = attempt.body == null ? "" : attempt.body.trim();
            if (body.startsWith("-") || attempt.blockedLooking()) {
                continue;
            }
            String firstPart = body.split("#", 2)[0];
            Map<String, String> parsed = parseColonMap(firstPart);
            if (levelId.equals(parsed.get("1"))) {
                out.found = true;
                out.levelName = parsed.get("2") == null ? "" : parsed.get("2");
                return out;
            }
        }

        out.error = "Returned upload ID did not resolve through getGJLevels21.php type=26 yet.";
        return out;
    }

    public AccountLookupResult lookupAccountId(String username) throws Exception {
        AccountLookupResult out = new AccountLookupResult();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("gameVersion", "22");
        payload.put("binaryVersion", "48");
        payload.put("gdw", "0");
        payload.put("str", username);
        payload.put("page", "0");
        payload.put("total", "0");
        payload.put("secret", "Wmfd2893gb7");

        UploadResult result = postWithFallback("getGJUsers20.php", payload);
        out.attempts.addAll(result.attempts);
        for (UploadAttempt attempt : result.attempts) {
            String accountId = parseColonMap(attempt.body).get("16");
            if (accountId != null && !accountId.trim().isEmpty()) {
                out.success = true;
                out.accountId = accountId.trim();
                return out;
            }
        }
        out.error = "Account lookup failed.";
        return out;
    }

    private UploadResult postWithFallback(String endpoint, Map<String, String> payload) throws Exception {
        UploadResult result = new UploadResult();
        for (String base : BASES) {
            for (String userAgent : USER_AGENTS) {
                UploadAttempt attempt = post(base + endpoint, endpoint, payload, userAgent);
                result.attempts.add(attempt);

                String body = attempt.body == null ? "" : attempt.body.trim();
                if (isPositiveInt(body)) {
                    result.success = true;
                    result.levelId = body;
                    return result;
                }
                if (looksLikeColonData(body)) {
                    result.success = true;
                    return result;
                }
            }
        }
        return result;
    }

    private UploadAttempt post(String urlText, String endpoint, Map<String, String> payload, String userAgent) {
        UploadAttempt attempt = new UploadAttempt();
        attempt.url = urlText;
        attempt.endpoint = endpoint;
        attempt.userAgent = userAgent == null ? "(none)" : userAgent;
        long start = System.currentTimeMillis();
        try {
            byte[] data = encodePayload(payload).getBytes(StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "*/*");
            if (userAgent != null) {
                conn.setRequestProperty("User-Agent", userAgent);
            }
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }
            attempt.status = conn.getResponseCode();
            InputStream stream = attempt.status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            attempt.body = readAll(stream).trim();
        } catch (Exception e) {
            attempt.status = 0;
            attempt.body = e.toString();
        } finally {
            attempt.elapsedMs = System.currentTimeMillis() - start;
        }
        return attempt;
    }

    private String encodePayload(Map<String, String> payload) throws Exception {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (!first) out.append('&');
            first = false;
            out.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            out.append('=');
            out.append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), "UTF-8"));
        }
        return out.toString();
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private boolean isPositiveInt(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean looksLikeColonData(String value) {
        return value != null && value.contains(":") && !value.trim().startsWith("-") && !value.toLowerCase().contains("cloudflare");
    }

    private Map<String, String> parseColonMap(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        if (text == null) return out;
        String[] parts = text.split(":");
        for (int i = 0; i + 1 < parts.length; i += 2) {
            out.put(parts[i], parts[i + 1]);
        }
        return out;
    }

    public static class AccountLookupResult {
        public boolean success = false;
        public String accountId = "";
        public String error = "";
        public final java.util.List<UploadAttempt> attempts = new java.util.ArrayList<>();
    }

    public static class LevelVerifyResult {
        public boolean found = false;
        public String levelId = "";
        public String levelName = "";
        public String error = "";
        public final java.util.List<UploadAttempt> attempts = new java.util.ArrayList<>();
    }
}
