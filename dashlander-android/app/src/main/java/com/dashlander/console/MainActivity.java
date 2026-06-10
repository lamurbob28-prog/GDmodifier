package com.dashlander.console;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends Activity {
    private TextView log;
    private EditText usernameInput;
    private EditText accountIdInput;
    private EditText onlineNameInput;
    private CheckBox unlistedInput;
    private EditText passwordInput;
    private EditText confirmInput;
    private GitHubUploadsClient.UploadFile selectedFile;
    private String selectedXml;
    private GdLevelInfo selectedInfo;
    private UploadPreview selectedPreview;
    private String lastLevelId = "";

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

        usernameInput = new EditText(this);
        usernameInput.setHint("GD username");
        usernameInput.setText("BrotherOnGod");
        root.addView(usernameInput);

        accountIdInput = new EditText(this);
        accountIdInput.setHint("GD accountID, blank = auto lookup");
        accountIdInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(accountIdInput);

        onlineNameInput = new EditText(this);
        onlineNameInput.setHint("Online level name, blank = internal k2");
        root.addView(onlineNameInput);

        unlistedInput = new CheckBox(this);
        unlistedInput.setText("Unlisted upload");
        unlistedInput.setChecked(true);
        root.addView(unlistedInput);

        passwordInput = new EditText(this);
        passwordInput.setHint("GD password, not saved");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput);

        confirmInput = new EditText(this);
        confirmInput.setHint("Type UPLOAD after preview");
        root.addView(confirmInput);

        Button inspect = new Button(this);
        inspect.setText("Inspect GitHub uploads");
        root.addView(inspect);

        Button preview = new Button(this);
        preview.setText("Build upload preview");
        root.addView(preview);

        Button upload = new Button(this);
        upload.setText("UPLOAD to Geometry Dash");
        root.addView(upload);

        Button viewReceipt = new Button(this);
        viewReceipt.setText("View last debug receipt");
        root.addView(viewReceipt);

        Button copyLevelId = new Button(this);
        copyLevelId.setText("Copy last level ID");
        root.addView(copyLevelId);

        Button copyLog = new Button(this);
        copyLog.setText("Copy debug log");
        root.addView(copyLog);

        log = new TextView(this);
        log.setText("Ready. Termux replacement begins here.\n");
        log.setTextSize(14);
        root.addView(log);

        inspect.setOnClickListener(v -> inspectUploads());
        preview.setOnClickListener(v -> buildUploadPreview());
        upload.setOnClickListener(v -> uploadSelectedLevel());
        viewReceipt.setOnClickListener(v -> viewLastReceipt());
        copyLevelId.setOnClickListener(v -> copyLastLevelId());
        copyLog.setOnClickListener(v -> copyDebugLog());

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

                if (onlineNameInput.getText().toString().trim().isEmpty()) {
                    runOnUiThread(() -> onlineNameInput.setText(selectedInfo.levelName));
                }

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

    private UploadSettings makeSettings() {
        UploadSettings settings = new UploadSettings();
        settings.username = usernameInput.getText().toString().trim();
        settings.accountId = accountIdInput.getText().toString().trim();
        String chosenName = onlineNameInput.getText().toString().trim();
        settings.onlineLevelName = chosenName.isEmpty() && selectedInfo != null ? selectedInfo.levelName : chosenName;
        settings.unlisted = unlistedInput.isChecked();
        settings.forceStockSong = false;
        settings.audioTrackOverride = "";
        settings.songIdOverride = "";
        return settings;
    }

    private void buildUploadPreview() {
        if (selectedXml == null || selectedInfo == null) {
            append("\nInspect a GitHub .gmd first. The app is not guessing, because guessing is how we got the haunted wizard.\n");
            return;
        }

        append("\nBuilding create-new upload preview...\n");
        new Thread(() -> {
            try {
                selectedPreview = GdPayloadBuilder.buildPreview(selectedXml, makeSettings());
                post("\n" + selectedPreview.summary());
                post("No password was used. No upload sent yet. Type UPLOAD, then tap upload.\n");
            } catch (Exception e) {
                post("ERROR building preview: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void uploadSelectedLevel() {
        if (selectedXml == null || selectedInfo == null) {
            append("\nInspect a GitHub .gmd first.\n");
            return;
        }
        if (!"UPLOAD".equals(confirmInput.getText().toString().trim())) {
            append("\nType UPLOAD in the confirmation box first.\n");
            return;
        }
        String secret = passwordInput.getText().toString();
        if (secret.trim().isEmpty()) {
            append("\nPassword is empty. Not uploading.\n");
            return;
        }

        try {
            selectedPreview = GdPayloadBuilder.buildPreview(selectedXml, makeSettings());
            append("\nRebuilt upload preview from current fields.\n" + selectedPreview.summary());
        } catch (Exception e) {
            append("\nERROR rebuilding preview before upload: " + e.getMessage() + "\n");
            return;
        }

        append("\nUploading to Geometry Dash...\n");
        UploadSettings settings = makeSettings();
        new DashlanderUploadController(this).upload(selectedFile, selectedInfo, selectedPreview, settings, secret, new DashlanderUploadController.Callback() {
            @Override
            public void log(String message) {
                post(message);
            }

            @Override
            public void done(UploadResult result) {
                if (result.success) {
                    lastLevelId = result.levelId;
                    post("SUCCESS. Level ID: " + result.levelId + "\nSearch this exact ID in Geometry Dash. Do not search by name.\n");
                } else {
                    post("ERROR: " + result.error + "\n");
                }
            }
        });
    }

    private void viewLastReceipt() {
        try {
            String receipt = readInternalFile("last_upload_debug.json");
            append("\nLast debug receipt:\n" + receipt + "\n");
        } catch (Exception e) {
            append("\nNo debug receipt found yet. Upload once first.\n");
        }
    }

    private void copyLastLevelId() {
        String id = lastLevelId == null ? "" : lastLevelId.trim();
        if (id.isEmpty()) {
            try {
                String receipt = readInternalFile("last_upload_debug.json");
                JSONObject json = new JSONObject(receipt);
                id = json.optString("levelId", "").trim();
            } catch (Exception ignored) {
            }
        }
        if (id.isEmpty()) {
            append("\nNo level ID to copy yet. Upload successfully first.\n");
            return;
        }
        lastLevelId = id;
        copyText("Dashlander level ID", id);
        append("\nCopied level ID: " + id + "\n");
    }

    private void copyDebugLog() {
        copyText("Dashlander debug log", log.getText().toString());
        append("\nCopied debug log to clipboard.\n");
    }

    private String readInternalFile(String name) throws Exception {
        File file = new File(getFilesDir(), name);
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(data);
            if (read < 0) return "";
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private void copyText(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        }
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
