package com.dashlander.console;

import android.content.Context;

public final class DashlanderUploadController {
    public interface Callback {
        void log(String message);
        void done(UploadResult result);
    }

    private final Context context;

    public DashlanderUploadController(Context context) {
        this.context = context.getApplicationContext();
    }

    public void upload(
            GitHubUploadsClient.UploadFile selectedFile,
            GdLevelInfo selectedInfo,
            UploadPreview preview,
            UploadSettings settings,
            String password,
            Callback callback
    ) {
        new Thread(() -> {
            UploadResult result = new UploadResult();
            try {
                if (selectedFile == null || selectedInfo == null || preview == null) {
                    result.error = "Inspect and preview a .gmd before upload.";
                    callback.done(result);
                    return;
                }
                if (password == null || password.trim().isEmpty()) {
                    result.error = "Password is empty.";
                    callback.done(result);
                    return;
                }
                if (settings.username == null || settings.username.trim().isEmpty()) {
                    result.error = "Username is empty.";
                    callback.done(result);
                    return;
                }

                BoomlingsClient client = new BoomlingsClient();
                String accountId = settings.accountId == null ? "" : settings.accountId.trim();
                if (accountId.isEmpty()) {
                    callback.log("Looking up account ID for " + settings.username + "...\n");
                    BoomlingsClient.AccountLookupResult lookup = client.lookupAccountId(settings.username);
                    for (UploadAttempt attempt : lookup.attempts) {
                        callback.log("lookup status=" + attempt.status + " preview=" + attempt.preview() + "\n");
                    }
                    if (!lookup.success) {
                        result.error = lookup.error;
                        result.attempts.addAll(lookup.attempts);
                        DebugReceiptWriter.write(context, selectedFile, selectedInfo, preview, result);
                        callback.done(result);
                        return;
                    }
                    accountId = lookup.accountId;
                    callback.log("Using accountID: " + accountId + "\n");
                }

                preview.payload.put("accountID", accountId);
                preview.payload.put("userName", settings.username);
                result = client.upload(preview.payload, password);
                DebugReceiptWriter.write(context, selectedFile, selectedInfo, preview, result);
                callback.done(result);
            } catch (Exception e) {
                result.error = e.toString();
                try {
                    DebugReceiptWriter.write(context, selectedFile, selectedInfo, preview, result);
                } catch (Exception ignored) {
                }
                callback.done(result);
            }
        }).start();
    }
}
