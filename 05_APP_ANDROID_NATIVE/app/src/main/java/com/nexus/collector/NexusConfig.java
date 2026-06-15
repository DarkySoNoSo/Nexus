package com.nexus.collector;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class NexusConfig {
    public static final String PREFS = "nexus_collector";
    public static final String DEFAULT_ENDPOINT = "http://100.107.24.67:8081/api/communication/ingest";
    public static final String FALLBACK_ENDPOINT = "http://192.168.1.216:8081/api/communication/ingest";
    public static final String LOCAL_ENDPOINT = "http://192.168.1.216:8081/api/communication/ingest";

    private NexusConfig() {}

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String endpoint(Context context) {
        return normalizeEndpoint(prefs(context).getString("endpoint", DEFAULT_ENDPOINT));
    }

    public static void setEndpoint(Context context, String value) {
        prefs(context).edit().putString("endpoint", normalizeEndpoint(value)).apply();
    }

    public static void rememberWorkingEndpoint(Context context, String endpoint) {
        String clean = normalizeEndpoint(endpoint);
        prefs(context).edit().putString("endpoint", clean).putString("last_ok_endpoint", clean).apply();
    }

    public static void rememberWorkingBaseUrl(Context context, String baseUrl) {
        rememberWorkingEndpoint(context, normalizeBaseUrl(baseUrl) + "/api/communication/ingest");
    }

    public static List<String> endpointCandidates(Context context) {
        SharedPreferences p = prefs(context);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        addEndpoint(set, p.getString("endpoint", ""));
        addEndpoint(set, p.getString("last_ok_endpoint", ""));
        addEndpoint(set, DEFAULT_ENDPOINT);
        addEndpoint(set, FALLBACK_ENDPOINT);
        addEndpoint(set, LOCAL_ENDPOINT);
        return new ArrayList<>(set);
    }

    public static List<String> baseUrlCandidates(Context context) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String endpoint : endpointCandidates(context)) set.add(baseUrl(endpoint));
        return new ArrayList<>(set);
    }

    public static String baseUrl(Context context) { return baseUrl(endpoint(context)); }

    public static String baseUrl(String endpoint) {
        String value = normalizeEndpoint(endpoint);
        if (value.endsWith("/api/communication/ingest")) return value.substring(0, value.length() - "/api/communication/ingest".length());
        int marker = value.indexOf("/api/");
        if (marker > 0) return value.substring(0, marker);
        return normalizeBaseUrl(value);
    }

    public static String normalizeBaseUrl(String value) {
        if (value == null) value = "";
        String clean = value.trim();
        if (clean.isEmpty()) return "http://100.107.24.67:8081";
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) clean = "http://" + clean;
        while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
        int marker = clean.indexOf("/api/");
        if (marker > 0) clean = clean.substring(0, marker);
        return clean;
    }

    public static String normalizeEndpoint(String value) {
        String base = normalizeBaseUrl(value);
        return base + "/api/communication/ingest";
    }

    public static void setLastWidgetStatus(Context context, String value) { prefs(context).edit().putString("last_widget_status", value == null ? "" : value).apply(); }
    public static String lastWidgetStatus(Context context) { return prefs(context).getString("last_widget_status", ""); }
    public static void setLastSendStatus(Context context, String value) { prefs(context).edit().putString("last_send_status", value == null ? "" : value).apply(); }
    public static String lastSendStatus(Context context) { return prefs(context).getString("last_send_status", ""); }
    public static void setLastAppDataStatus(Context context, String value) { prefs(context).edit().putString("last_app_data_status", value == null ? "" : value).apply(); }
    public static String lastAppDataStatus(Context context) { return prefs(context).getString("last_app_data_status", ""); }

    private static void addEndpoint(LinkedHashSet<String> set, String value) {
        if (value == null || value.trim().isEmpty()) return;
        set.add(normalizeEndpoint(value));
    }

    public static boolean enabled(Context context) { return prefs(context).getBoolean("enabled", true); }
    public static void setEnabled(Context context, boolean enabled) { prefs(context).edit().putBoolean("enabled", enabled).apply(); }
    public static void increment(Context context, String key) { SharedPreferences p = prefs(context); p.edit().putLong(key, p.getLong(key, 0L) + 1L).apply(); }
    public static long count(Context context, String key) { return prefs(context).getLong(key, 0L); }
}
