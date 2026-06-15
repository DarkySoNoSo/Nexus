package com.nexus.collector;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NexusMessagesWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.nexus.collector.WIDGET_REFRESH";
    public static final String ACTION_MESSAGE = "com.nexus.collector.WIDGET_MESSAGE_ACTION";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_ACTION = "action";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        updateAsync(context.getApplicationContext(), widgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null) return;
        if (ACTION_REFRESH.equals(intent.getAction())) {
            updateAll(context);
            return;
        }
        if (ACTION_MESSAGE.equals(intent.getAction())) {
            handleMessageAction(context.getApplicationContext(), intent.getStringExtra(EXTRA_EVENT_ID), intent.getStringExtra(EXTRA_ACTION));
        }
    }

    public static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        ComponentName component = new ComponentName(app, NexusMessagesWidgetProvider.class);
        updateAsync(app, manager.getAppWidgetIds(component));
    }

    private static void updateAsync(Context context, int[] widgetIds) {
        if (widgetIds == null || widgetIds.length == 0) return;
        Context app = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        for (int widgetId : widgetIds) manager.updateAppWidget(widgetId, buildViews(app, WidgetState.loading()));
        EXECUTOR.execute(() -> {
            WidgetState state = fetchState(app);
            for (int widgetId : widgetIds) manager.updateAppWidget(widgetId, buildViews(app, state));
        });
    }

    private static void handleMessageAction(Context context, String eventId, String action) {
        if (eventId == null || eventId.trim().isEmpty() || action == null || action.trim().isEmpty()) {
            NexusConfig.setLastWidgetStatus(context, "Widget-Aktion ohne Event");
            updateAll(context);
            return;
        }

        // Sofort lokal ausblenden. Der Server kann nachziehen; die Bedienung bleibt trotzdem stabil.
        hideLocal(context, eventId);
        NexusConfig.setLastWidgetStatus(context, actionLabel(action) + " lokal markiert");
        updateAll(context);

        EXECUTOR.execute(() -> {
            String lastError = "";
            for (String base : NexusConfig.baseUrlCandidates(context)) {
                try {
                    String body = "event_id=" + encode(eventId) + "&action=" + encode(action) + "&scope=conversation";
                    String response = httpPost(base + "/api/widget/message-action", body);
                    JSONObject root = new JSONObject(response);
                    if (root.optBoolean("ok", false)) {
                        NexusConfig.rememberWorkingBaseUrl(context, base);
                        NexusConfig.setLastWidgetStatus(context, actionLabel(action) + " OK " + hostLabel(base));
                        updateAll(context);
                        return;
                    }
                    lastError = hostLabel(base) + ": " + root.optString("message", "ok=false");
                } catch (Exception ex) {
                    lastError = hostLabel(base) + ": " + ex.getClass().getSimpleName();
                }
            }
            NexusConfig.setLastWidgetStatus(context, actionLabel(action) + " lokal erledigt; Server offen " + lastError);
            updateAll(context);
        });
    }

    private static RemoteViews buildViews(Context context, WidgetState state) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.nexus_messages_widget);
        views.setTextViewText(R.id.widgetTitle, "NEXUS");
        views.setTextViewText(R.id.widgetStatus, state.statusLine);
        views.setTextViewText(R.id.widgetUpdated, state.updatedLine);
        bindMessage(views, context, 0, state.items[0], R.id.msg1Row, R.id.msg1Title, R.id.msg1Body, R.id.msg1Important, R.id.msg1Focus, R.id.msg1Ok);
        bindMessage(views, context, 1, state.items[1], R.id.msg2Row, R.id.msg2Title, R.id.msg2Body, R.id.msg2Important, R.id.msg2Focus, R.id.msg2Ok);
        bindMessage(views, context, 2, state.items[2], R.id.msg3Row, R.id.msg3Title, R.id.msg3Body, R.id.msg3Important, R.id.msg3Focus, R.id.msg3Ok);
        bindMessage(views, context, 3, state.items[3], R.id.msg4Row, R.id.msg4Title, R.id.msg4Body, R.id.msg4Important, R.id.msg4Focus, R.id.msg4Ok);
        bindMessage(views, context, 4, state.items[4], R.id.msg5Row, R.id.msg5Title, R.id.msg5Body, R.id.msg5Important, R.id.msg5Focus, R.id.msg5Ok);
        Intent refreshIntent = new Intent(context, NexusMessagesWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPending = PendingIntent.getBroadcast(context, 808105, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRefresh, refreshPending);
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(context, 808106, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetOpen, openPending);
        return views;
    }

    private static void bindMessage(RemoteViews views, Context context, int index, WidgetItem item, int rowId, int titleId, int bodyId, int importantId, int focusId, int okId) {
        if (item == null || item.eventId.isEmpty()) {
            views.setViewVisibility(rowId, View.GONE);
            return;
        }
        views.setViewVisibility(rowId, View.VISIBLE);
        views.setTextViewText(titleId, (index + 1) + ". [" + item.band + "] " + item.sender);
        views.setTextViewText(bodyId, item.body);
        views.setTextViewText(importantId, "Wichtig");
        views.setTextViewText(focusId, "Pin");
        views.setTextViewText(okId, "OK");
        views.setOnClickPendingIntent(importantId, actionPendingIntent(context, item.eventId, "very_important", 8800 + index));
        views.setOnClickPendingIntent(focusId, actionPendingIntent(context, item.eventId, "timeline_focus", 9000 + index));
        views.setOnClickPendingIntent(okId, actionPendingIntent(context, item.eventId, "done", 9100 + index));
    }

    private static PendingIntent actionPendingIntent(Context context, String eventId, String action, int requestCode) {
        Intent intent = new Intent(context, NexusMessagesWidgetProvider.class);
        intent.setAction(ACTION_MESSAGE);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_ACTION, action);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static WidgetState fetchState(Context context) {
        String lastError = "";
        for (String base : NexusConfig.baseUrlCandidates(context)) {
            try {
                JSONObject root = new JSONObject(httpGet(base + "/api/widget/messages?limit=12"));
                if (!root.optBoolean("ok", false)) {
                    lastError = hostLabel(base) + ": ungueltig";
                    continue;
                }
                NexusConfig.rememberWorkingBaseUrl(context, base);
                NexusConfig.setLastWidgetStatus(context, "OK " + hostLabel(base));
                return parseState(context, root);
            } catch (Exception ex) {
                lastError = hostLabel(base) + ": " + ex.getClass().getSimpleName();
            }
        }
        NexusConfig.setLastWidgetStatus(context, "Fehler " + lastError);
        return WidgetState.error("Nicht erreichbar: " + lastError);
    }

    private static WidgetState parseState(Context context, JSONObject root) {
        JSONObject counters = root.optJSONObject("counters");
        JSONArray items = root.optJSONArray("items");
        int focus = counters == null ? 0 : counters.optInt("focus", 0);
        int alerts = counters == null ? 0 : counters.optInt("alerts", 0);
        int replies = counters == null ? 0 : counters.optInt("needs_reply", 0);
        WidgetState state = WidgetState.ready("Fokus " + focus + " | Alarm " + alerts + " | Antwort " + replies);
        if (items == null || items.length() == 0) {
            state.items[0] = new WidgetItem("", "P0", "Keine Fokusnachricht", "Nexus ist bereit.");
            return state;
        }
        int slot = 0;
        for (int i = 0; i < items.length() && slot < 5; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String eventId = item.optString("event_id", "");
            if (isHidden(context, eventId)) continue;
            state.items[slot] = new WidgetItem(eventId, item.optString("priority_band", "P?"), shorten(item.optString("sender", "Unbekannt"), 22), shorten(item.optString("body_preview", ""), 88));
            slot++;
        }
        if (slot == 0) state.items[0] = new WidgetItem("", "P0", "Keine offene Fokusnachricht", "Ausgeblendete Nachrichten bleiben lokal verborgen.");
        return state;
    }

    private static boolean isHidden(Context context, String eventId) {
        return eventId != null && !eventId.isEmpty() && NexusConfig.prefs(context).getBoolean("hidden_message_" + eventId, false);
    }

    private static void hideLocal(Context context, String eventId) {
        if (eventId != null && !eventId.isEmpty()) NexusConfig.prefs(context).edit().putBoolean("hidden_message_" + eventId, true).apply();
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(6000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Nexus-Collector", "nexus-collector-widget-v3");
            int code = conn.getResponseCode();
            String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " " + shorten(body, 80));
            return body;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String httpPost(String url, String body) throws Exception {
        HttpURLConnection conn = null;
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(6000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            conn.setRequestProperty("X-Nexus-Collector", "nexus-collector-widget-v3");
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = conn.getOutputStream()) { output.write(bytes); }
            int code = conn.getResponseCode();
            String response = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " " + shorten(response, 80));
            return response;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String encode(String value) throws Exception { return URLEncoder.encode(value == null ? "" : value, "UTF-8"); }

    private static String actionLabel(String action) {
        if ("timeline_focus".equals(action)) return "Fokus";
        if ("very_important".equals(action)) return "Sehr wichtig";
        if ("done".equals(action)) return "Erledigt";
        if ("not_important".equals(action)) return "Nicht wichtig";
        return action == null ? "" : action;
    }

    private static String hostLabel(String base) {
        try {
            URL url = new URL(base);
            int port = url.getPort();
            return url.getHost() + (port > 0 ? ":" + port : "");
        } catch (Exception ex) {
            return base == null ? "" : base;
        }
    }

    private static String shorten(String value, int max) {
        if (value == null) return "";
        String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 3) + "...";
    }

    private static String nowLine() { return "Aktualisiert " + new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date()); }

    private static final class WidgetState {
        final String statusLine;
        final String updatedLine;
        final WidgetItem[] items = new WidgetItem[5];
        private WidgetState(String statusLine) { this.statusLine = statusLine; this.updatedLine = nowLine(); }
        static WidgetState loading() { WidgetState state = new WidgetState("Lade Nexus..."); state.items[0] = new WidgetItem("", "P?", "Nexus wird abgefragt", ""); return state; }
        static WidgetState ready(String status) { return new WidgetState(status); }
        static WidgetState error(String body) { WidgetState state = new WidgetState("Fehler"); state.items[0] = new WidgetItem("", "!", "Nexus nicht aktuell", shorten(body, 160)); return state; }
    }

    private static final class WidgetItem {
        final String eventId;
        final String band;
        final String sender;
        final String body;
        WidgetItem(String eventId, String band, String sender, String body) {
            this.eventId = eventId == null ? "" : eventId;
            this.band = band == null ? "P?" : band;
            this.sender = sender == null ? "" : sender;
            this.body = body == null ? "" : body;
        }
    }
}
