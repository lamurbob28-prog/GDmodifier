package com.dashlander.console;

import java.util.Map;

public class UploadPreview {
    public Map<String, String> payload;
    public String levelStringHash;
    public int levelStringLength;

    public String summary() {
        return "CREATE-NEW payload preview\n" +
                "Payload levelID: " + payload.get("levelID") + "\n" +
                "Payload levelName: " + payload.get("levelName") + "\n" +
                "Payload objects: " + payload.get("objects") + "\n" +
                "Payload songID: " + payload.get("songID") + "\n" +
                "Payload audioTrack: " + payload.get("audioTrack") + "\n" +
                "Payload songIDs: " + blank(payload.get("songIDs")) + "\n" +
                "Payload sfxIDs: " + blank(payload.get("sfxIDs")) + "\n" +
                "Payload auto: " + payload.get("auto") + "\n" +
                "Payload copy password: " + payload.get("password") + "\n" +
                "Payload unlisted: " + payload.get("unlisted") + "\n" +
                "Level string length: " + levelStringLength + "\n" +
                "Level string sha256: " + levelStringHash.substring(0, Math.min(16, levelStringHash.length())) + "...\n";
    }

    private String blank(String value) {
        return value == null || value.trim().isEmpty() ? "(blank)" : value;
    }
}
