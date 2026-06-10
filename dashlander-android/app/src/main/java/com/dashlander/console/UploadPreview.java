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
                "Payload unlisted: " + payload.get("unlisted") + "\n" +
                "Level string length: " + levelStringLength + "\n" +
                "Level string sha256: " + levelStringHash.substring(0, Math.min(16, levelStringHash.length())) + "...\n";
    }
}
