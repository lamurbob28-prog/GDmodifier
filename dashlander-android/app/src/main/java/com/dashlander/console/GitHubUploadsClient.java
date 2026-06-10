package com.dashlander.console;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GitHubUploadsClient {
    public static class UploadFile {
        public final String name;
        public final String path;
        public final String rawUrl;

        public UploadFile(String name, String path, String rawUrl) {
            this.name = name;
            this.path = path;
            this.rawUrl = rawUrl;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public List<UploadFile> listUploads() throws Exception {
        String body = HttpUtil.getText(DashlanderConfig.UPLOADS_API);
        JSONArray arr = new JSONArray(body);
        List<UploadFile> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.getJSONObject(i);
            String type = item.optString("type", "");
            String name = item.optString("name", "");
            String path = item.optString("path", "");
            if (!"file".equals(type) || !name.toLowerCase().endsWith(".gmd")) {
                continue;
            }
            String encodedPath = encodePath(path);
            String rawUrl = DashlanderConfig.RAW_BASE + encodedPath;
            out.add(new UploadFile(name, path, rawUrl));
        }
        return out;
    }

    public String downloadGmd(UploadFile file) throws Exception {
        return HttpUtil.getText(file.rawUrl);
    }

    private static String encodePath(String path) throws Exception {
        String[] parts = path.split("/");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append('/');
            out.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8.name()).replace("+", "%20"));
        }
        return out.toString();
    }
}
