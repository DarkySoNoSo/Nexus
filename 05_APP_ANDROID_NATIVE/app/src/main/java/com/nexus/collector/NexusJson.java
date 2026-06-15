package com.nexus.collector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class NexusJson {
    private NexusJson() {}

    public static String iso(long timestampMs) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        fmt.setTimeZone(TimeZone.getDefault());
        return fmt.format(new Date(timestampMs > 0 ? timestampMs : System.currentTimeMillis()));
    }

    public static String normalizeSender(String value) {
        if (value == null || value.trim().isEmpty()) return "unknown";
        String n = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return n.isEmpty() ? "unknown" : n;
    }

    public static JSONObject baseEvent(String timestamp, String source, String channelType, String sender, String title, String body) throws Exception {
        JSONObject event = new JSONObject();
        event.put("timestamp", timestamp);
        event.put("source", source);
        event.put("channel_type", channelType);
        event.put("sender_raw", sender == null ? JSONObject.NULL : sender);
        event.put("sender_normalized", normalizeSender(sender));
        event.put("title", title == null ? JSONObject.NULL : title);
        event.put("body", body == null ? JSONObject.NULL : body);
        JSONObject semantic = new JSONObject();
        semantic.put("category", JSONObject.NULL);
        semantic.put("priority_score", JSONObject.NULL);
        semantic.put("requires_response", JSONObject.NULL);
        semantic.put("detected_entities", new JSONArray());
        semantic.put("sentiment", JSONObject.NULL);
        semantic.put("risk_flags", new JSONArray());
        event.put("semantic", semantic);
        JSONObject context = new JSONObject();
        context.put("known_person", JSONObject.NULL);
        context.put("relationship", JSONObject.NULL);
        context.put("active_topic", JSONObject.NULL);
        context.put("importance_rule", JSONObject.NULL);
        event.put("context_match", context);
        JSONObject status = new JSONObject();
        status.put("processed", false);
        status.put("archived", false);
        status.put("needs_review", true);
        event.put("status", status);
        event.put("sensitivity", "private");
        return event;
    }
}
