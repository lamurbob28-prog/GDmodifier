package com.dashlander.console;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private TextView log;
    private GitHubUploadsClient.UploadFile selectedFile;
    private String selectedXml;
    private GdLevelInfo selectedInfo;
    private UploadPreview selectedPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Dashlander Console\nNative uploader alpha");
        title.setTextSize(22);
        root.addView(title);

        Button inspect = new Button(this);
        inspect.setText("Inspect GitHub uploads");
        root.addView(inspect);

        Button upload = new Button(this);
        upload.setText("Build upload preview");
        root.addView(upload);

        log = new TextView(this);
        log.setText("Ready. Termux replacement begins here.\n");
        log.setTextSize(14);
        root.addView(log);

        inspect.setOnClickListener(v -> inspectUploads());
        upload.setOnClickListener(v -> buildUploadPreview());

        setContentView(scroll);
    }

    private void inspectUploads() {
        append("\nFetching GitHub uploads...\n");
        new Thread(() -> {
            try {
                GitHubUploadsClient client = new GitHubUploadsClient();
                List<GitHubUploadsClient.UploadFile> files = client.listUploads();
                if (files.isEmpty()) {
                    post("No .gmd files found in GitHub uploads.\n");
                    return;
                }

                StringBuilder listing = new StringBuilder();
                listing.append("Found ").append(files.size()).append(" .gmd file(s):\n");
                for (int i = 0; i < files.size(); i++) {
                    listing.append(i + 1).append(". ").append(files.get(i).path).append('\n');
                }
                post(listing.toString());

                selectedFile = files.get(0);
                post("\nDownloading first file for inspection: " + selectedFile.path + "\n");
                selectedXml = client.downloadGmd(selectedFile);
                selectedInfo = GmdParser.parse(selectedFile.path, selectedXml);

                post("\nInspection result:\n" +
                        "Source: " + selectedInfo.sourceName + "\n" +
                        "Internal name k2: " + selectedInfo.levelName + "\n" +
                        "Original online ID k1: " + blank(selectedInfo.originalLevelId) + "\n" +
                        "Objects k48: " + selectedInfo.objects + "\n" +
                        "Song k45: " + selectedInfo.songId + "\n" +
                        "Audio track k8: " + selectedInfo.audioTrack + "\n" +
                        "Level string length: " + selectedInfo.levelStringLength + "\n" +
                        "Level string sha256: " + selectedInfo.levelStringHash.substring(0, Math.min(16, selectedInfo.levelStringHash.length())) + "...\n");
            } catch (Exception e) {
                post("ERROR: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void buildUploadPreview() {
        if (selectedXml == null || selectedInfo == null) {
            append("\nInspect a GitHub .gmd first. The app is not guessing, because guessing is how we got the haunted wizard.\n");
            return;
        }

        append("\nBuilding create-new upload preview...\n");
        new Thread(() -> {
            try {
                UploadSettings settings = new UploadSettings();
                settings.onlineLevelName = selectedInfo.levelName;
                settings.unlisted = true;
                settings.forceStockSong = false;
                settings.audioTrackOverride = "";
                settings.songIdOverride = "";

                selectedPreview = GdPayloadBuilder.buildPreview(selectedXml, settings);
                post("\n" + selectedPreview.summary());
                post("This is only a preview. No password asked. No upload sent.\n");
            } catch (Exception e) {
                post("ERROR building preview: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private String blank(String value) {
        return value == null || value.trim().isEmpty() ? "(blank)" : value;
    }

    private void post(String text) {
        runOnUiThread(() -> append(text));
    }

    private void append(String text) {
        log.append(text);
    }
}
