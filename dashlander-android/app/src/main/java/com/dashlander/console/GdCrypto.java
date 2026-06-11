package com.dashlander.console;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class GdCrypto {
    private GdCrypto() {}

    public static String sha1Hex(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }

    public static String sha256Hex(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }

    public static String xor(String text, String key) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            out.append((char) (text.charAt(i) ^ key.charAt(i % key.length())));
        }
        return out.toString();
    }

    public static String b64Url(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    public static String oldGjp(String password) {
        return b64Url(xor(password, "37526").getBytes(StandardCharsets.UTF_8));
    }

    public static String gjp2(String password) throws Exception {
        return sha1Hex(password + "mI29fmAnxgTs");
    }

    public static String chk(String raw, String key, String salt) throws Exception {
        String digest = sha1Hex(raw + salt);
        return b64Url(xor(digest, key).getBytes(StandardCharsets.UTF_8));
    }

    public static String b64Description(String text) {
        return b64Url(text.getBytes(StandardCharsets.UTF_8));
    }
}
