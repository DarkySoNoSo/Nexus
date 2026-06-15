package com.nexus.collector;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NexusEventSender {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String OUTBOX_FILE = "nexus_communication_outbox.jsonl";

    private NexusEventSender() {}

    public static void sendAsync(Context context, String json) {
        Context app = context.getApplicationContext();
        if (!NexusConfig.enabled(app)) {
            appendOutbox(app, json);
            return;
        }
        EXECUTOR.execute(() -> {
            boolean ok = postFirstAvailable(app, json);
            if (ok) {
                NexusConfig.increment(app, "sent_count");
                retryOutbox(app);
            } else {
                NexusConfig.increment(app, "queued_count");
                appendOutbox(app, json);
            }
        });
    }

    public static void retryOutbox(Context context) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            File file = outbox(app);
            if (!file.exists() || file.length() == 0) return;
            List<String> remaining = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    if (postFirstAvailable(app, line)) NexusConfig.increment(app, "sent_count");
                    else remaining.add(line);
                }
            } catch (Exception ignored) { return; }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (String line : remaining) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (Exception ignored) {}
        });
    }

    public static long outboxBytes(Context context) {
        File file = outbox(context.getApplicationContext());
        return file.exists() ? file.length() : 0L;
    }

    public static int outboxEvents(Context context) {
        File file = outbox(context.getApplicationContext());
        if (!file.exists() || file.length() == 0) return 0;
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) if (!line.trim().isEmpty()) count++;
        } catch (Exception ignored) {}
        return count;
    }

    private static boolean post(Context context, String endpoint, String json) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("X-Nexus-Collector", "nexus-collector-apk-v1");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) { os.write(bytes); }
            int code = conn.getResponseCode();
            String body = code < 200 || code >= 300 ? shorten(readAll(conn.getErrorStream()), 120) : "";
            NexusConfig.setLastSendStatus(context, hostLabel(endpoint) + " HTTP " + code + (body.isEmpty() ? "" : " " + body));
            return code >= 200 && code < 300;
        } catch (Exception ex) {
            NexusConfig.setLastSendStatus(context, hostLabel(endpoint) + " " + ex.getClass().getSimpleName() + ": " + shorten(ex.getMessage(), 120));
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static boolean postFirstAvailable(Context context, String json) {
        for (String endpoint : NexusConfig.endpointCandidates(context)) {
            if (post(context, endpoint, json)) {
                NexusConfig.rememberWorkingEndpoint(context, endpoint);
                return true;
            }
        }
        return false;
    }

    private static synchronized void appendOutbox(Context context, String json) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outbox(context), true))) {
            writer.write(json);
            writer.newLine();
        } catch (Exception ignored) {}
    }

    private static File outbox(Context context) { return new File(context.getFilesDir(), OUTBOX_FILE); }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String shorten(String value, int max) {
        if (value == null) return "";
        String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 3) + "...";
    }

    private static String hostLabel(String endpoint) {
        try {
            URL url = new URL(endpoint);
            int port = url.getPort();
            return url.getHost() + (port > 0 ? ":" + port : "");
        } catch (Exception ex) {
            return endpoint == null ? "" : endpoint;
        }
    }
}
