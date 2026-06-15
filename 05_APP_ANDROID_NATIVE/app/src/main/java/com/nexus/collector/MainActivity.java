package com.nexus.collector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

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

public final class MainActivity extends Activity {
    private LinearLayout root;
    private LinearLayout messageList;
    private LinearLayout page;
    private TextView messageSummary;
    private TextView chefLog;
    private TextView pageTitle;
    private TextView pageBody;
    private TextView statusText;
    private EditText chefInput;
    private EditText endpointInput;
    private WebView webView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
        loadChefLog();
        loadMessages();
        showOverview();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) { webView.goBack(); return; }
        super.onBackPressed();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(Color.rgb(3, 4, 5));
        root = vertical();
        root.setPadding(dp(10), dp(12), dp(10), dp(18));
        scroll.addView(root);

        LinearLayout head = panel();
        head.addView(label("NEXUS MOBILE", 24, true, Color.WHITE));
        head.addView(label("Chef, Kommunikation, Dateien und Widget in einer nativen App.", 12, false, sub()));
        root.addView(head, card(0));

        LinearLayout menu = panel();
        menu.addView(section("MENUE"));
        row(menu, nav("Chef", v -> focusChef()), nav("Nachrichten", v -> loadMessages()));
        row(menu, nav("Dateien", v -> showFiles()), nav("Web", v -> showWeb("/")));
        row(menu, nav("Zeitstrahl", v -> showTimeline()), nav("Collector", v -> showCollector()));
        root.addView(menu, card(8));

        LinearLayout chat = panel();
        chat.addView(section("CHEF-KANAL"));
        chefInput = new EditText(this);
        chefInput.setMinLines(2);
        chefInput.setMaxLines(5);
        chefInput.setTextColor(Color.WHITE);
        chefInput.setHintTextColor(Color.rgb(130, 135, 142));
        chefInput.setHint("Dem Chef Kontext, Frage oder Auftrag schreiben...");
        chefInput.setTextSize(13);
        chefInput.setPadding(dp(11), dp(8), dp(11), dp(8));
        chefInput.setBackground(box(14, Color.rgb(13, 14, 14), Color.rgb(52, 37, 27)));
        chat.addView(chefInput, card(6));
        row(chat, nav("An Chef senden", v -> sendChef()), nav("Chef laden", v -> loadChefLog()));
        chefLog = label("Chef-Kanal wird geladen...", 13, false, Color.rgb(232, 226, 218));
        chefLog.setMaxLines(12);
        chefLog.setPadding(dp(10), dp(10), dp(10), dp(10));
        chefLog.setBackground(box(14, Color.rgb(8, 9, 9), Color.rgb(58, 42, 30)));
        chat.addView(chefLog, card(8));
        root.addView(chat, card(8));

        LinearLayout messages = panel();
        messages.addView(section("NACHRICHTEN"));
        messageSummary = label("Lade Nachrichten...", 13, true, Color.rgb(240, 235, 226));
        messages.addView(messageSummary);
        messageList = vertical();
        messages.addView(messageList);
        row(messages, nav("Neu laden", v -> loadMessages()), nav("Widget neu", v -> { NexusMessagesWidgetProvider.updateAll(this); loadMessages(); }));
        root.addView(messages, card(8));

        page = panel();
        pageTitle = label("", 18, true, orange());
        pageBody = label("", 13, false, Color.rgb(226, 220, 212));
        page.addView(pageTitle);
        page.addView(pageBody);
        root.addView(page, card(8));

        LinearLayout status = panel();
        status.addView(section("STATUS"));
        statusText = label("", 12, false, Color.rgb(215, 213, 208));
        status.addView(statusText);
        root.addView(status, card(8));
        return scroll;
    }

    private void focusChef() {
        chefInput.requestFocus();
        showChefStatus();
    }

    private void loadMessages() {
        if (messageSummary != null) messageSummary.setText("Lade Nachrichten...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject json = new JSONObject(httpGet(base + "/api/widget/messages?limit=20"));
                    if (!json.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> renderMessages(json, base));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 70); }
            }
            final String err = last;
            runOnUiThread(() -> { messageSummary.setText("Nachrichten nicht erreichbar. " + err); messageList.removeAllViews(); });
        }).start();
    }

    private void renderMessages(JSONObject root, String base) {
        JSONObject c = root.optJSONObject("counters");
        int focus = c == null ? 0 : c.optInt("focus", 0);
        int alarm = c == null ? 0 : c.optInt("alerts", 0);
        int reply = c == null ? 0 : c.optInt("needs_reply", 0);
        messageSummary.setText("Quelle: " + host(base) + "\nFokus: " + focus + " | Alarm: " + alarm + " | Antwort: " + reply + "\nDirekt entscheiden oder dem Chef Kontext geben.");
        messageList.removeAllViews();
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) { messageList.addView(label("Keine offenen Nachrichten.", 13, false, sub())); return; }
        int max = Math.min(12, items.length());
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String eventId = item.optString("event_id", "");
            String sender = item.optString("sender", "Unbekannt");
            String preview = item.optString("body_preview", item.optString("body", ""));
            LinearLayout card = miniCard();
            card.addView(label((i + 1) + ". [" + item.optString("priority_band", "P?") + "] " + sender, 15, true, Color.WHITE));
            card.addView(label(item.optString("suggested_action", "pruefen"), 11, true, orange()));
            card.addView(label(cut(preview, 240), 13, false, Color.rgb(232, 226, 216)));
            row(card, nav("Wichtig", v -> decide(eventId, "very_important")), nav("OK", v -> decide(eventId, "done")));
            row(card, nav("Fokus", v -> decide(eventId, "timeline_focus")), nav("Chef", v -> putContext(sender, preview)));
            messageList.addView(card, card(8));
        }
    }

    private void putContext(String sender, String preview) {
        chefInput.setText("Kontext zu Nachricht von " + sender + ":\n" + cut(preview, 180) + "\n\nMeine Einordnung: ");
        chefInput.requestFocus();
        chefLog.setText("Kontext eintragen und an Chef senden.");
    }

    private void decide(String eventId, String action) {
        messageSummary.setText("Sende Aktion: " + action + "...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    String body = "event_id=" + enc(eventId) + "&action=" + enc(action) + "&scope=conversation";
                    JSONObject res = new JSONObject(httpPost(base + "/api/widget/message-action", body));
                    if (res.optBoolean("ok", false)) {
                        NexusConfig.rememberWorkingBaseUrl(this, base);
                        runOnUiThread(this::loadMessages);
                        return;
                    }
                    last = host(base) + ": " + res.optString("message", "ok=false");
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> messageSummary.setText("Aktion fehlgeschlagen: " + err));
        }).start();
    }

    private void loadChefLog() {
        if (chefLog != null) chefLog.setText("Lade Chef-Kanal...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject root = new JSONObject(httpGet(base + "/api/communication/chef-log"));
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    String text = renderChefLog(root);
                    runOnUiThread(() -> chefLog.setText(text));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> chefLog.setText("Chef-Kanal nicht erreichbar: " + err));
        }).start();
    }

    private String renderChefLog(JSONObject root) {
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) return "Noch kein Chef-Kanal-Verlauf.";
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, items.length() - 8);
        for (int i = start; i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            sb.append(it.optString("role", "chef").toUpperCase()).append(": ").append(cut(it.optString("text", it.optString("content", "")), 420)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private void sendChef() {
        String prompt = chefInput.getText().toString().trim();
        if (prompt.isEmpty()) { chefLog.setText("Schreib zuerst eine Nachricht an den Chef."); return; }
        chefLog.setText("Sende an Chef...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject res = new JSONObject(httpPost(base + "/api/mobile/chef-chat", "prompt=" + enc(prompt)));
                    if (res.optBoolean("ok", false)) {
                        NexusConfig.rememberWorkingBaseUrl(this, base);
                        runOnUiThread(() -> { chefInput.setText(""); chefLog.setText(res.optString("message", "Chef-Auftrag gesendet.")); loadChefLog(); });
                        return;
                    }
                    last = host(base) + ": " + res.optString("message", "ok=false");
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> chefLog.setText("Chef-Chat fehlgeschlagen: " + err));
        }).start();
    }

    private void showOverview() {
        pageTitle.setText("Uebersicht");
        pageBody.setText("Chef und Nachrichten sind oben. Nutze Menue fuer Dateien, Zeitstrahl, Collector oder Web.");
    }

    private void showChefStatus() { loadApiText("Index-Chef", "/api/chef/status"); }
    private void showTimeline() { loadApiText("Zeitstrahl", "/api/timeline?limit=80"); }
    private void showFiles() { loadApiText("Dateien", "/api/v1/files/folders"); }

    private void loadApiText(String title, String path) {
        pageTitle.setText(title);
        pageBody.setText("Lade " + title + "...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    String body = httpGet(base + path);
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> pageBody.setText("Quelle: " + host(base) + "\n" + cut(body, 2200)));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> pageBody.setText(title + " nicht erreichbar: " + err));
        }).start();
    }

    private void showCollector() {
        pageTitle.setText("Collector");
        pageBody.setText("SMS, Gmail und Messenger-Benachrichtigungen werden als Events an Nexus gesendet.");
        clearPageExtras();
        Switch sw = new Switch(this);
        sw.setText("Collector aktiv");
        sw.setTextColor(Color.WHITE);
        sw.setChecked(NexusConfig.enabled(this));
        sw.setOnCheckedChangeListener((button, checked) -> { NexusConfig.setEnabled(this, checked); refreshStatus(); });
        page.addView(sw, card(8));
        endpointInput = new EditText(this);
        endpointInput.setSingleLine(true);
        endpointInput.setText(NexusConfig.endpoint(this));
        endpointInput.setTextColor(Color.WHITE);
        endpointInput.setTextSize(12);
        endpointInput.setBackground(box(14, Color.rgb(13, 14, 14), Color.rgb(52, 37, 27)));
        endpointInput.setPadding(dp(10), dp(8), dp(10), dp(8));
        page.addView(endpointInput, card(8));
        row(page, nav("Speichern", v -> { NexusConfig.setEndpoint(this, endpointInput.getText().toString()); refreshStatus(); }), nav("Outbox senden", v -> { NexusEventSender.retryOutbox(this); refreshStatus(); }));
        row(page, nav("Nachrichtenrecht", v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))), nav("SMS-Recht", v -> requestSms()));
        row(page, nav("Testevent", v -> sendTestEvent()), nav("Status", v -> refreshStatus()));
    }

    private void showWeb(String path) {
        pageTitle.setText("Nexus Web");
        pageBody.setText("Bestehendes Web-Cockpit innerhalb der App.");
        clearPageExtras();
        row(page, nav("Cockpit", v -> loadWeb("/")), nav("Kommunikation", v -> loadWeb("/communication")));
        row(page, nav("Dateien", v -> loadWeb("/files")), nav("Chef", v -> loadWeb("/chef")));
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        page.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(680)));
        loadWeb(path);
    }

    private void loadWeb(String path) {
        if (webView == null) return;
        if (path == null || path.isEmpty()) path = "/";
        webView.loadUrl(NexusConfig.baseUrl(this) + path);
    }

    private void clearPageExtras() { while (page.getChildCount() > 2) page.removeViewAt(2); webView = null; }

    private void refreshStatus() { if (statusText != null) statusText.setText(status()); }

    private String status() {
        return "Aktiv: " + (NexusConfig.enabled(this) ? "ja" : "nein") + "\n"
                + "Notification-Zugriff: " + (notificationAccess() ? "ja" : "nein") + "\n"
                + "SMS-Recht: " + (smsPermission() ? "ja" : "nein") + "\n"
                + "Gesendet: " + NexusConfig.count(this, "sent_count") + "\n"
                + "Outbox: " + NexusEventSender.outboxEvents(this) + " Event(s), " + NexusEventSender.outboxBytes(this) + " Bytes\n"
                + "Endpoint: " + NexusConfig.endpoint(this) + "\n"
                + "Sendestatus: " + NexusConfig.lastSendStatus(this) + "\n"
                + "Widget: " + NexusConfig.lastWidgetStatus(this);
    }

    private boolean smsPermission() { return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED; }
    private void requestSms() { if (!smsPermission()) requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 1001); }
    private boolean notificationAccess() { String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners"); return enabled != null && enabled.toLowerCase().contains(getPackageName().toLowerCase()); }

    private void sendTestEvent() {
        try {
            JSONObject event = NexusJson.baseEvent(NexusJson.iso(System.currentTimeMillis()), "NexusCollectorTest", "system", "Nexus Collector", "Nexus Collector Test", "Testevent vom Android Collector");
            JSONObject visual = new JSONObject();
            visual.put("app_name", "Nexus Collector");
            visual.put("package", getPackageName());
            visual.put("has_attachment_hint", false);
            event.put("visual", visual);
            NexusEventSender.sendAsync(this, event.toString());
            refreshStatus();
        } catch (Exception ignored) {}
    }

    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout panel() { LinearLayout l = vertical(); l.setPadding(dp(10), dp(10), dp(10), dp(10)); l.setBackground(box(18, Color.rgb(18, 19, 17), Color.rgb(72, 47, 31))); l.setElevation(dp(7)); return l; }
    private LinearLayout miniCard() { LinearLayout l = vertical(); l.setPadding(dp(9), dp(8), dp(9), dp(8)); l.setBackground(box(15, Color.rgb(11, 12, 11), Color.rgb(72, 47, 31))); l.setElevation(dp(3)); return l; }
    private TextView section(String s) { return label(s, 12, true, orange()); }
    private TextView label(String s, int sp, boolean bold, int color) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START); v.setPadding(0, dp(4), 0, dp(5)); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private Button nav(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(10); b.setTypeface(Typeface.DEFAULT_BOLD); b.setTextColor(Color.rgb(18, 13, 8)); b.setMinHeight(dp(28)); b.setMinimumHeight(0); b.setPadding(dp(4), 0, dp(4), 0); b.setBackground(box(12, Color.rgb(255, 158, 38), Color.rgb(255, 180, 58))); b.setOnClickListener(l); return b; }
    private void row(LinearLayout parent, Button a, Button b) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lp.setMargins(0, dp(4), dp(4), 0); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); rp.setMargins(dp(4), dp(4), 0, 0); r.addView(a, lp); r.addView(b, rp); parent.addView(r); }
    private LinearLayout.LayoutParams card(int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, dp(top), 0, 0); return lp; }
    private GradientDrawable box(int radius, int fill, int stroke) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{fill, Color.rgb(4, 5, 5)}); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), stroke); return d; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int orange() { return Color.rgb(255, 169, 54); }
    private int sub() { return Color.rgb(185, 178, 168); }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(4500); c.setReadTimeout(7000); c.setRequestMethod("GET"); c.setRequestProperty("X-Nexus-Collector", "nexus-collector-app-v3");
            int code = c.getResponseCode();
            String body = readAll(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " " + cut(body, 120));
            return body;
        } finally { if (c != null) c.disconnect(); }
    }

    private static String httpPost(String url, String body) throws Exception {
        HttpURLConnection c = null;
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(4500); c.setReadTimeout(7000); c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"); c.setRequestProperty("X-Nexus-Collector", "nexus-collector-app-v3"); c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int code = c.getResponseCode();
            String res = readAll(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " " + cut(res, 120));
            return res;
        } finally { if (c != null) c.disconnect(); }
    }

    private static String readAll(InputStream s) throws Exception { if (s == null) return ""; StringBuilder sb = new StringBuilder(); try (BufferedReader r = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8))) { String line; while ((line = r.readLine()) != null) sb.append(line); } return sb.toString(); }
    private static String enc(String v) throws Exception { return URLEncoder.encode(v == null ? "" : v, "UTF-8"); }
    private static String host(String base) { try { URL u = new URL(base); return u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : ""); } catch (Exception e) { return base == null ? "" : base; } }
    private static String cut(String v, int max) { if (v == null) return ""; String c = v.replace('\n', ' ').replace('\r', ' ').trim(); return c.length() <= max ? c : c.substring(0, Math.max(0, max - 3)) + "..."; }
}
