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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN_GMD = 4107;

    private TextView log;
    private EditText usernameInput;
    private EditText accountIdInput;
    private EditText onlineNameInput;
    private TextView visibilityLabel;
    private RadioGroup visibilityGroup;
    private RadioButton publicVisibilityInput;
    private RadioButton friendsVisibilityInput;
    private RadioButton unlistedVisibilityInput;
    private TextView copyPasswordLabel;
    private RadioGroup copyPasswordGroup;
    private RadioButton freeCopyInput;
    private RadioButton noCopyInput;
    private RadioButton customCopyInput;
    private EditText customCopyPasswordInput;
    private EditText passwordInput;
    private EditText confirmInput;
    private Button openLocalButton;
    private Button healthCheckButton;
    private Button previewButton;
    private Button uploadButton;
    private Button viewReceiptButton;
    private Button shareReceiptButton;
    private Button copyLevelIdButton;
    private Button copyLogButton;
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
        title.setText("GMD Uploader\nUpload local .gmd files");
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

        visibilityLabel = new TextView(this);
        visibilityLabel.setText("Geometry Dash visibility");
        root.addView(visibilityLabel);

        visibilityGroup = new RadioGroup(this);
        visibilityGroup.setOrientation(RadioGroup.VERTICAL);
        publicVisibilityInput = new RadioButton(this);
        publicVisibilityInput.setId(View.generateViewId());
        publicVisibilityInput.setText("Public / Listed");
        visibilityGroup.addView(publicVisibilityInput);

        friendsVisibilityInput = new RadioButton(this);
        friendsVisibilityInput.setId(View.generateViewId());
        friendsVisibilityInput.setText("Friends only / Legacy hidden");
        visibilityGroup.addView(friendsVisibilityInput);

        unlistedVisibilityInput = new RadioButton(this);
        unlistedVisibilityInput.setId(View.generateViewId());
        unlistedVisibilityInput.setText("Unlisted by ID/share");
        visibilityGroup.addView(unlistedVisibilityInput);
        friendsVisibilityInput.setChecked(true);
        root.addView(visibilityGroup);

        copyPasswordLabel = new TextView(this);
        copyPasswordLabel.setText("Copy password setting");
        root.addView(copyPasswordLabel);

        copyPasswordGroup = new RadioGroup(this);
        copyPasswordGroup.setOrientation(RadioGroup.VERTICAL);
        freeCopyInput = new RadioButton(this);
        freeCopyInput.setId(View.generateViewId());
        freeCopyInput.setText("Free copy");
        copyPasswordGroup.addView(freeCopyInput);

        noCopyInput = new RadioButton(this);
        noCopyInput.setId(View.generateViewId());
        noCopyInput.setText("No copy");
        copyPasswordGroup.addView(noCopyInput);

        customCopyInput = new RadioButton(this);
        customCopyInput.setId(View.generateViewId());
        customCopyInput.setText("Custom copy password");
        copyPasswordGroup.addView(customCopyInput);
        freeCopyInput.setChecked(true);
        root.addView(copyPasswordGroup);

        customCopyPasswordInput = new EditText(this);
        customCopyPasswordInput.setHint("Custom copy password number");
        customCopyPasswordInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(customCopyPasswordInput);
        copyPasswordGroup.setOnCheckedChangeListener((group, checkedId) -> updateUiState());

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

        healthCheckButton = new Button(this);
        healthCheckButton.setText("Run health check");
        root.addView(healthCheckButton);

        previewButton = new Button(this);
        previewButton.setText("Build upload preview");
        root.addView(previewButton);

        uploadButton = new Button(this);
        uploadButton.setText("UPLOAD to Geometry Dash");
        root.addView(uploadButton);

        viewReceiptButton = new Button(this);
        viewReceiptButton.setText("View last debug receipt");
        root.addView(viewReceiptButton);

        shareReceiptButton = new Button(this);
        shareReceiptButton.setText("Share debug receipt");
        root.addView(shareReceiptButton);

        copyLevelIdButton = new Button(this);
        copyLevelIdButton.setText("Copy last level ID");
        root.addView(copyLevelIdButton);

        copyLogButton = new Button(this);
        copyLogButton.setText("Copy debug log");
        root.addView(copyLogButton);

        log = new TextView(this);
        log.setText("Ready. Open a local .gmd file.\n");
        log.setTextSize(14);
        root.addView(log);

        openLocalButton.setOnClickListener(v -> openLocalGmdFile());
        healthCheckButton.setOnClickListener(v -> runHealthCheck());
        previewButton.setOnClickListener(v -> buildUploadPreview());
        uploadButton.setOnClickListener(v -> uploadSelectedLevel());
        viewReceiptButton.setOnClickListener(v -> viewLastReceipt());
        shareReceiptButton.setOnClickListener(v -> shareLastReceipt());
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

    private void applySelectedFile(GitHubUploadsClient.UploadFile file, String xml) throws Exception {
        selectedFile = file;
        selectedXml = xml;
        selectedInfo = GmdParser.parse(file.path, xml);
        selectedPreview = null;
        lastLevelId = "";
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
        settings.unlistedValue = selectedVisibilityValue();
        settings.unlisted = !"0".equals(settings.unlistedValue);
        settings.copyPasswordValue = selectedCopyPasswordValue();
        settings.forceStockSong = false;
        settings.audioTrackOverride = "";
        settings.songIdOverride = "";
        return settings;
    }

    private String selectedVisibilityValue() {
        int checkedId = visibilityGroup.getCheckedRadioButtonId();
        if (checkedId == publicVisibilityInput.getId()) return "0";
        if (checkedId == unlistedVisibilityInput.getId()) return "2";
        return "1";
    }

    private String selectedCopyPasswordValue() {
        int checkedId = copyPasswordGroup.getCheckedRadioButtonId();
        if (checkedId == noCopyInput.getId()) return "0";
        if (checkedId == customCopyInput.getId()) return customCopyPasswordInput.getText().toString().trim();
        return "1";
    }

    private boolean validateCopyPasswordSelection() {
        if (copyPasswordGroup.getCheckedRadioButtonId() != customCopyInput.getId()) return true;
        String value = customCopyPasswordInput.getText().toString().trim();
        if (value.isEmpty()) {
            append("\nCustom copy password is empty. Choose Free copy, No copy, or enter a number.\n");
            return false;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw new NumberFormatException();
            return true;
        } catch (Exception e) {
            append("\nCustom copy password must be a non-negative number.\n");
            return false;
        }
    }

    private void buildUploadPreview() {
        if (selectedXml == null || selectedInfo == null) {
            append("\nOpen a local .gmd first.\n");
            return;
        }
        if (!validateCopyPasswordSelection()) {
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
            append("\nOpen a local .gmd first.\n");
            return;
        }
        if (!"UPLOAD".equals(confirmInput.getText().toString().trim())) {
            append("\nType UPLOAD in the confirmation box first.\n");
            return;
        }
        if (!validateCopyPasswordSelection()) {
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
                    post("Upload accepted by server.\nLevel ID: " + result.levelId + "\n");
                    if (result.verificationAttempted && result.verificationFound) {
                        post("Verification: PASSED. Level resolves as: " + result.verificationLevelName + "\n");
                    } else if (result.verificationAttempted) {
                        post("Verification: WARNING. Exact ID did not resolve yet. " + result.verificationWarning + "\n");
                    } else {
                        post("Verification: not attempted.\n");
                    }
                    post("Search this exact ID in Geometry Dash. Do not search by name.\n");
                } else {
                    post("ERROR: " + result.error + "\n");
                }
                runOnUiThread(MainActivity.this::updateUiState);
            }
        });
    }

    private void runHealthCheck() {
        String username = usernameInput.getText().toString().trim();
        append("\nRunning health check...\n");
        new Thread(() -> {
            try {
                BoomlingsClient client = new BoomlingsClient();
                if (username.isEmpty()) {
                    post("Username is empty. Account lookup skipped.\n");
                } else {
                    post("Checking Boomlings account lookup for " + username + "...\n");
                    BoomlingsClient.AccountLookupResult lookup = client.lookupAccountId(username);
                    for (UploadAttempt attempt : lookup.attempts) {
                        post("lookup status=" + attempt.status + " preview=" + attempt.preview() + "\n");
                    }
                    if (lookup.success) {
                        post("Account lookup: PASSED. accountID=" + lookup.accountId + "\n");
                    } else {
                        post("Account lookup: WARNING. " + lookup.error + "\n");
                    }
                }

                String id = getLastLevelIdCandidate();
                if (id.isEmpty()) {
                    post("Exact-ID verification: skipped. No previous level ID found.\n");
                } else {
                    post("Checking exact-ID verification for " + id + "...\n");
                    BoomlingsClient.LevelVerifyResult verify = client.verifyLevelId(id);
                    for (UploadAttempt attempt : verify.attempts) {
                        post("verify status=" + attempt.status + " preview=" + attempt.preview() + "\n");
                    }
                    if (verify.found) {
                        post("Exact-ID verification: PASSED. Level resolves as: " + verify.levelName + "\n");
                    } else {
                        post("Exact-ID verification: WARNING. " + verify.error + "\n");
                    }
                }
                post("Health check finished. No upload was sent.\n");
            } catch (Exception e) {
                post("Health check ERROR: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private String getLastLevelIdCandidate() {
        String id = lastLevelId == null ? "" : lastLevelId.trim();
        if (!id.isEmpty()) return id;
        try {
            String receipt = readInternalFile("last_upload_debug.json");
            JSONObject json = new JSONObject(receipt);
            return json.optString("levelId", "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void viewLastReceipt() {
        try {
            String receipt = readInternalFile("last_upload_debug.json");
            append("\nLast debug receipt:\n" + receipt + "\n");
        } catch (Exception e) {
            append("\nNo debug receipt found yet. Upload once first.\n");
        }
    }

    private void shareLastReceipt() {
        try {
            String receipt = readInternalFile("last_upload_debug.json");
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "GMD Uploader debug receipt");
            send.putExtra(Intent.EXTRA_TEXT, receipt);
            startActivity(Intent.createChooser(send, "Share debug receipt"));
        } catch (Exception e) {
            append("\nNo debug receipt found yet. Upload once first.\n");
        }
    }

    private void copyLastLevelId() {
        String id = getLastLevelIdCandidate();
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
        boolean hasResult = !getLastLevelIdCandidate().isEmpty();
        boolean customCopySelected = copyPasswordGroup != null && copyPasswordGroup.getCheckedRadioButtonId() == customCopyInput.getId();

        openLocalButton.setText(hasFile ? "Change local .gmd file" : "Open local .gmd file");
        openLocalButton.setVisibility(View.VISIBLE);
        healthCheckButton.setVisibility(View.VISIBLE);

        int settingsVisibility = hasFile ? View.VISIBLE : View.GONE;
        usernameInput.setVisibility(settingsVisibility);
        accountIdInput.setVisibility(settingsVisibility);
        onlineNameInput.setVisibility(settingsVisibility);
        visibilityLabel.setVisibility(settingsVisibility);
        visibilityGroup.setVisibility(settingsVisibility);
        copyPasswordLabel.setVisibility(settingsVisibility);
        copyPasswordGroup.setVisibility(settingsVisibility);
        customCopyPasswordInput.setVisibility(hasFile && customCopySelected ? View.VISIBLE : View.GONE);
        passwordInput.setVisibility(settingsVisibility);
        previewButton.setVisibility(settingsVisibility);

        confirmInput.setVisibility(hasPreview ? View.VISIBLE : View.GONE);
        uploadButton.setVisibility(hasPreview ? View.VISIBLE : View.GONE);

        int resultVisibility = hasResult ? View.VISIBLE : View.GONE;
        copyLevelIdButton.setVisibility(resultVisibility);
        viewReceiptButton.setVisibility(resultVisibility);
        shareReceiptButton.setVisibility(resultVisibility);
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
