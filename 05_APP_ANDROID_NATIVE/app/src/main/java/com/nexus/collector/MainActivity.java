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
    private static final String PAGE_HOME = "home";
    private static final String PAGE_CHEF = "chef";
    private static final String PAGE_MESSAGES = "messages";
    private static final String PAGE_FILES = "files";
    private static final String PAGE_TIMELINE = "timeline";
    private static final String PAGE_COLLECTOR = "collector";
    private static final String PAGE_WEB = "web";

    private LinearLayout root;
    private LinearLayout content;
    private TextView accessText;
    private TextView statusText;
    private TextView contentTitle;
    private TextView contentBody;
    private EditText chefInput;
    private TextView chefLog;
    private EditText endpointInput;
    private WebView webView;
    private String currentPage = PAGE_HOME;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
        showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
        if (PAGE_HOME.equals(currentPage)) loadHomeSnapshot();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) { webView.goBack(); return; }
        if (!PAGE_HOME.equals(currentPage)) { showHome(); return; }
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
        head.addView(label("NEXUS CHEF NATIVE", 24, true, Color.WHITE));
        head.addView(label("Chef, Kommunikation, Dateien und Widget in einer nativen App.", 12, false, sub()));
        root.addView(head, card(0));

        LinearLayout access = panel();
        access.addView(section("ZUGAENGE"));
        accessText = label("Pruefe Zugriffe...", 13, true, Color.rgb(240, 235, 226));
        access.addView(accessText);
        row(access, nav("Verbindung testen", v -> testConnection()), nav("Server / Rechte", v -> showCollectorPage()));
        row(access, nav("Nachrichtenrecht", v -> openNotificationAccess()), nav("SMS-Recht", v -> requestSms()));
        root.addView(access, card(8));

        LinearLayout menu = panel();
        menu.addView(section("MENUE"));
        row(menu, nav("Home", v -> showHome()), nav("Chef", v -> showChefPage()));
        row(menu, nav("Nachrichten", v -> showMessagesPage()), nav("Dateien", v -> showFilesPage()));
        row(menu, nav("Zeitstrahl", v -> showTimelinePage()), nav("Collector", v -> showCollectorPage()));
        row(menu, nav("Web", v -> showWebPage("/")), nav("Widget neu", v -> { NexusMessagesWidgetProvider.updateAll(this); refreshStatus(); }));
        root.addView(menu, card(8));

        content = panel();
        root.addView(content, card(8));

        LinearLayout status = panel();
        status.addView(section("STATUS"));
        statusText = label("", 12, false, Color.rgb(215, 213, 208));
        status.addView(statusText);
        root.addView(status, card(8));
        return scroll;
    }

    private void clearContent(String page, String title, String body) {
        currentPage = page;
        webView = null;
        endpointInput = null;
        chefInput = null;
        chefLog = null;
        content.removeAllViews();
        contentTitle = label(title, 20, true, orange());
        contentBody = label(body == null ? "" : body, 13, false, Color.rgb(226, 220, 212));
        content.addView(contentTitle);
        if (body != null && !body.isEmpty()) content.addView(contentBody);
    }

    private void showHome() {
        clearContent(PAGE_HOME, "Kurzlage", "Nur die wichtigsten Dinge. Vollansichten liegen auf eigenen Seiten.");
        row(content, nav("Chef oeffnen", v -> showChefPage()), nav("Nachrichten", v -> showMessagesPage()));
        row(content, nav("Dateien", v -> showFilesPage()), nav("Zeitstrahl", v -> showTimelinePage()));
        row(content, nav("Collector", v -> showCollectorPage()), nav("Web", v -> showWebPage("/")));
        loadHomeSnapshot();
    }

    private void loadHomeSnapshot() {
        refreshStatus();
        TextView snapshot = label("Lade Kurzlage...", 13, true, Color.rgb(240, 235, 226));
        if (PAGE_HOME.equals(currentPage)) content.addView(snapshot, card(8));
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject json = new JSONObject(httpGet(base + "/api/widget/messages?limit=3"));
                    if (!json.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    String text = homeText(json, base);
                    runOnUiThread(() -> { if (PAGE_HOME.equals(currentPage)) snapshot.setText(text); });
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> { if (PAGE_HOME.equals(currentPage)) snapshot.setText("Nexus nicht erreichbar: " + err); });
        }).start();
    }

    private String homeText(JSONObject root, String base) {
        JSONObject c = root.optJSONObject("counters");
        int focus = c == null ? 0 : c.optInt("focus", 0);
        int alerts = c == null ? 0 : c.optInt("alerts", 0);
        int reply = c == null ? 0 : c.optInt("needs_reply", 0);
        StringBuilder sb = new StringBuilder();
        sb.append("Quelle: ").append(host(base)).append('\n');
        sb.append("Fokus: ").append(focus).append(" | Alarm: ").append(alerts).append(" | Antwort: ").append(reply).append("\n\n");
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) return sb.append("Keine offenen Fokusnachrichten.").toString();
        int max = Math.min(3, items.length());
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            sb.append(i + 1).append(". ").append(item.optString("sender", "Unbekannt")).append(" - ")
                    .append(cut(item.optString("body_preview", item.optString("body", "")), 120)).append('\n');
        }
        return sb.toString().trim();
    }

    private void showChefPage() {
        clearContent(PAGE_CHEF, "Chef", "Kontext, Frage oder Auftrag direkt an den Index-Chef senden.");
        chefInput = input("Dem Chef Kontext, Frage oder Auftrag schreiben...", false);
        chefInput.setMinLines(3);
        chefInput.setMaxLines(7);
        content.addView(chefInput, card(8));
        row(content, nav("An Chef senden", v -> sendChef()), nav("Chef laden", v -> loadChefLog()));
        chefLog = label("Chef-Kanal wird geladen...", 13, false, Color.rgb(232, 226, 218));
        chefLog.setPadding(dp(10), dp(10), dp(10), dp(10));
        chefLog.setBackground(box(14, Color.rgb(8, 9, 9), Color.rgb(58, 42, 30)));
        content.addView(chefLog, card(8));
        loadChefLog();
    }

    private void showMessagesPage() {
        clearContent(PAGE_MESSAGES, "Nachrichten", "Eigene Ansicht. Gespräche sind kompakt gruppiert. Aktionen wirken auf die Unterhaltung.");
        row(content, nav("Neu laden", v -> showMessagesPage()), nav("Widget neu", v -> { NexusMessagesWidgetProvider.updateAll(this); showMessagesPage(); }));
        loadMessagesIntoContent();
    }

    private void loadMessagesIntoContent() {
        TextView summary = label("Lade Nachrichten...", 13, true, Color.rgb(240, 235, 226));
        content.addView(summary, card(8));
        LinearLayout list = vertical();
        content.addView(list);
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject json = new JSONObject(httpGet(base + "/api/widget/messages?limit=30"));
                    if (!json.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> renderMessages(summary, list, json, base));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 70); }
            }
            final String err = last;
            runOnUiThread(() -> { summary.setText("Nachrichten nicht erreichbar: " + err); list.removeAllViews(); });
        }).start();
    }

    private void renderMessages(TextView summary, LinearLayout list, JSONObject root, String base) {
        if (!PAGE_MESSAGES.equals(currentPage)) return;
        JSONObject c = root.optJSONObject("counters");
        int focus = c == null ? 0 : c.optInt("focus", 0);
        int alarm = c == null ? 0 : c.optInt("alerts", 0);
        int reply = c == null ? 0 : c.optInt("needs_reply", 0);
        summary.setText("Quelle: " + host(base) + "\nFokus: " + focus + " | Alarm: " + alarm + " | Antwort: " + reply);
        list.removeAllViews();
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) { list.addView(label("Keine offenen Nachrichten.", 13, false, sub())); return; }
        int max = Math.min(20, items.length());
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String eventId = item.optString("event_id", "");
            String sender = item.optString("sender", "Unbekannt");
            String preview = item.optString("body_preview", item.optString("body", ""));
            LinearLayout card = miniCard();
            card.addView(label((i + 1) + ". [" + item.optString("priority_band", "P?") + "] " + sender, 15, true, Color.WHITE));
            card.addView(label(item.optString("suggested_action", "pruefen"), 11, true, orange()));
            card.addView(label(cut(preview, 260), 13, false, Color.rgb(232, 226, 216)));
            row(card, nav("Wichtig", v -> decide(eventId, "very_important")), nav("Erledigt", v -> decide(eventId, "done")));
            row(card, nav("Zeitstrahl", v -> decide(eventId, "timeline_focus")), nav("Chef-Kontext", v -> putContext(sender, preview)));
            list.addView(card, card(8));
        }
    }

    private void putContext(String sender, String preview) {
        showChefPage();
        chefInput.setText("Kontext zu Nachricht von " + sender + ":\n" + cut(preview, 180) + "\n\nMeine Einordnung: ");
        chefInput.requestFocus();
        chefLog.setText("Kontext eintragen und an Chef senden.");
    }

    private void decide(String eventId, String action) {
        TextView info = label("Sende Aktion: " + action + "...", 12, true, orange());
        content.addView(info, card(6));
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    String body = "event_id=" + enc(eventId) + "&action=" + enc(action) + "&scope=conversation";
                    JSONObject res = new JSONObject(httpPost(base + "/api/widget/message-action", body));
                    if (res.optBoolean("ok", false)) {
                        NexusConfig.rememberWorkingBaseUrl(this, base);
                        runOnUiThread(this::showMessagesPage);
                        return;
                    }
                    last = host(base) + ": " + res.optString("message", "ok=false");
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> info.setText("Aktion fehlgeschlagen: " + err));
        }).start();
    }

    private void showFilesPage() {
        clearContent(PAGE_FILES, "Dateien", "Ordner aus Nexus. Kein Roh-JSON mehr.");
        row(content, nav("Neu laden", v -> showFilesPage()), nav("Web-Dateien", v -> showWebPage("/files")));
        TextView info = label("Lade Ordner...", 13, true, Color.rgb(240, 235, 226));
        content.addView(info, card(8));
        LinearLayout list = vertical();
        content.addView(list);
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject root = new JSONObject(httpGet(base + "/api/v1/files/folders"));
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> renderFolders(info, list, root, base));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> info.setText("Dateien nicht erreichbar: " + err));
        }).start();
    }

    private void renderFolders(TextView info, LinearLayout list, JSONObject root, String base) {
        if (!PAGE_FILES.equals(currentPage)) return;
        JSONArray items = null;
        JSONObject data = root.optJSONObject("data");
        if (data != null) items = data.optJSONArray("items");
        if (items == null) items = root.optJSONArray("items");
        if (items == null) { info.setText("Quelle: " + host(base) + "\nAntwort konnte nicht als Ordnerliste gelesen werden."); return; }
        info.setText("Quelle: " + host(base) + "\nOrdner: " + items.length());
        list.removeAllViews();
        int max = Math.min(35, items.length());
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            LinearLayout card = miniCard();
            card.addView(label(item.optString("label", item.optString("name", "Ordner")), 14, true, Color.WHITE));
            card.addView(label("Dateien: " + item.optInt("count", 0) + " | Ordner: " + item.optInt("folders", 0) + " | Tiefe: " + item.optInt("depth", 0), 11, false, sub()));
            card.addView(label(cut(item.optString("path", ""), 160), 11, false, Color.rgb(180, 174, 166)));
            list.addView(card, card(6));
        }
    }

    private void showTimelinePage() {
        clearContent(PAGE_TIMELINE, "Zeitstrahl", "Chronik und Entscheidungen. Erledigt bleibt sichtbar, aber markiert.");
        row(content, nav("Neu laden", v -> showTimelinePage()), nav("Web-Zeitstrahl", v -> showWebPage("/timeline")));
        loadTextEndpoint("Zeitstrahl", "/api/timeline?limit=80");
    }

    private void showCollectorPage() {
        clearContent(PAGE_COLLECTOR, "Collector", "Server, Rechte, Testevent und Outbox. Diese Seite ist isoliert.");
        Switch sw = new Switch(this);
        sw.setText("Collector aktiv");
        sw.setTextColor(Color.WHITE);
        sw.setChecked(NexusConfig.enabled(this));
        sw.setOnCheckedChangeListener((button, checked) -> { NexusConfig.setEnabled(this, checked); refreshStatus(); });
        content.addView(sw, card(8));
        endpointInput = input("http://192.168.1.216:8081", true);
        endpointInput.setText(NexusConfig.baseUrl(this));
        content.addView(endpointInput, card(8));
        row(content, nav("Server speichern", v -> { NexusConfig.setEndpoint(this, endpointInput.getText().toString()); refreshStatus(); }), nav("Verbindung testen", v -> testConnection()));
        row(content, nav("LAN 192.168", v -> { endpointInput.setText("http://192.168.1.216:8081"); NexusConfig.setEndpoint(this, endpointInput.getText().toString()); refreshStatus(); }), nav("Tailscale 100", v -> { endpointInput.setText("http://100.107.24.67:8081"); NexusConfig.setEndpoint(this, endpointInput.getText().toString()); refreshStatus(); }));
        row(content, nav("Nachrichtenrecht", v -> openNotificationAccess()), nav("SMS-Recht", v -> requestSms()));
        row(content, nav("Testevent", v -> sendTestEvent()), nav("Outbox senden", v -> { NexusEventSender.retryOutbox(this); refreshStatus(); }));
    }

    private void showWebPage(String path) {
        clearContent(PAGE_WEB, "Nexus Web", "Bestehendes Web-Cockpit innerhalb der App.");
        row(content, nav("Cockpit", v -> loadWeb("/")), nav("Kommunikation", v -> loadWeb("/communication")));
        row(content, nav("Dateien", v -> loadWeb("/files")), nav("Chef", v -> loadWeb("/chef")));
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        content.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(680)));
        loadWeb(path);
    }

    private void loadWeb(String path) {
        if (webView == null) return;
        if (path == null || path.isEmpty()) path = "/";
        webView.loadUrl(NexusConfig.baseUrl(this) + path);
    }

    private void loadTextEndpoint(String title, String path) {
        TextView output = label("Lade " + title + "...", 13, false, Color.rgb(226, 220, 212));
        output.setPadding(dp(10), dp(10), dp(10), dp(10));
        output.setBackground(box(14, Color.rgb(8, 9, 9), Color.rgb(58, 42, 30)));
        content.addView(output, card(8));
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    String body = httpGet(base + path);
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> { if (title.equals(contentTitle.getText().toString())) output.setText("Quelle: " + host(base) + "\n" + cut(body, 2600)); });
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> output.setText(title + " nicht erreichbar: " + err));
        }).start();
    }

    private void loadChefLog() {
        if (chefLog == null) return;
        chefLog.setText("Lade Chef-Kanal...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject root = new JSONObject(httpGet(base + "/api/communication/chef-log"));
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    String text = renderChefLog(root);
                    runOnUiThread(() -> { if (chefLog != null) chefLog.setText(text); });
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> { if (chefLog != null) chefLog.setText("Chef-Kanal nicht erreichbar: " + err); });
        }).start();
    }

    private String renderChefLog(JSONObject root) {
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) return "Noch kein Chef-Kanal-Verlauf.";
        StringBuilder sb = new StringBuilder();
        boolean tokenErrorShown = false;
        int shown = 0;
        for (int i = Math.max(0, items.length() - 12); i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            String text = cut(it.optString("text", it.optString("content", "")), 420);
            if (text.toLowerCase().contains("max_output_tokens")) {
                if (!tokenErrorShown) {
                    sb.append("SYSTEM: Chef-Antwort wurde vom Server wegen max_output_tokens abgeschnitten. Das ist ein Server-/Responses-Limit, kein Chatinhalt.\n\n");
                    tokenErrorShown = true;
                    shown++;
                }
                continue;
            }
            sb.append(it.optString("role", "chef").toUpperCase()).append(": ").append(text).append("\n\n");
            shown++;
        }
        return shown == 0 ? "Keine brauchbaren Chef-Nachrichten im Verlauf." : sb.toString().trim();
    }

    private void sendChef() {
        if (chefInput == null || chefLog == null) return;
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
                        runOnUiThread(() -> { if (chefInput != null) chefInput.setText(""); if (chefLog != null) chefLog.setText(res.optString("message", "Chef-Auftrag gesendet.")); loadChefLog(); });
                        return;
                    }
                    last = host(base) + ": " + res.optString("message", "ok=false");
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> { if (chefLog != null) chefLog.setText("Chef-Chat fehlgeschlagen: " + err); });
        }).start();
    }

    private void refreshStatus() {
        if (accessText != null) accessText.setText("Server: " + NexusConfig.baseUrl(this) + "\nNachrichtenrecht: " + (notificationAccess() ? "aktiv" : "fehlt") + " | SMS: " + (smsPermission() ? "aktiv" : "fehlt") + "\nBei Fehler: Verbindung testen oder Collector oeffnen.");
        if (statusText != null) statusText.setText(status());
    }

    private void testConnection() {
        if (accessText != null) accessText.setText("Teste Nexus-Verbindung...");
        new Thread(() -> {
            StringBuilder failures = new StringBuilder();
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    httpGet(base + "/api/widget/messages?limit=1");
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    String ok = "OK: " + host(base) + "\nNachrichtenrecht: " + (notificationAccess() ? "aktiv" : "fehlt") + " | SMS: " + (smsPermission() ? "aktiv" : "fehlt");
                    runOnUiThread(() -> { if (accessText != null) accessText.setText(ok); refreshStatus(); if (PAGE_HOME.equals(currentPage)) showHome(); });
                    return;
                } catch (Exception ex) {
                    failures.append(host(base)).append(": ").append(ex.getClass().getSimpleName()).append(" ").append(cut(ex.getMessage(), 80)).append('\n');
                }
            }
            String msg = "Keine Nexus-Verbindung.\n" + failures.toString().trim() + "\nPruefe: Handy im gleichen WLAN oder Tailscale aktiv, Windows-Firewall Port 8081 offen.";
            runOnUiThread(() -> { if (accessText != null) accessText.setText(msg); });
        }).start();
    }

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
    private void requestSms() { if (!smsPermission()) requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 1001); refreshStatus(); }
    private boolean notificationAccess() { String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners"); return enabled != null && enabled.toLowerCase().contains(getPackageName().toLowerCase()); }
    private void openNotificationAccess() { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }

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

    private EditText input(String hint, boolean singleLine) {
        EditText e = new EditText(this);
        e.setSingleLine(singleLine);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(130, 135, 142));
        e.setHint(hint);
        e.setTextSize(13);
        e.setPadding(dp(11), dp(8), dp(11), dp(8));
        e.setBackground(box(14, Color.rgb(13, 14, 14), Color.rgb(52, 37, 27)));
        return e;
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
