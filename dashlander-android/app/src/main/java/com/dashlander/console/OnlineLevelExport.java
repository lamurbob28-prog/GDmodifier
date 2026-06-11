package com.dashlander.console;

public class OnlineLevelExport {
    public boolean success = false;
    public String error = "";
    public String levelId = "";
    public String levelName = "";
    public String creator = "";
    public String songId = "0";
    public String audioTrack = "0";
    public String songIds = "";
    public String sfxIds = "";
    public String objects = "0";
    public String xml = "";
    public final java.util.List<UploadAttempt> attempts = new java.util.ArrayList<>();

    public String fileName() {
        String base = levelName == null || levelName.trim().isEmpty() ? "level_" + levelId : levelName.trim();
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.isEmpty()) base = "level_" + levelId;
        if (!base.toLowerCase().endsWith(".gmd")) base += ".gmd";
        return base;
    }

    public String summary() {
        return "Exported online level preview\n" +
                "Level ID: " + levelId + "\n" +
                "Name: " + levelName + "\n" +
                "Creator: " + blank(creator) + "\n" +
                "Objects: " + objects + "\n" +
                "Song k45: " + songId + "\n" +
                "Audio track k8: " + audioTrack + "\n" +
                "songIDs/k104: " + blank(songIds) + "\n" +
                "sfxIDs/k105: " + blank(sfxIds) + "\n";
    }

    private String blank(String value) {
        return value == null || value.trim().isEmpty() ? "(blank)" : value;
    }
}
