package com.dashlander.console;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GdPayloadBuilder {
    private static final String SECRET = "Wmfd2893gb7";

    private GdPayloadBuilder() {}

    public static UploadPreview buildPreview(String xml, UploadSettings settings) throws Exception {
        Map<String, String> data = GmdParser.parsePlist(xml);
        String levelString = data.getOrDefault("k4", "");
        if (levelString.length() < 20) {
            throw new IllegalArgumentException("Bad or missing k4 levelString.");
        }

        String levelName = firstNonBlank(settings.onlineLevelName, data.getOrDefault("k2", "Imported GMD"));
        if (levelName.length() > 40) levelName = levelName.substring(0, 40);

        String originalSongId = data.getOrDefault("k45", "0");
        String originalAudioTrack = data.getOrDefault("k8", "0");
        String songList = data.getOrDefault("k104", "");
        String sfxList = data.getOrDefault("k105", "");
        String songId;
        String audioTrack;

        if (settings.forceStockSong) {
            songId = "0";
            audioTrack = firstNonBlank(settings.audioTrackOverride, originalAudioTrack, "0");
        } else if (!isBlank(settings.songIdOverride)) {
            songId = settings.songIdOverride.trim();
            audioTrack = firstNonBlank(settings.audioTrackOverride, isPositive(songId) ? "0" : originalAudioTrack, "0");
        } else {
            songId = firstNonBlank(originalSongId, "0");
            audioTrack = firstNonBlank(settings.audioTrackOverride, isPositive(songId) ? "0" : originalAudioTrack, "0");
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("accountID", settings.accountId);
        payload.put("userName", settings.username);
        payload.put("levelID", "0");
        payload.put("levelName", levelName);
        payload.put("levelDesc", data.getOrDefault("k3", ""));
        payload.put("levelVersion", data.getOrDefault("k16", "1"));
        payload.put("levelLength", data.getOrDefault("k23", "0"));
        payload.put("audioTrack", audioTrack);
        payload.put("auto", "0");
        payload.put("password", "1");
        payload.put("original", "0");
        payload.put("twoPlayer", data.getOrDefault("k43", "0"));
        payload.put("songID", songId);
        putIfNotBlank(payload, "songIDs", songList);
        putIfNotBlank(payload, "sfxIDs", sfxList);
        payload.put("objects", data.getOrDefault("k48", "0"));
        payload.put("coins", countCoins(data));
        payload.put("requestedStars", data.getOrDefault("k66", "0"));
        payload.put("unlisted", normalizeUnlistedValue(settings.unlistedValue, settings.unlisted));
        payload.put("ldm", data.getOrDefault("k72", "0"));
        payload.put("levelString", levelString);
        payload.put("seed2", GdCrypto.chk(uploadSeed(levelString), "41274", "xI25fpAapCQg"));
        payload.put("secret", SECRET);
        payload.put("gameVersion", "22");
        payload.put("binaryVersion", "48");
        payload.put("gdw", "0");
        payload.put("dvs", "2");

        UploadPreview preview = new UploadPreview();
        preview.payload = payload;
        preview.levelStringLength = levelString.length();
        preview.levelStringHash = GdCrypto.sha256Hex(levelString);
        return preview;
    }

    private static String uploadSeed(String levelString) {
        int chars = 50;
        if (levelString.length() < chars) return levelString;
        int step = Math.max(1, levelString.length() / chars);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < levelString.length() && out.length() < chars; i += step) {
            out.append(levelString.charAt(i));
        }
        return out.toString();
    }

    private static String countCoins(Map<String, String> data) {
        int count = 0;
        if (!isBlank(data.get("k61"))) count++;
        if (!isBlank(data.get("k62"))) count++;
        if (!isBlank(data.get("k63"))) count++;
        return String.valueOf(count);
    }

    private static String normalizeUnlistedValue(String value, boolean legacyUnlisted) {
        if ("0".equals(value) || "1".equals(value) || "2".equals(value)) {
            return value;
        }
        return legacyUnlisted ? "1" : "0";
    }

    private static void putIfNotBlank(Map<String, String> payload, String key, String value) {
        if (!isBlank(value)) {
            payload.put(key, value.trim());
        }
    }

    private static boolean isPositive(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }
}
