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

    private LinearLayout content;
    private TextView topTitle;
    private TextView topSub;
    private EditText chefInput;
    private TextView chefLog;
    private EditText endpointInput;
    private EditText messageSearch;
    private WebView webView;
    private String currentPage = PAGE_HOME;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
        showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        if (PAGE_HOME.equals(currentPage)) showHome();
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

        LinearLayout root = vertical();
        root.setPadding(dp(10), dp(12), dp(10), dp(18));
        scroll.addView(root);

        LinearLayout header = panel();
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout titleCol = vertical();
        topTitle = label("NEXUS", 23, true, Color.WHITE);
        topSub = label("Mobile Chef-Zentrale", 12, false, sub());
        titleCol.addView(topTitle);
        titleCol.addView(topSub);
        titleRow.addView(titleCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button menu = nav("Menue", v -> showHome());
        titleRow.addView(menu, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        header.addView(titleRow);
        root.addView(header, card(0));

        content = vertical();
        root.addView(content, card(8));
        return scroll;
    }

    private void clearPage(String page, String title, String subtitle) {
        currentPage = page;
        webView = null;
        endpointInput = null;
        chefInput = null;
        chefLog = null;
        messageSearch = null;
        content.removeAllViews();
        topTitle.setText(PAGE_HOME.equals(page) ? "NEXUS" : title.toUpperCase());
        topSub.setText(PAGE_HOME.equals(page) ? "Mobile Chef-Zentrale" : "Eigene Seite | Zurueck ueber Menue");

        if (!PAGE_HOME.equals(page)) {
            LinearLayout back = panel();
            row(back, nav("Zurueck", v -> showHome()), nav("Menue", v -> showHome()));
            content.addView(back, card(0));
        }

        LinearLayout pagePanel = panel();
        pagePanel.addView(label(title, 21, true, orange()));
        if (subtitle != null && !subtitle.isEmpty()) pagePanel.addView(label(subtitle, 13, false, Color.rgb(226, 220, 212)));
        content.addView(pagePanel, card(PAGE_HOME.equals(page) ? 0 : 8));
    }

    private LinearLayout activePanel() {
        return (LinearLayout) content.getChildAt(content.getChildCount() - 1);
    }

    private void showHome() {
        clearPage(PAGE_HOME, "Uebersicht", "Nur Menue, Zugriffe, Kurzlage und Status. Jede Funktion oeffnet eine eigene Seite.");
        LinearLayout p = activePanel();
        p.addView(section("ZUGAENGE"));
        p.addView(label(accessText(), 13, true, Color.rgb(238, 232, 224)));
        row(p, nav("Verbindung testen", v -> testConnection()), nav("Collector", v -> showCollectorPage()));
        row(p, nav("Nachrichtenrecht", v -> openNotificationAccess()), nav("SMS-Recht", v -> requestSms()));

        p.addView(section("MENUE"));
        row(p, nav("Chef", v -> showChefPage()), nav("Nachrichten", v -> showMessagesPage()));
        row(p, nav("Dateien", v -> showFilesPage()), nav("Zeitstrahl", v -> showTimelinePage()));
        row(p, nav("Collector", v -> showCollectorPage()), nav("Web", v -> showWebPage("/")));
        row(p, nav("Widget neu", v -> { NexusMessagesWidgetProvider.updateAll(this); showHome(); }), nav("Status", v -> showStatusOnly()));

        TextView snapshot = logBox("Lade Kurzlage...");
        p.addView(snapshot, card(8));
        loadHomeSnapshot(snapshot);
        p.addView(section("STATUS"));
        p.addView(label(status(), 12, false, Color.rgb(215, 213, 208)));
    }

    private void showStatusOnly() {
        clearPage(PAGE_HOME, "Status", "Aktueller App- und Collector-Zustand.");
        activePanel().addView(label(status(), 13, false, Color.rgb(230, 225, 216)));
    }

    private void loadHomeSnapshot(TextView target) {
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject json = new JSONObject(httpGet(base + "/api/widget/messages?limit=5"));
                    if (!json.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    String text = homeText(json, base);
                    runOnUiThread(() -> { if (PAGE_HOME.equals(currentPage)) target.setText(text); });
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> { if (PAGE_HOME.equals(currentPage)) target.setText("Nexus nicht erreichbar: " + err); });
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
        int shown = 0;
        for (int i = 0; i < items.length() && shown < 5; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null || isHidden(item.optString("event_id", ""))) continue;
            sb.append(shown + 1).append(". ").append(item.optString("sender", "Unbekannt")).append(" - ")
                    .append(cut(item.optString("body_preview", item.optString("body", "")), 120)).append('\n');
            shown++;
        }
        return sb.toString().trim();
    }

    private void showChefPage() {
        clearPage(PAGE_CHEF, "Chef", "Direkter Kanal. Keine Kommunikationsliste auf dieser Seite.");
        LinearLayout p = activePanel();
        chefInput = input("Dem Chef Kontext, Frage oder Auftrag schreiben...", false);
        chefInput.setMinLines(3);
        chefInput.setMaxLines(7);
        p.addView(chefInput, card(8));
        row(p, nav("An Chef senden", v -> sendChef()), nav("Chef laden", v -> loadChefLog()));
        chefLog = logBox("Chef-Kanal wird geladen...");
        p.addView(chefLog, card(8));
        loadChefLog();
    }

    private void showMessagesPage() {
        clearPage(PAGE_MESSAGES, "Nachrichten", "Eigene Seite. Suche findet auch Eintraege, die nicht in den ersten Fokus-Karten stehen.");
        LinearLayout p = activePanel();
        messageSearch = input("Suchen, z.B. Inkasso, PayPal, Person, Betrag...", true);
        p.addView(messageSearch, card(8));
        TextView summary = label("Lade Nachrichten...", 13, true, Color.rgb(240, 235, 226));
        LinearLayout list = vertical();
        row(p, nav("Suche", v -> loadMessages(summary, list, searchText())), nav("Inkasso", v -> { messageSearch.setText("inkasso"); loadMessages(summary, list, "inkasso"); }));
        row(p, nav("Alle neu", v -> loadMessages(summary, list, "")), nav("Widget neu", v -> { NexusMessagesWidgetProvider.updateAll(this); loadMessages(summary, list, searchText()); }));
        p.addView(summary, card(8));
        p.addView(list);
        loadMessages(summary, list, "");
    }

    private String searchText() {
        return messageSearch == null ? "" : messageSearch.getText().toString().trim().toLowerCase();
    }

    private void loadMessages(TextView summary, LinearLayout list, String filter) {
        summary.setText("Lade Nachrichten...");
        list.removeAllViews();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject json = new JSONObject(httpGet(base + "/api/widget/messages?limit=120"));
                    if (!json.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> renderMessages(summary, list, json, base, filter));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 70); }
            }
            final String err = last;
            runOnUiThread(() -> { summary.setText("Nachrichten nicht erreichbar: " + err); list.removeAllViews(); });
        }).start();
    }

    private void renderMessages(TextView summary, LinearLayout list, JSONObject root, String base, String filter) {
        if (!PAGE_MESSAGES.equals(currentPage)) return;
        String needle = filter == null ? "" : filter.trim().toLowerCase();
        JSONObject c = root.optJSONObject("counters");
        int focus = c == null ? 0 : c.optInt("focus", 0);
        int alarm = c == null ? 0 : c.optInt("alerts", 0);
        int reply = c == null ? 0 : c.optInt("needs_reply", 0);
        JSONArray items = root.optJSONArray("items");
        list.removeAllViews();
        if (items == null || items.length() == 0) {
            summary.setText("Quelle: " + host(base) + "\nKeine Nachrichten vom Server erhalten.");
            return;
        }
        int shown = 0;
        for (int i = 0; i < items.length() && shown < 40; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String eventId = item.optString("event_id", "");
            if (isHidden(eventId)) continue;
            String sender = item.optString("sender", "Unbekannt");
            String preview = item.optString("body_preview", item.optString("body", ""));
            String action = item.optString("suggested_action", "pruefen");
            String hay = (sender + " " + preview + " " + action + " " + item.optString("source", "")).toLowerCase();
            if (!needle.isEmpty() && !hay.contains(needle)) continue;

            LinearLayout card = miniCard();
            card.addView(label((shown + 1) + ". [" + item.optString("priority_band", "P?") + "] " + sender, 15, true, Color.WHITE));
            card.addView(label(action, 11, true, orange()));
            card.addView(label(cut(preview, 270), 13, false, Color.rgb(232, 226, 216)));
            row(card, nav("Sehr wichtig", v -> decide(eventId, "very_important")), nav("Erledigt", v -> decide(eventId, "done")));
            row(card, nav("Zeitstrahl", v -> decide(eventId, "timeline_focus")), nav("Chef-Kontext", v -> putContext(sender, preview)));
            list.addView(card, card(8));
            shown++;
        }
        String filterText = needle.isEmpty() ? "" : " | Suche: " + needle;
        summary.setText("Quelle: " + host(base) + "\nGeladen: " + items.length() + " | Sichtbar: " + shown + filterText + "\nFokus: " + focus + " | Alarm: " + alarm + " | Antwort: " + reply);
        if (shown == 0) list.addView(logBox("Keine Treffer. Wenn du etwas erwartest: Suchwort vereinfachen oder im Web pruefen, ob der Server es liefert."));
    }

    private void putContext(String sender, String preview) {
        showChefPage();
        chefInput.setText("Kontext zu Nachricht von " + sender + ":\n" + cut(preview, 180) + "\n\nMeine Einordnung: ");
        chefInput.requestFocus();
        chefLog.setText("Kontext eintragen und an Chef senden.");
    }

    private void decide(String eventId, String action) {
        if (eventId == null || eventId.isEmpty()) return;
        hideLocal(eventId);
        if (PAGE_MESSAGES.equals(currentPage)) showMessagesPage();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    String body = "event_id=" + enc(eventId) + "&action=" + enc(action) + "&scope=conversation";
                    JSONObject res = new JSONObject(httpPost(base + "/api/widget/message-action", body));
                    if (res.optBoolean("ok", false)) {
                        NexusConfig.rememberWorkingBaseUrl(this, base);
                        NexusConfig.setLastWidgetStatus(this, action + " OK " + eventId);
                        return;
                    }
                    last = host(base) + ": " + res.optString("message", "ok=false");
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            NexusConfig.setLastWidgetStatus(this, "Lokal ausgeblendet, Serveraktion offen: " + action + " " + last);
        }).start();
    }

    private boolean isHidden(String eventId) {
        return eventId != null && !eventId.isEmpty() && NexusConfig.prefs(this).getBoolean("hidden_message_" + eventId, false);
    }

    private void hideLocal(String eventId) {
        if (eventId != null && !eventId.isEmpty()) NexusConfig.prefs(this).edit().putBoolean("hidden_message_" + eventId, true).apply();
    }

    private void showFilesPage() {
        clearPage(PAGE_FILES, "Dateien", "Ordner aus Nexus. Kein Roh-JSON.");
        LinearLayout p = activePanel();
        row(p, nav("Neu laden", v -> showFilesPage()), nav("Web-Dateien", v -> showWebPage("/files")));
        TextView info = label("Lade Ordner...", 13, true, Color.rgb(240, 235, 226));
        p.addView(info, card(8));
        LinearLayout list = vertical();
        p.addView(list);
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
        clearPage(PAGE_TIMELINE, "Zeitstrahl", "Chronik und Entscheidungen. Erledigt bleibt sichtbar, aber markiert.");
        LinearLayout p = activePanel();
        row(p, nav("Neu laden", v -> showTimelinePage()), nav("Web-Zeitstrahl", v -> showWebPage("/timeline")));
        loadTextEndpoint("Zeitstrahl", "/api/timeline?limit=80");
    }

    private void showCollectorPage() {
        clearPage(PAGE_COLLECTOR, "Collector", "Server, Rechte, Testevent und Outbox.");
        LinearLayout p = activePanel();
        Switch sw = new Switch(this);
        sw.setText("Collector aktiv");
        sw.setTextColor(Color.WHITE);
        sw.setChecked(NexusConfig.enabled(this));
        sw.setOnCheckedChangeListener((button, checked) -> NexusConfig.setEnabled(this, checked));
        p.addView(sw, card(8));
        endpointInput = input("http://192.168.1.216:8081", true);
        endpointInput.setText(NexusConfig.baseUrl(this));
        p.addView(endpointInput, card(8));
        row(p, nav("Server speichern", v -> NexusConfig.setEndpoint(this, endpointInput.getText().toString())), nav("Verbindung testen", v -> testConnection()));
        row(p, nav("LAN 192.168", v -> { endpointInput.setText("http://192.168.1.216:8081"); NexusConfig.setEndpoint(this, endpointInput.getText().toString()); }), nav("Tailscale 100", v -> { endpointInput.setText("http://100.107.24.67:8081"); NexusConfig.setEndpoint(this, endpointInput.getText().toString()); }));
        row(p, nav("Nachrichtenrecht", v -> openNotificationAccess()), nav("SMS-Recht", v -> requestSms()));
        row(p, nav("Testevent", v -> sendTestEvent()), nav("Outbox senden", v -> NexusEventSender.retryOutbox(this)));
    }

    private void showWebPage(String path) {
        clearPage(PAGE_WEB, "Nexus Web", "Bestehendes Web-Cockpit innerhalb der App.");
        LinearLayout p = activePanel();
        row(p, nav("Cockpit", v -> loadWeb("/")), nav("Kommunikation", v -> loadWeb("/communication")));
        row(p, nav("Dateien", v -> loadWeb("/files")), nav("Chef", v -> loadWeb("/chef")));
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient());
        p.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(680)));
        loadWeb(path);
    }

    private void loadWeb(String path) {
        if (webView == null) return;
        webView.loadUrl(NexusConfig.baseUrl(this) + (path == null || path.isEmpty() ? "/" : path));
    }

    private void loadTextEndpoint(String title, String path) {
        TextView output = logBox("Lade " + title + "...");
        activePanel().addView(output, card(8));
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    String body = httpGet(base + path);
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    runOnUiThread(() -> output.setText("Quelle: " + host(base) + "\n" + cut(body, 2600)));
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
        for (int i = Math.max(0, items.length() - 14); i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            String text = cut(it.optString("text", it.optString("content", "")), 420);
            if (text.toLowerCase().contains("max_output_tokens")) {
                if (!tokenErrorShown) {
                    sb.append("SYSTEM: Chef-Antwort vom Server wegen max_output_tokens abgeschnitten. Serverlimit muss im Nexus-Core korrigiert werden.\n\n");
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

    private String accessText() {
        return "Server: " + NexusConfig.baseUrl(this) + "\nNachrichtenrecht: " + (notificationAccess() ? "aktiv" : "fehlt") + " | SMS: " + (smsPermission() ? "aktiv" : "fehlt") + "\nBei Fehler: Collector oeffnen und Verbindung testen.";
    }

    private void testConnection() {
        TextView out = logBox("Teste Nexus-Verbindung...");
        activePanel().addView(out, card(6));
        new Thread(() -> {
            StringBuilder failures = new StringBuilder();
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    httpGet(base + "/api/widget/messages?limit=1");
                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    String ok = "OK: " + host(base) + "\nNachrichtenrecht: " + (notificationAccess() ? "aktiv" : "fehlt") + " | SMS: " + (smsPermission() ? "aktiv" : "fehlt");
                    runOnUiThread(() -> out.setText(ok));
                    return;
                } catch (Exception ex) {
                    failures.append(host(base)).append(": ").append(ex.getClass().getSimpleName()).append(" ").append(cut(ex.getMessage(), 80)).append('\n');
                }
            }
            String msg = "Keine Nexus-Verbindung.\n" + failures.toString().trim() + "\nPruefe WLAN/Tailscale und Windows Port 8081.";
            runOnUiThread(() -> out.setText(msg));
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
    private void requestSms() { if (!smsPermission()) requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 1001); }
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

    private TextView logBox(String text) {
        TextView v = label(text, 13, false, Color.rgb(232, 226, 218));
        v.setPadding(dp(10), dp(10), dp(10), dp(10));
        v.setBackground(box(14, Color.rgb(8, 9, 9), Color.rgb(58, 42, 30)));
        return v;
    }

    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout panel() { LinearLayout l = vertical(); l.setPadding(dp(10), dp(10), dp(10), dp(10)); l.setBackground(box(18, Color.rgb(18, 19, 17), Color.rgb(72, 47, 31))); l.setElevation(dp(7)); return l; }
    private LinearLayout miniCard() { LinearLayout l = vertical(); l.setPadding(dp(9), dp(8), dp(9), dp(8)); l.setBackground(box(15, Color.rgb(11, 12, 11), Color.rgb(72, 47, 31))); l.setElevation(dp(3)); return l; }
    private TextView section(String s) { return label(s, 12, true, orange()); }
    private TextView label(String s, int sp, boolean bold, int color) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START); v.setPadding(0, dp(4), 0, dp(5)); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private Button nav(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(10); b.setTypeface(Typeface.DEFAULT_BOLD); b.setTextColor(Color.rgb(18, 13, 8)); b.setMinHeight(dp(30)); b.setMinimumHeight(0); b.setPadding(dp(4), 0, dp(4), 0); b.setBackground(box(12, Color.rgb(255, 158, 38), Color.rgb(255, 180, 58))); b.setOnClickListener(l); return b; }
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
