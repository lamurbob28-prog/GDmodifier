package com.dashlander.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpUtil {
    private HttpUtil() {}

    public static String getText(String urlText) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "DashlanderConsole/0.1");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);
        int status = conn.getResponseCode();
        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readAll(stream);
        if (status >= 400) {
            throw new RuntimeException("HTTP " + status + ": " + body);
        }
        return body;
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append('\n');
        }
        return out.toString();
    }
}
