package com.dashlander.console;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

public final class GmdParser {
    private GmdParser() {}

    public static GdLevelInfo parse(String sourceName, String xml) throws Exception {
        Map<String, String> data = parsePlist(xml);
        String levelString = data.getOrDefault("k4", "");

        if (!data.containsKey("k2") || levelString.length() < 20) {
            throw new IllegalArgumentException("This does not look like a normal .gmd export. Missing k2 or k4.");
        }

        GdLevelInfo info = new GdLevelInfo();
        info.sourceName = sourceName;
        info.originalLevelId = data.getOrDefault("k1", "");
        info.levelName = data.getOrDefault("k2", "");
        info.creator = data.getOrDefault("k5", "");
        info.songId = data.getOrDefault("k45", "0");
        info.audioTrack = data.getOrDefault("k8", "0");
        info.songIds = data.getOrDefault("k104", "");
        info.sfxIds = data.getOrDefault("k105", "");
        info.objects = data.getOrDefault("k48", "0");
        info.levelLength = data.getOrDefault("k23", "0");
        info.levelStringLength = levelString.length();
        info.levelStringHash = sha256(levelString);
        return info;
    }

    public static Map<String, String> parsePlist(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
        }

        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
        Element root = document.getDocumentElement();
        Node container = findTopLevelDictionary(root);

        NodeList children = container.getChildNodes();
        Map<String, String> out = new HashMap<>();
        String pendingKey = null;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String tag = child.getNodeName();
            String text = child.getTextContent() == null ? "" : child.getTextContent();

            if ("k".equals(tag) || "key".equals(tag)) {
                pendingKey = text;
            } else if (pendingKey != null) {
                if ("s".equals(tag) || "string".equals(tag) || "i".equals(tag) || "integer".equals(tag)) {
                    out.put(pendingKey, text);
                } else if ("t".equals(tag) || "true".equals(tag)) {
                    out.put(pendingKey, "1");
                } else if ("f".equals(tag) || "false".equals(tag)) {
                    out.put(pendingKey, "0");
                } else {
                    out.put(pendingKey, text);
                }
                pendingKey = null;
            }
        }
        return out;
    }

    private static Node findTopLevelDictionary(Element root) {
        String rootTag = root.getNodeName();
        if ("d".equals(rootTag) || "dict".equals(rootTag)) {
            return root;
        }

        NodeList dicts = root.getElementsByTagName("dict");
        if (dicts.getLength() > 0) {
            return dicts.item(0);
        }

        NodeList compactDicts = root.getElementsByTagName("d");
        if (compactDicts.getLength() > 0) {
            return compactDicts.item(0);
        }

        throw new IllegalArgumentException("No .gmd dictionary found. Expected <d>, <dict>, or <plist><dict>.");
    }

    private static String sha256(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }
}
