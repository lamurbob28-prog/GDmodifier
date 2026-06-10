package com.dashlander.console;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class DebugReceiptWriter {
    private DebugReceiptWriter() {}

    public static File write(Context context, GitHubUploadsClient.UploadFile file, GdLevelInfo info, UploadPreview preview, UploadResult result) throws Exception {
        JSONObject root = new JSONObject();
        root.put("sourcePath", file == null ? "" : file.path);
        root.put("sourceRawUrl", file == null ? "" : file.rawUrl);

        JSONObject inspection = new JSONObject();
        if (info != null) {
            inspection.put("k1_originalLevelId", info.originalLevelId);
            inspection.put("k2_levelName", info.levelName);
            inspection.put("k5_creator", info.creator);
            inspection.put("k45_songId", info.songId);
            inspection.put("k8_audioTrack", info.audioTrack);
            inspection.put("k48_objects", info.objects);
            inspection.put("levelStringLength", info.levelStringLength);
            inspection.put("levelStringHash", info.levelStringHash);
        }
        root.put("inspection", inspection);

        JSONObject payload = new JSONObject();
        if (preview != null && preview.payload != null) {
            payload.put("levelID", preview.payload.get("levelID"));
            payload.put("levelName", preview.payload.get("levelName"));
            payload.put("objects", preview.payload.get("objects"));
            payload.put("songID", preview.payload.get("songID"));
            payload.put("audioTrack", preview.payload.get("audioTrack"));
            payload.put("unlisted", preview.payload.get("unlisted"));
            payload.put("levelString", "<omitted length=" + preview.levelStringLength + " sha256=" + preview.levelStringHash + ">");
        }
        root.put("payloadPreview", payload);

        if (result != null) {
            root.put("success", result.success);
            root.put("levelId", result.levelId);
            root.put("error", result.error);
            JSONArray attempts = new JSONArray();
            for (UploadAttempt a : result.attempts) {
                JSONObject item = new JSONObject();
                item.put("endpoint", a.endpoint);
                item.put("url", a.url);
                item.put("status", a.status);
                item.put("userAgent", a.userAgent);
                item.put("blockedLooking", a.blockedLooking());
                item.put("elapsedMs", a.elapsedMs);
                item.put("preview", a.preview());
                attempts.put(item);
            }
            root.put("attempts", attempts);
        }

        File fileOut = new File(context.getFilesDir(), "last_upload_debug.json");
        try (FileOutputStream fos = new FileOutputStream(fileOut)) {
            fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return fileOut;
    }
}
