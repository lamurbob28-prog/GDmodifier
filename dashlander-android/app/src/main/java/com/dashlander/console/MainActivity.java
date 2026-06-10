package com.dashlander.console;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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
    private static final int REQUEST_OPEN_GMD = 4107;

    private TextView log;
    private EditText usernameInput;
    private EditText accountIdInput;
    private EditText onlineNameInput;
    private CheckBox unlistedInput;
    private EditText passwordInput;
    private EditText confirmInput;
    private Button openLocalButton;
    private Button inspectButton;
    private Button previewButton;
    private Button uploadButton;
    private Button viewReceiptButton;
    private Button copyLevelIdButton;
    private Button copyLogButton;
    private GitHubUploadsClient.UploadFile selectedFile;
    private String selectedXml;
    private GdLevelInfo selectedInfo;
    private UploadPreview selectedPreview;
    private String lastLevelId = "";
    private boolean usingLocalSource = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("GMD Uploader\nUpload local or GitHub .gmd files");
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

        openLocalButton = new Button(this);
        openLocalButton.setText("Open local .gmd file");
        root.addView(openLocalButton);

        inspectButton = new Button(this);
        inspectButton.setText("Inspect GitHub uploads");
        root.addView(inspectButton);

        previewButton = new Button(this);
        previewButton.setText("Build upload preview");
        root.addView(previewButton);

        uploadButton = new Button(this);
        uploadButton.setText("UPLOAD to Geometry Dash");
        root.addView(uploadButton);

        viewReceiptButton = new Button(this);
        viewReceiptButton.setText("View last debug receipt");
        root.addView(viewReceiptButton);

        copyLevelIdButton = new Button(this);
        copyLevelIdButton.setText("Copy last level ID");
        root.addView(copyLevelIdButton);

        copyLogButton = new Button(this);
        copyLogButton.setText("Copy debug log");
        root.addView(copyLogButton);

        log = new TextView(this);
        log.setText("Ready. Choose a local .gmd file or inspect GitHub uploads.\n");
        log.setTextSize(14);
        root.addView(log);

        openLocalButton.setOnClickListener(v -> openLocalGmdFile());
        inspectButton.setOnClickListener(v -> inspectUploads());
        previewButton.setOnClickListener(v -> buildUploadPreview());
        uploadButton.setOnClickListener(v -> uploadSelectedLevel());
        viewReceiptButton.setOnClickListener(v -> viewLastReceipt());
        copyLevelIdButton.setOnClickListener(v -> copyLastLevelId());
        copyLogButton.setOnClickListener(v -> copyDebugLog());

        setContentView(scroll);
        updateUiState();
    }

    private void openLocalGmdFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Open .gmd file"), REQUEST_OPEN_GMD);
        } catch (Exception e) {
            append("\nERROR opening Android file picker: " + e.getMessage() + "\n");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_GMD) {
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            append("\nNo local .gmd selected.\n");
            return;
        }

        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        inspectLocalGmd(uri);
    }

    private void inspectLocalGmd(Uri uri) {
        append("\nOpening local .gmd file...\n");
        new Thread(() -> {
            try {
                LocalGmdFile local = LocalGmdReader.read(this, uri);
                applySelectedFile(local.asUploadFile(), local.xml);
                post("\nLoaded local file: " + local.displayName + "\n");
                postInspectionResult();
            } catch (Exception e) {
                post("ERROR loading local .gmd: " + e.getMessage() + "\n");
            }
        }).start();
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

                GitHubUploadsClient.UploadFile firstFile = files.get(0);
                post("\nDownloading first file for inspection: " + firstFile.path + "\n");
                String xml = client.downloadGmd(firstFile);
                applySelectedFile(firstFile, xml);
                postInspectionResult();
            } catch (Exception e) {
                post("ERROR: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void applySelectedFile(GitHubUploadsClient.UploadFile file, String xml) throws Exception {
        selectedFile = file;
        selectedXml = xml;
        selectedInfo = GmdParser.parse(file.path, xml);
        selectedPreview = null;
        lastLevelId = "";
        usingLocalSource = file.path != null && file.path.startsWith("local/");
        runOnUiThread(() -> {
            onlineNameInput.setText(selectedInfo.levelName);
            confirmInput.setText("");
            updateUiState();
        });
    }

    private void postInspectionResult() {
        post("\nInspection result:\n" +
                "Source: " + selectedInfo.sourceName + "\n" +
                "Internal name k2: " + selectedInfo.levelName + "\n" +
                "Original online ID k1: " + blank(selectedInfo.originalLevelId) + "\n" +
                "Objects k48: " + selectedInfo.objects + "\n" +
                "Song k45: " + selectedInfo.songId + "\n" +
                "Audio track k8: " + selectedInfo.audioTrack + "\n" +
                "Level string length: " + selectedInfo.levelStringLength + "\n" +
                "Level string sha256: " + selectedInfo.levelStringHash.substring(0, Math.min(16, selectedInfo.levelStringHash.length())) + "...\n");
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
            append("\nOpen a local .gmd or inspect a GitHub .gmd first.\n");
            return;
        }

        append("\nBuilding create-new upload preview...\n");
        new Thread(() -> {
            try {
                selectedPreview = GdPayloadBuilder.buildPreview(selectedXml, makeSettings());
                post("\n" + selectedPreview.summary());
                post("No password was used. No upload sent yet. Type UPLOAD, then tap upload.\n");
                runOnUiThread(this::updateUiState);
            } catch (Exception e) {
                post("ERROR building preview: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void uploadSelectedLevel() {
        if (selectedXml == null || selectedInfo == null) {
            append("\nOpen a local .gmd or inspect a GitHub .gmd first.\n");
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
                runOnUiThread(MainActivity.this::updateUiState);
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
        copyText("GMD Uploader level ID", id);
        append("\nCopied level ID: " + id + "\n");
        updateUiState();
    }

    private void copyDebugLog() {
        copyText("GMD Uploader debug log", log.getText().toString());
        append("\nCopied debug log to clipboard.\n");
    }

    private void updateUiState() {
        boolean hasFile = selectedInfo != null && selectedXml != null;
        boolean hasPreview = hasFile && selectedPreview != null;
        boolean hasResult = lastLevelId != null && !lastLevelId.trim().isEmpty();

        openLocalButton.setText(hasFile && usingLocalSource ? "Change local .gmd file" : "Open local .gmd file");
        inspectButton.setText(hasFile && !usingLocalSource ? "Refresh GitHub upload" : "Inspect GitHub uploads");

        openLocalButton.setVisibility(!hasFile || usingLocalSource ? View.VISIBLE : View.GONE);
        inspectButton.setVisibility(!hasFile || !usingLocalSource ? View.VISIBLE : View.GONE);

        int settingsVisibility = hasFile ? View.VISIBLE : View.GONE;
        usernameInput.setVisibility(settingsVisibility);
        accountIdInput.setVisibility(settingsVisibility);
        onlineNameInput.setVisibility(settingsVisibility);
        unlistedInput.setVisibility(settingsVisibility);
        passwordInput.setVisibility(settingsVisibility);
        previewButton.setVisibility(settingsVisibility);

        confirmInput.setVisibility(hasPreview ? View.VISIBLE : View.GONE);
        uploadButton.setVisibility(hasPreview ? View.VISIBLE : View.GONE);

        int resultVisibility = hasResult ? View.VISIBLE : View.GONE;
        copyLevelIdButton.setVisibility(resultVisibility);
        viewReceiptButton.setVisibility(resultVisibility);
        copyLogButton.setVisibility(resultVisibility);
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
