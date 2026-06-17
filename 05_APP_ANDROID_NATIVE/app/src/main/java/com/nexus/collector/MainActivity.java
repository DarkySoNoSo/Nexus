package com.nexus.collector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
import java.util.HashSet;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String PAGE_HOME = "home";
    private static final String PAGE_CHEF = "chef";
    private static final String PAGE_MESSAGES = "messages";
    private static final String PAGE_FILES = "files";
    private static final String PAGE_TIMELINE = "timeline";
    private static final String PAGE_COMPANION = "companion";
    private static final String PAGE_DIGIPAD = "digipad";
    private static final String PAGE_COLLECTOR = "collector";
    private static final String PAGE_NEXY = "nexy";
    private static final String PAGE_WEB = "web";

    private ScrollView mainScroll;
    private LinearLayout content;
    private TextView topTitle;
    private TextView topSub;
    private EditText chefInput;
    private TextView chefLog;
    private EditText endpointInput;
    private EditText messageSearch;
    private EditText nexySearchInput;
    private EditText digipadTokenInput;
    private WebView webView;
    private String currentPage = PAGE_HOME;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        installCrashGuard();
        try {
            safeFullscreen();
            setContentView(buildUi());
            if (BuildConfig.DIGIPAD_ONLY) showDigiPadPage(); else showHome();
        } catch (Throwable t) {
            showCrashScreen("onCreate", t);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        safeFullscreen();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) safeFullscreen();
    }

    @Override public void onBackPressed() {
        if (BuildConfig.DIGIPAD_ONLY) { showDigiPadPage(); return; }
        if (webView != null && webView.canGoBack()) { webView.goBack(); return; }
        if (!PAGE_HOME.equals(currentPage)) { showHome(); return; }
        super.onBackPressed();
    }

    private void installCrashGuard() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                runOnUiThread(() -> showCrashScreen("uncaught:" + thread.getName(), throwable));
            } catch (Throwable ignored) {}
        });
    }

    private void safeFullscreen() {
        try {
            enableFullscreen();
        } catch (Throwable t) {
            try { Log.w("NexusMain", "Fullscreen disabled after failure", t); } catch (Throwable ignored) {}
        }
    }

    private void enableFullscreen() {
        Window window = getWindow();
        if (window == null) return;

        View decor = window.getDecorView();
        if (decor == null) return;

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void showCrashScreen(String phase, Throwable t) {
        try {
            String report = "NEXUS SAFE CRASHSCREEN\\n"
                    + "Phase: " + phase + "\\n"
                    + "Android SDK: " + Build.VERSION.SDK_INT + "\\n"
                    + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\\n\\n"
                    + Log.getStackTraceString(t);

            try { Log.e("NexusCrash", report, t); } catch (Throwable ignored) {}

            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            scroll.setBackgroundColor(Color.BLACK);

            TextView out = new TextView(this);
            out.setText(report);
            out.setTextColor(Color.WHITE);
            out.setTextSize(12);
            out.setPadding(dp(14), dp(14), dp(14), dp(14));

            scroll.addView(out);
            setContentView(scroll);
        } catch (Throwable ignored) {
            TextView out = new TextView(this);
            out.setText("NEXUS STARTFEHLER: " + t.getClass().getName() + ": " + t.getMessage());
            out.setTextColor(Color.WHITE);
            out.setBackgroundColor(Color.BLACK);
            setContentView(out);
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        mainScroll = scroll;
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
        Button menu = BuildConfig.DIGIPAD_ONLY ? nav("DigiPad", v -> showDigiPadPage()) : nav("Menue", v -> showHome());
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
        nexySearchInput = null;
        digipadTokenInput = null;
        content.removeAllViews();
        topTitle.setText(BuildConfig.DIGIPAD_ONLY ? "DIGIPAD" : (PAGE_HOME.equals(page) ? "NEXUS" : title.toUpperCase()));
        topSub.setText(BuildConfig.DIGIPAD_ONLY ? "Remote Client" : (PAGE_HOME.equals(page) ? "Mobile Chef-Zentrale" : "Aktive Seite: " + title));

        if (!BuildConfig.DIGIPAD_ONLY && !PAGE_HOME.equals(page)) {
            LinearLayout back = panel();
            row(back, nav("Zurueck", v -> showHome()), nav("Menue", v -> showHome()));
            content.addView(back, card(0));
        }

        LinearLayout pagePanel = panel();
        pagePanel.addView(label(title, 21, true, orange()));
        if (subtitle != null && !subtitle.isEmpty()) pagePanel.addView(label(subtitle, 13, false, Color.rgb(226, 220, 212)));
        content.addView(pagePanel, card(PAGE_HOME.equals(page) ? 0 : 8));
        if (mainScroll != null) mainScroll.post(() -> mainScroll.scrollTo(0, 0));
        safeFullscreen();
    }

    private LinearLayout activePanel() {
        return (LinearLayout) content.getChildAt(content.getChildCount() - 1);
    }

    private void showHome() {
        if (BuildConfig.DIGIPAD_ONLY) { showDigiPadPage(); return; }
        clearPage(PAGE_HOME, "Uebersicht", "Nur Menue, Zugriffe, Kurzlage und Status. Jede Funktion oeffnet eine eigene Seite.");
        LinearLayout p = activePanel();
        p.addView(section("ZUGAENGE"));
        p.addView(label(accessText(), 13, true, Color.rgb(238, 232, 224)));
        row(p, nav("Verbindung testen", v -> testConnection()), nav("Collector", v -> showCollectorPage()));
        row(p, nav("Nachrichtenrecht", v -> openNotificationAccess()), nav("SMS-Recht", v -> requestSms()));

        p.addView(section("MENUE"));
        row(p, nav("Chef", v -> showChefPage()), nav("Nachrichten", v -> showMessagesPage()));
        row(p, nav("Dateien", v -> showFilesPage()), nav("Zeitstrahl", v -> showTimelinePage()));
        row(p, nav("Digi Dragon", v -> showCompanionPage()), nav("DigiPad", v -> showDigiPadPage()));
        row(p, nav("Collector", v -> showCollectorPage()), nav("Collector Status", v -> showStatusOnly()));
        row(p, nav("Web", v -> showWebPage("/")), nav("Widget neu", v -> { NexusMessagesWidgetProvider.updateAll(this); showHome(); }));
        row(p, nav("Nexy", v -> showNexyPage()), nav("Status", v -> showStatusOnly()));
        row(p, nav("Seite neu", v -> showHome()), nav("Nexy Briefing", v -> showNexyPage()));

        TextView snapshot = logBox("Start OK. Keine automatische Serverabfrage beim App-Start.\nBackend offline möglich. Nutze 'Verbindung testen' oder öffne gezielt Nexy, Chef, Nachrichten, Dateien oder Zeitstrahl.");
        p.addView(snapshot, card(8));
        p.addView(section("STATUS"));
        p.addView(label(status(), 12, false, Color.rgb(215, 213, 208)));
        p.addView(section("THEME"));
        row(p, nav("Cyberblau", v -> setTheme("blue")), nav("Neon Gruen", v -> setTheme("green")));
        row(p, nav("Orange", v -> setTheme("orange")), nav("Menue oben", v -> showHome()));
    }


    private void showNexyPage() {
        clearPage(PAGE_NEXY, "Nexy", "Gedächtnis, Fokus, Recall und Briefing über die lokale Nexy Bridge.");
        LinearLayout p = activePanel();

        p.addView(section("BRIDGE"));
        endpointInput = input("http://127.0.0.1:8765", true);
        endpointInput.setText(nexyBridgeBase());
        p.addView(endpointInput, card(8));

        TextView out = logBox("Nexy bereit.\nBridge: " + nexyBridgeBase() + "\nKeine automatische Abfrage beim App-Start. Diese Seite lädt nur auf Knopfdruck.");
        p.addView(out, card(8));

        row(p,
                nav("Lokal 127", v -> useNexyBridge(out, "http://127.0.0.1:8765", "Lokal-Termux")),
                nav("LAN 192", v -> useNexyBridge(out, "http://192.168.1.216:8765", "PC/LAN"))
        );

        row(p,
                nav("Tailscale 100", v -> useNexyBridge(out, "http://100.107.24.67:8765", "Tailscale")),
                nav("Status", v -> loadNexyEndpoint(out, "Nexy Status", "/api/nexy/status"))
        );

        row(p,
                nav("Bridge speichern", v -> { setNexyBridgeBase(endpointInput.getText().toString()); out.setText("Nexy Bridge gespeichert:\n" + nexyBridgeBase() + "\n\nDrücke Status oder Briefing."); }),
                nav("Briefing", v -> loadNexyEndpoint(out, "Nexy Briefing", "/api/nexy/briefing"))
        );

        row(p,
                nav("Fokus", v -> loadNexyEndpoint(out, "Nexy Fokus", "/api/nexy/focus?limit=5")),
                nav("Timeline", v -> loadNexyEndpoint(out, "Nexy Timeline", "/api/nexy/timeline?limit=8"))
        );

        p.addView(section("SUCHE"));
        nexySearchInput = input("Suchbegriff: Patrick, Safe-Start, Timeline...", true);
        p.addView(nexySearchInput, card(8));

        row(p,
                nav("Suchen", v -> loadNexySearch(out)),
                nav("Patrick", v -> { nexySearchInput.setText("Patrick"); loadNexySearch(out); })
        );

        row(p,
                nav("Safe-Start", v -> { nexySearchInput.setText("Safe-Start"); loadNexySearch(out); }),
                nav("Memory", v -> loadNexyEndpoint(out, "Nexy Memory", "/api/nexy/status"))
        );
    }

    private String nexyBridgeBase() {
        return NexusConfig.prefs(this).getString("nexy_bridge_url", "http://127.0.0.1:8765");
    }

    private void setNexyBridgeBase(String value) {
        String v = value == null ? "" : value.trim();
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (v.isEmpty()) v = "http://127.0.0.1:8765";
        NexusConfig.prefs(this).edit().putString("nexy_bridge_url", v).apply();
    }

    private void useNexyBridge(TextView out, String url, String label) {
        setNexyBridgeBase(url);
        if (endpointInput != null) endpointInput.setText(nexyBridgeBase());
        if (out != null) {
            out.setText("✅ Nexy Bridge gesetzt\n"
                    + "Profil: " + label + "\n"
                    + "URL: " + nexyBridgeBase() + "\n\n"
                    + "Jetzt Status, Briefing, Fokus oder Suche drücken.");
        }
    }

    private String nexyRender(String title, String body, String base) {
        try {
            JSONObject root = new JSONObject(body);
            StringBuilder sb = new StringBuilder();

            sb.append("✅ ").append(title).append("\n");
            sb.append("Quelle: ").append(host(base)).append("\n");

            if (!root.optBoolean("ok", true)) {
                sb.append("\n❌ Status: Fehler\n");
                sb.append(root.optString("error", root.optString("message", "ok=false")));
                return sb.toString().trim();
            }

            JSONObject briefing = root.optJSONObject("briefing");
            if (briefing != null) {
                JSONObject status = briefing.optJSONObject("status");
                if (status != null) appendNexyCounts(sb, status);

                appendNexyFocus(sb, briefing.optJSONArray("focus"));
                appendNexyLessons(sb, briefing.optJSONArray("top_lessons"));
                appendNexyFacts(sb, briefing.optJSONArray("facts"));
                appendNexyTimeline(sb, briefing.optJSONArray("latest_timeline"));

                return sb.toString().trim();
            }

            JSONObject counts = root.optJSONObject("counts");
            if (counts != null) appendNexyCounts(sb, counts);

            JSONArray items = root.optJSONArray("items");
            if (items == null) items = root.optJSONArray("focus");
            if (items == null) items = root.optJSONArray("latest_timeline");

            if (items != null) appendNexyItems(sb, "Einträge", items, 8);

            if (counts != null || items != null) return sb.toString().trim();

            return sb.append("\n").append(cutKeepLines(body, 3000)).toString().trim();
        } catch (Exception ex) {
            return title + "\nQuelle: " + host(base) + "\n\n" + cutKeepLines(body, 3000);
        }
    }

    private void appendNexyCounts(StringBuilder sb, JSONObject c) {
        sb.append("\n📊 Speicher\n");
        sb.append("Events: ").append(c.optInt("nexy_events", 0))
                .append(" | Timeline: ").append(c.optInt("nexy_timeline", 0)).append("\n");
        sb.append("Context: ").append(c.optInt("nexy_context", 0))
                .append(" | Facts: ").append(c.optInt("nexy_facts", 0)).append("\n");
        sb.append("Lessons: ").append(c.optInt("nexy_lessons", 0))
                .append(" | Fokus: ").append(c.optInt("nexy_active_focus", 0)).append("\n");
    }

    private void appendNexyFocus(StringBuilder sb, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\n🎯 Aktiver Fokus\n");
        int max = Math.min(arr.length(), 6);
        for (int i = 0; i < max; i++) {
            JSONObject x = arr.optJSONObject(i);
            if (x == null) continue;
            sb.append(i + 1).append(". ").append(x.optString("focus_name", x.optString("name", "Fokus"))).append("\n");
            String next = x.optString("next_action", "");
            if (!next.isEmpty()) sb.append("   → ").append(cut(next, 150)).append("\n");
            String desc = x.optString("description", "");
            if (!desc.isEmpty()) sb.append("   ").append(cut(desc, 180)).append("\n");
        }
    }

    private void appendNexyLessons(StringBuilder sb, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\n🧠 Lessons\n");
        int max = Math.min(arr.length(), 5);
        for (int i = 0; i < max; i++) {
            JSONObject x = arr.optJSONObject(i);
            if (x == null) continue;
            String lesson = x.optString("lesson", x.optString("title", ""));
            String rule = x.optString("rule", "");
            if (!lesson.isEmpty()) sb.append("- ").append(cut(lesson, 180)).append("\n");
            if (!rule.isEmpty()) sb.append("  Regel: ").append(cut(rule, 160)).append("\n");
        }
    }

    private void appendNexyFacts(StringBuilder sb, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\n📌 Facts\n");
        int max = Math.min(arr.length(), 6);
        for (int i = 0; i < max; i++) {
            JSONObject x = arr.optJSONObject(i);
            if (x == null) continue;
            String fact = x.optString("statement", x.optString("fact", ""));
            if (!fact.isEmpty()) sb.append("- ").append(cut(fact, 190)).append("\n");
        }
    }

    private void appendNexyTimeline(StringBuilder sb, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\n🕒 Timeline\n");
        int max = Math.min(arr.length(), 6);
        for (int i = 0; i < max; i++) {
            JSONObject x = arr.optJSONObject(i);
            if (x == null) continue;
            String t = x.optString("ts", x.optString("created_at", ""));
            String text = x.optString("title", x.optString("summary", x.optString("text", "")));
            sb.append("- ");
            if (!t.isEmpty()) sb.append(cut(t, 19)).append(" ");
            sb.append(cut(text, 180)).append("\n");
        }
    }

    private void appendNexyItems(StringBuilder sb, String label, JSONArray arr, int limit) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\n📋 ").append(label).append("\n");
        int max = Math.min(arr.length(), limit);
        for (int i = 0; i < max; i++) {
            JSONObject x = arr.optJSONObject(i);
            if (x == null) continue;
            String text = x.optString("title", x.optString("focus_name", x.optString("summary", x.optString("text", x.toString()))));
            sb.append(i + 1).append(". ").append(cut(text, 220)).append("\n");
        }
    }

    private void loadNexySearch(TextView out) {
        String q = nexySearchInput == null ? "" : nexySearchInput.getText().toString().trim();
        if (q.isEmpty()) {
            out.setText("Suchbegriff fehlt.");
            return;
        }
        try {
            loadNexyEndpoint(out, "Nexy Suche: " + q, "/api/nexy/search?q=" + enc(q) + "&limit=5");
        } catch (Exception ex) {
            out.setText("Nexy Suche konnte nicht vorbereitet werden: " + ex.getClass().getSimpleName());
        }
    }

    private void loadNexyEndpoint(TextView out, String title, String path) {
        String base = nexyBridgeBase();
        out.setText("Lade " + title + "...\n" + base + path);
        new Thread(() -> {
            try {
                String body = httpGet(base + path);
                String text = nexyRender(title, body, base);
                runOnUiThread(() -> { if (PAGE_NEXY.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "Nexy nicht erreichbar.\nBridge: " + base + "\nFehler: " + ex.getClass().getSimpleName() + " " + cutKeepLines(ex.getMessage(), 300)
                        + "\n\nPrüfen: PC Bridge läuft mit NEXY_HOST=0.0.0.0, Port 8765 offen, Handy im gleichen WLAN/Tailscale.";
                runOnUiThread(() -> { if (PAGE_NEXY.equals(currentPage)) out.setText(err); });
            }
        }).start();
    }



    private void showDigiPadPage() {
        clearPage(PAGE_DIGIPAD, "DigiPad", "Geschützter Remote-Client für das Fiona-Profil über die DigiPad Container API.");
        LinearLayout p = activePanel();

        p.addView(section("VERBINDUNG"));
        endpointInput = input("http://192.168.x.x:8788 oder Tailscale:8788", true);
        endpointInput.setText(digipadBase());
        p.addView(endpointInput, card(8));

        digipadTokenInput = input("DigiPad Token", true);
        digipadTokenInput.setText(digipadToken());
        p.addView(digipadTokenInput, card(8));

        TextView out = logBox("DigiPad bereit.\nProfil: fiona\nAPI: " + digipadBase()
                + "\nToken wird lokal auf diesem Gerät gespeichert.\nKeine Nexy-, Chef-, Collector-, Nachrichten- oder Datei-Endpunkte.");
        p.addView(out, card(8));

        row(p,
                nav("Speichern", v -> saveDigiPadSettings(out)),
                nav("Status", v -> loadDigiPadEndpoint(out, "Fiona Status", "/api/pad/fiona/status"))
        );

        row(p,
                nav("Füttern", v -> postDigiPadAction(out, "Füttern", "/api/pad/fiona/feed", "{}")),
                nav("Pflegen", v -> postDigiPadAction(out, "Pflegen", "/api/pad/fiona/care", "{}"))
        );

        row(p,
                nav("Training Fokus", v -> postDigiPadAction(out, "Training Fokus", "/api/pad/fiona/train", "{\"training_type\":\"focus\"}")),
                nav("Training Speed", v -> postDigiPadAction(out, "Training Speed", "/api/pad/fiona/train", "{\"training_type\":\"speed\"}"))
        );

        row(p,
                nav("Freikampf", v -> postDigiPadAction(out, "Freikampf", "/api/pad/fiona/freefight", "{}")),
                nav("Arena", v -> postDigiPadAction(out, "Arena", "/api/pad/fiona/arena", "{}"))
        );

        row(p,
                nav("Evolution", v -> postDigiPadAction(out, "Evolution", "/api/pad/fiona/evolve", "{}")),
                nav("Battle Export", v -> loadDigiPadEndpoint(out, "Battle Export", "/api/pad/fiona/battle/export"))
        );

        p.addView(section("HINWEIS"));
        p.addView(label("Fionas Handy nutzt später die WLAN- oder Tailscale-Adresse von Patricks Host auf Port 8788. 127.0.0.1 funktioniert nur auf dem Gerät, auf dem Termux läuft.", 12, false, Color.rgb(220, 214, 206)));
    }

    private String digipadBase() {
        return NexusConfig.prefs(this).getString("digipad_base_url", "http://127.0.0.1:8788");
    }

    private String digipadToken() {
        return NexusConfig.prefs(this).getString("digipad_fiona_token", "");
    }

    private void setDigiPadBase(String value) {
        String v = value == null ? "" : value.trim();
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (v.isEmpty()) v = "http://127.0.0.1:8788";
        NexusConfig.prefs(this).edit().putString("digipad_base_url", v).apply();
    }

    private void setDigiPadToken(String value) {
        String v = value == null ? "" : value.trim();
        NexusConfig.prefs(this).edit().putString("digipad_fiona_token", v).apply();
    }

    private void saveDigiPadSettings(TextView out) {
        if (endpointInput != null) setDigiPadBase(endpointInput.getText().toString());
        if (digipadTokenInput != null) setDigiPadToken(digipadTokenInput.getText().toString());
        out.setText("DigiPad gespeichert.\nAPI: " + digipadBase()
                + "\nToken: " + (digipadToken().isEmpty() ? "fehlt" : "gespeichert")
                + "\n\nJetzt Status drücken.");
    }

    private boolean requireDigiPadAuth(TextView out) {
        if (endpointInput != null) setDigiPadBase(endpointInput.getText().toString());
        if (digipadTokenInput != null) setDigiPadToken(digipadTokenInput.getText().toString());

        if (digipadToken().isEmpty()) {
            out.setText("DigiPad Token fehlt.\nToken auf Patricks Host liegt lokal in:\n.run/fiona_digipad_token.txt\n\nToken einmal auf Fionas Handy eintragen und speichern.");
            return false;
        }
        return true;
    }

    private void loadDigiPadEndpoint(TextView out, String title, String path) {
        if (!requireDigiPadAuth(out)) return;
        String base = digipadBase();
        String token = digipadToken();
        out.setText("Lade " + title + "...\n" + base + path);
        new Thread(() -> {
            try {
                String body = httpGetToken(base + path, "X-DigiPad-Token", token);
                String text = digipadRender(title, body, base);
                runOnUiThread(() -> { if (PAGE_DIGIPAD.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "DigiPad nicht erreichbar.\n"
                        + "API: " + base + "\n"
                        + "Fehler: " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 180) + "\n\n"
                        + "Prüfen: Port 8788, WLAN/Tailscale, Token.";
                runOnUiThread(() -> { if (PAGE_DIGIPAD.equals(currentPage)) out.setText(err); });
            }
        }).start();
    }

    private void postDigiPadAction(TextView out, String title, String path, String jsonBody) {
        if (!requireDigiPadAuth(out)) return;
        String base = digipadBase();
        String token = digipadToken();
        out.setText("Sende " + title + "...\n" + base + path);
        new Thread(() -> {
            try {
                String body = httpPostJsonToken(base + path, jsonBody, "X-DigiPad-Token", token);
                String text = digipadRender(title, body, base);
                runOnUiThread(() -> { if (PAGE_DIGIPAD.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "DigiPad-Aktion fehlgeschlagen.\n"
                        + "Aktion: " + title + "\n"
                        + "API: " + base + "\n"
                        + "Fehler: " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 180);
                runOnUiThread(() -> { if (PAGE_DIGIPAD.equals(currentPage)) out.setText(err); });
            }
        }).start();
    }

    private String digipadRender(String title, String body, String base) {
        try {
            JSONObject root = new JSONObject(body);
            StringBuilder sb = new StringBuilder();
            sb.append(title).append("\n");
            sb.append("Quelle: ").append(host(base)).append("\n\n");

            if (!root.optBoolean("ok", true)) {
                sb.append("Status: Fehler\n");
                sb.append(root.optString("error", root.optString("message", "ok=false")));
                return sb.toString().trim();
            }

            String msg = root.optString("message", "");
            if (!msg.isEmpty()) sb.append(msg).append("\n\n");

            JSONObject profile = root.optJSONObject("profile");
            if (profile != null) {
                sb.append("Profil: ").append(profile.optString("display_name", profile.optString("profile_id", "fiona"))).append("\n");
                sb.append("Zugriff: ").append(profile.optString("access_level", "digipad_only")).append("\n\n");
            }

            JSONObject pet = root.optJSONObject("pet");
            if (pet == null) {
                JSONObject status = root.optJSONObject("status");
                if (status != null) pet = status.optJSONObject("pet");
            }

            if (pet != null) {
                sb.append("Pet: ").append(pet.optString("name", "Fiona-Drache")).append("\n");
                sb.append("Stufe: ").append(pet.optString("stage_label", pet.optString("stage", "?"))).append("\n");
                sb.append("Level: ").append(pet.optInt("level", 0)).append(" | XP: ").append(pet.optInt("xp", 0)).append("\n");
                sb.append("Pfad: ").append(pet.optString("evolution_path", "unknown")).append("\n\n");
                sb.append("Zustand\n");
                sb.append("Energie: ").append(pet.optInt("energy", 0)).append(" | Stimmung: ").append(pet.optInt("mood", 0)).append("\n");
                sb.append("Bindung: ").append(pet.optInt("bond", 0)).append(" | Kampfbereit: ").append(pet.optInt("battle_ready", 0)).append("\n");
                sb.append("HP: ").append(pet.optInt("hp", 0)).append("/").append(pet.optInt("max_hp", 0)).append("\n\n");
                sb.append("Battle Rating: ").append(pet.optInt("battle_rating", 0)).append("\n\n");

                JSONArray attacks = pet.optJSONArray("attacks");
                if (attacks != null && attacks.length() > 0) {
                    sb.append("Attacken\n");
                    int max = Math.min(attacks.length(), 7);
                    for (int i = 0; i < max; i++) {
                        JSONObject a = attacks.optJSONObject(i);
                        if (a == null) continue;
                        sb.append("- ").append(a.optString("name", a.optString("id", "?")))
                                .append(" [").append(a.optString("element", "?"))
                                .append("/").append(a.optString("class", "?")).append("]");
                        if (a.optInt("equipped", 0) == 1) sb.append(" aktiv");
                        sb.append("\n");
                    }
                }
            }

            JSONArray unlocks = root.optJSONArray("unlocks");
            if (unlocks != null && unlocks.length() > 0) {
                sb.append("\nNeue Freischaltungen\n");
                for (int i = 0; i < unlocks.length(); i++) sb.append("- ").append(unlocks.optString(i)).append("\n");
            }

            JSONArray levelNotes = root.optJSONArray("level_notes");
            if (levelNotes != null && levelNotes.length() > 0) {
                sb.append("\nLevel\n");
                for (int i = 0; i < levelNotes.length(); i++) sb.append("- ").append(levelNotes.optString(i)).append("\n");
            }

            String code = root.optString("code", "");
            if (!code.isEmpty()) {
                sb.append("\nBattle Code\n");
                sb.append(code).append("\n");
                JSONObject snapshot = root.optJSONObject("snapshot");
                if (snapshot != null) {
                    sb.append("Snapshot: ").append(snapshot.optString("pet_name", "Pet"))
                            .append(" L").append(snapshot.optInt("level", 0))
                            .append(" BR ").append(snapshot.optInt("battle_rating", 0)).append("\n");
                }
            }

            return sb.toString().trim();
        } catch (Exception ex) {
            return title + "\nQuelle: " + host(base) + "\n\n" + cutKeepLines(body, 5000);
        }
    }

    private void showStatusOnly() {
        clearPage(PAGE_HOME, "Status", "Aktueller App- und Collector-Zustand.");
        activePanel().addView(label(status(), 13, false, Color.rgb(230, 225, 216)));
    }

    private void showCompanionPage() {
        clearPage(PAGE_COMPANION, "Digi Dragon", "Echter Digi-Dragon-Core über lokale Termux-Bridge. Offline startfähig, Aktionen nur auf Knopfdruck.");
        LinearLayout p = activePanel();

        p.addView(section("BRIDGE"));
        endpointInput = input("http://127.0.0.1:8777", true);
        endpointInput.setText(dragonBridgeBase());
        p.addView(endpointInput, card(8));

        TextView out = logBox("Digi Dragon bereit.\nBridge: " + dragonBridgeBase()
                + "\nKeine automatische Abfrage beim App-Start.\nTermux muss laufen: ./tools/nexus_start_all.sh");
        p.addView(out, card(8));

        row(p,
                nav("Bridge speichern", v -> { setDragonBridgeBase(endpointInput.getText().toString()); out.setText("Digi Dragon Bridge gespeichert:\n" + dragonBridgeBase()); }),
                nav("Status", v -> loadDragonEndpoint(out, "Digi Dragon Status", "/api/dragon/status"))
        );

        row(p,
                nav("Habitat", v -> loadDragonEndpoint(out, "Digi Dragon Habitat", "/api/dragon/habitat")),
                nav("Codex", v -> loadDragonEndpoint(out, "Digi Dragon Codex", "/api/dragon/codex"))
        );

        p.addView(section("AKTIONEN"));

        row(p,
                nav("Füttern", v -> postDragonAction(out, "Füttern", "/api/dragon/feed", "{}")),
                nav("Pflegen", v -> postDragonAction(out, "Pflegen", "/api/dragon/care", "{}"))
        );

        row(p,
                nav("Training Fokus", v -> postDragonAction(out, "Training Fokus", "/api/dragon/train", "{\"training_type\":\"focus\"}")),
                nav("Training Kraft", v -> postDragonAction(out, "Training Kraft", "/api/dragon/train", "{\"training_type\":\"strength\"}"))
        );

        row(p,
                nav("Freikampf", v -> postDragonAction(out, "Freikampf", "/api/dragon/freefight", "{}")),
                nav("Arena", v -> postDragonAction(out, "Arena", "/api/dragon/arena", "{}"))
        );

        row(p,
                nav("Evolution", v -> postDragonAction(out, "Evolution", "/api/dragon/evolve", "{}")),
                nav("Chef mit Zustand", v -> dragonChef(out))
        );
    }

    private String dragonBridgeBase() {
        return NexusConfig.prefs(this).getString("dragon_bridge_url", "http://127.0.0.1:8777");
    }

    private void setDragonBridgeBase(String value) {
        String v = value == null ? "" : value.trim();
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (v.isEmpty()) v = "http://127.0.0.1:8777";
        NexusConfig.prefs(this).edit().putString("dragon_bridge_url", v).apply();
    }

    private void loadDragonEndpoint(TextView out, String title, String path) {
        String base = dragonBridgeBase();
        out.setText("Lade " + title + "...\n" + base + path);
        new Thread(() -> {
            try {
                String body = httpGet(base + path);
                String text = dragonRender(title, body, base);
                runOnUiThread(() -> { if (PAGE_COMPANION.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "Digi Dragon nicht erreichbar.\n"
                        + "Bridge: " + base + "\n"
                        + "Fehler: " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 160) + "\n\n"
                        + "Termux prüfen:\n"
                        + "cd ~/Nexus-cleanwork\n"
                        + "./tools/nexus_start_all.sh\n"
                        + "./tools/nexus_doctor.sh";
                runOnUiThread(() -> { if (PAGE_COMPANION.equals(currentPage)) out.setText(err); });
            }
        }).start();
    }

    private void postDragonAction(TextView out, String title, String path, String jsonBody) {
        String base = dragonBridgeBase();
        out.setText("Sende " + title + "...\n" + base + path);
        new Thread(() -> {
            try {
                String body = httpPost(base + path, jsonBody);
                String text = dragonRender(title, body, base);
                runOnUiThread(() -> { if (PAGE_COMPANION.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "Digi-Dragon-Aktion fehlgeschlagen.\n"
                        + "Aktion: " + title + "\n"
                        + "Bridge: " + base + "\n"
                        + "Fehler: " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 160);
                runOnUiThread(() -> { if (PAGE_COMPANION.equals(currentPage)) out.setText(err); });
            }
        }).start();
    }

    private String dragonRender(String title, String body, String base) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject state = root.optJSONObject("state");
            if (state == null) state = root;

            StringBuilder sb = new StringBuilder();
            sb.append(title).append("\n");
            sb.append("Quelle: ").append(host(base)).append("\n\n");

            String msg = root.optString("message", "");
            if (!msg.isEmpty()) sb.append(msg).append("\n\n");

            sb.append("Drache: ").append(state.optString("name", "Nexus-Drache")).append("\n");
            sb.append("Stufe: ").append(state.optString("stage_label", state.optString("stage", "?"))).append("\n");
            sb.append("Level: ").append(state.optInt("level", 0)).append(" | XP: ").append(state.optInt("xp", 0)).append("\n");
            sb.append("Pfad: ").append(state.optString("evolution_path", "unknown")).append("\n\n");

            sb.append("Zustand\n");
            sb.append("Energie: ").append(state.optInt("energy", 0)).append(" | Stimmung: ").append(state.optInt("mood", 0)).append("\n");
            sb.append("Bindung: ").append(state.optInt("bond", 0)).append(" | Kampfbereit: ").append(state.optInt("battle_ready", 0)).append("\n");
            sb.append("HP: ").append(state.optInt("hp", 0)).append("/").append(state.optInt("max_hp", 0)).append("\n\n");

            sb.append("Stats\n");
            sb.append("Stärke ").append(state.optInt("strength", 0))
                    .append(" | Ausdauer ").append(state.optInt("endurance", 0))
                    .append(" | Tempo ").append(state.optInt("speed", 0))
                    .append(" | Fokus ").append(state.optInt("focus", 0)).append("\n");
            sb.append("Instinkt ").append(state.optInt("instinct", 0))
                    .append(" | Intelligenz ").append(state.optInt("intelligence", 0))
                    .append(" | Wille ").append(state.optInt("willpower", 0)).append("\n\n");

            JSONObject habitat = state.optJSONObject("habitat");
            if (habitat != null) {
                sb.append("Habitat: ").append(habitat.optString("name", "?"))
                        .append(" / ").append(habitat.optString("theme", "?")).append("\n\n");
            }

            JSONArray attacks = state.optJSONArray("attacks");
            if (attacks != null && attacks.length() > 0) {
                sb.append("Attacken\n");
                int max = Math.min(attacks.length(), 8);
                for (int i = 0; i < max; i++) {
                    JSONObject a = attacks.optJSONObject(i);
                    if (a == null) continue;
                    sb.append("- ").append(a.optString("name", a.optString("id", "?")))
                            .append(" [").append(a.optString("element", "?"))
                            .append("/").append(a.optString("class", "?")).append("]");
                    if (a.optInt("equipped", 0) == 1) sb.append(" aktiv");
                    sb.append("\n");
                }
            }

            JSONArray unlocks = root.optJSONArray("unlocks");
            if (unlocks != null && unlocks.length() > 0) {
                sb.append("\nNeue Freischaltungen\n");
                for (int i = 0; i < unlocks.length(); i++) sb.append("- ").append(unlocks.optString(i)).append("\n");
            }

            JSONArray levelNotes = root.optJSONArray("level_notes");
            if (levelNotes != null && levelNotes.length() > 0) {
                sb.append("\nLevel\n");
                for (int i = 0; i < levelNotes.length(); i++) sb.append("- ").append(levelNotes.optString(i)).append("\n");
            }

            return sb.toString().trim();
        } catch (Exception ex) {
            return title + "\nQuelle: " + host(base) + "\n\n" + cutKeepLines(body, 6500);
        }
    }

    private void dragonChef(TextView out) {
        String state = out == null ? "" : out.getText().toString();
        showChefPage();
        chefInput.setText("Digi-Dragon-Zustand:\n"
                + cutKeepLines(state, 1800)
                + "\n\nBitte kurz und konkret: Welche nächste sinnvolle Aktion im Digi-Dragon-/Nexus-System?");
        if (chefLog != null) chefLog.setText("Digi-Dragon-Zustand bereit. Drücke 'An Chef senden', wenn der Chef wirklich gefragt werden soll.");
    }

    private void companionTrain(TextView out) {
        int energy = companionInt("energy", 80);
        if (energy < 8) {
            out.setText(companionSummary() + "\n\nTraining blockiert: Energie zu niedrig. Erst Ruhig oder Tagesreset.");
            return;
        }
        addCompanionXp(12);
        setCompanionInt("energy", clamp(energy - 8, 0, 100));
        setCompanionInt("stress", clamp(companionInt("stress", 15) - 2, 0, 100));
        out.setText(companionSummary() + "\n\nTraining: +12 XP, Fokus verbessert.");
    }

    private void companionArena(TextView out) {
        int energy = companionInt("energy", 80);
        int xp = companionInt("xp", 0);
        int wins = companionInt("wins", 0);
        int level = companionLevel();
        if (energy < 20) {
            out.setText(companionSummary() + "\n\nArena blockiert: Energie unter 20. Erst regenerieren.");
            return;
        }
        boolean win = ((xp + wins * 7 + level * 11 + (int)(System.currentTimeMillis() / 60000L)) % 5) != 0;
        if (win) {
            setCompanionInt("wins", wins + 1);
            addCompanionXp(24);
            setCompanionInt("energy", clamp(energy - 18, 0, 100));
            setCompanionInt("stress", clamp(companionInt("stress", 15) + 6, 0, 100));
            out.setText(companionSummary() + "\n\nArena: Sieg. +24 XP. Stress steigt leicht.");
        } else {
            addCompanionXp(8);
            setCompanionInt("energy", clamp(energy - 14, 0, 100));
            setCompanionInt("stress", clamp(companionInt("stress", 15) + 10, 0, 100));
            out.setText(companionSummary() + "\n\nArena: Niederlage. +8 XP. Rueckzug und Training empfohlen.");
        }
    }

    private void companionEvolve(TextView out) {
        int xp = companionInt("xp", 0);
        int old = companionInt("level", 1);
        int target = companionLevel();
        setCompanionInt("level", Math.max(old, target));
        out.setText(companionSummary() + "\n\nEvolution geprueft: " + dragonStage() + ". Naechste Schwelle: " + nextEvolutionText() + ".");
    }

    private void companionCalm(TextView out) {
        setCompanionInt("energy", clamp(companionInt("energy", 80) + 15, 0, 100));
        setCompanionInt("stress", clamp(companionInt("stress", 15) - 20, 0, 100));
        addCompanionXp(3);
        out.setText(companionSummary() + "\n\nRuhig: Energie regeneriert, Stress reduziert.");
    }

    private void companionReset(TextView out) {
        setCompanionInt("energy", 85);
        setCompanionInt("stress", 15);
        out.setText(companionSummary() + "\n\nTagesreset: Energie und Stress neu gesetzt. XP und Siege bleiben erhalten.");
    }

    private void companionChef() {
        String state = companionSummary();
        showChefPage();
        chefInput.setText("Companion-Zustand:\n" + state + "\n\nBitte priorisiere meinen Tag kurz und konkret. Was soll ich zuerst beachten?");
        if (chefLog != null) chefLog.setText("Companion-Zustand bereit. Druecke 'An Chef senden', wenn du den Chef wirklich fragen willst.");
    }

    private int companionInt(String key, int fallback) {
        return NexusConfig.prefs(this).getInt("companion_" + key, fallback);
    }

    private void setCompanionInt(String key, int value) {
        NexusConfig.prefs(this).edit().putInt("companion_" + key, value).apply();
    }

    private void addCompanionXp(int delta) {
        int xp = clamp(companionInt("xp", 0) + delta, 0, 9999);
        setCompanionInt("xp", xp);
        setCompanionInt("level", Math.max(companionInt("level", 1), companionLevelForXp(xp)));
    }

    private int companionLevel() { return companionLevelForXp(companionInt("xp", 0)); }
    private int companionLevelForXp(int xp) { return Math.max(1, Math.min(50, 1 + (xp / 100))); }

    private String dragonStage() {
        int xp = companionInt("xp", 0);
        if (xp >= 700) return "Nexus-Drache";
        if (xp >= 320) return "Wachdrache";
        if (xp >= 120) return "Jungdrache";
        return "Nestling";
    }

    private String nextEvolutionText() {
        int xp = companionInt("xp", 0);
        if (xp < 120) return (120 - xp) + " XP bis Jungdrache";
        if (xp < 320) return (320 - xp) + " XP bis Wachdrache";
        if (xp < 700) return (700 - xp) + " XP bis Nexus-Drache";
        return "maximale Stufe aktiv";
    }

    private String companionSummary() {
        return "Mini-Drache: " + dragonStage()
                + "\nLevel: " + companionLevel() + " | XP: " + companionInt("xp", 0) + " | Siege: " + companionInt("wins", 0)
                + "\nEnergie: " + companionInt("energy", 80) + " | Stress: " + companionInt("stress", 15)
                + "\nNaechste Evolution: " + nextEvolutionText()
                + "\nPrinzip: lokal, kostenlos, erst Chef-Button nutzt den Chef-Kanal.";
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

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
            if (item == null || isHidden(item.optString("event_id", "")) || isClosedDecision(item)) continue;
            sb.append(shown + 1).append(". ").append(senderOf(item)).append(" - ")
                    .append(cut(previewOf(item), 120)).append('\n');
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
        clearPage(PAGE_MESSAGES, "Nachrichten", "Eigene Seite. Suche liest Gespraeche und bei Suchwort auch Roh-Events.");
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
        final String needle = filter == null ? "" : filter.trim().toLowerCase();
        summary.setText("Lade Nachrichten...");
        list.removeAllViews();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.baseUrlCandidates(this)) {
                try {
                    JSONObject conversations;
                    try {
                        conversations = new JSONObject(httpGet(base + "/api/communication/conversations?limit=200"));
                    } catch (Exception convEx) {
                        conversations = new JSONObject(httpGet(base + "/api/widget/messages?limit=20"));
                        conversations.put("_fallback", "widget");
                    }
                    if (!conversations.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }

                    JSONObject events = null;
                    if (!needle.isEmpty()) {
                        try {
                            events = new JSONObject(httpGet(base + "/api/communication/events?limit=2000"));
                        } catch (Exception ignored) {
                            events = null;
                        }
                    }

                    NexusConfig.rememberWorkingBaseUrl(this, base);
                    final JSONObject convFinal = conversations;
                    final JSONObject eventsFinal = events;
                    runOnUiThread(() -> renderMessages(summary, list, convFinal, eventsFinal, base, needle));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 70); }
            }
            final String err = last;
            runOnUiThread(() -> { summary.setText("Nachrichten nicht erreichbar: " + err); list.removeAllViews(); });
        }).start();
    }

    private void renderMessages(TextView summary, LinearLayout list, JSONObject conversationsRoot, JSONObject eventsRoot, String base, String needle) {
        if (!PAGE_MESSAGES.equals(currentPage)) return;
        list.removeAllViews();
        Set<String> seen = new HashSet<>();
        JSONArray conversations = itemsArray(conversationsRoot);
        JSONArray events = eventsRoot == null ? null : itemsArray(eventsRoot);
        int shown = 0;
        int loaded = (conversations == null ? 0 : conversations.length()) + (events == null ? 0 : events.length());
        if (conversations != null) shown += renderMessageItems(list, conversations, needle, seen, shown, 70);
        if (events != null) shown += renderMessageItems(list, events, needle, seen, shown, 70);

        String source = "Quelle: " + host(base);
        String basis = conversationsRoot.optString("_fallback", "").equals("widget")
                ? "Basis: Widget-Fallback 20"
                : "Basis: Gespraeche 200" + (events == null ? "" : " + Events 2000");
        String filterText = needle == null || needle.isEmpty() ? "" : " | Suche: " + needle;
        summary.setText(source + "\n" + basis + "\nGeladen: " + loaded + " | Sichtbar: " + shown + filterText);
        if (shown == 0) list.addView(logBox("Keine Treffer. Suchwort vereinfachen oder im Web pruefen, ob der Server diese Nachricht bereits aufgenommen hat."));
    }

    private JSONArray itemsArray(JSONObject root) {
        if (root == null) return null;
        JSONArray items = root.optJSONArray("items");
        if (items != null) return items;
        JSONObject data = root.optJSONObject("data");
        return data == null ? null : data.optJSONArray("items");
    }

    private int renderMessageItems(LinearLayout list, JSONArray items, String needle, Set<String> seen, int start, int maxTotal) {
        int added = 0;
        for (int i = 0; i < items.length() && start + added < maxTotal; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String eventId = item.optString("event_id", item.optString("conversation_key", ""));
            if (eventId.isEmpty()) eventId = item.optString("id", "");
            String unique = eventId.isEmpty() ? senderOf(item) + "|" + previewOf(item) : eventId;
            if (seen.contains(unique) || isHidden(eventId) || isClosedDecision(item)) continue;
            if (!matchesMessage(item, needle)) continue;
            seen.add(unique);

            final String finalEventId = eventId;
            final String finalSender = senderOf(item);
            final String finalPreview = previewOf(item);
            LinearLayout card = miniCard();
            card.addView(label((start + added + 1) + ". [" + priorityOf(item) + "] " + finalSender, 15, true, Color.WHITE));
            card.addView(label(actionOf(item), 11, true, orange()));
            card.addView(label(cut(finalPreview, 310), 13, false, Color.rgb(232, 226, 216)));
            row(card, nav("Sehr wichtig", v -> decide(finalEventId, "very_important")), nav("Erledigt", v -> decide(finalEventId, "done")));
            row(card, nav("Zeitstrahl", v -> decide(finalEventId, "timeline_focus")), nav("Chef-Kontext", v -> putContext(finalSender, finalPreview)));
            list.addView(card, card(8));
            added++;
        }
        return added;
    }

    private boolean isClosedDecision(JSONObject item) {
        String decision = item == null ? "" : item.optString("latest_decision", "");
        return "done".equals(decision) || "not_important".equals(decision);
    }

    private boolean matchesMessage(JSONObject item, String needle) {
        if (needle == null || needle.trim().isEmpty()) return true;
        StringBuilder hay = new StringBuilder();
        hay.append(senderOf(item)).append(' ').append(previewOf(item)).append(' ').append(actionOf(item)).append(' ');
        hay.append(item.optString("source", "")).append(' ').append(item.optString("channel_type", "")).append(' ');
        hay.append(item.optString("title", "")).append(' ').append(item.optString("sender_raw", "")).append(' ');
        JSONObject sem = item.optJSONObject("semantic");
        if (sem != null) hay.append(sem.optString("category", "")).append(' ');
        JSONObject chef = item.optJSONObject("chef_assessment");
        if (chef != null) hay.append(chef.optString("domain", "")).append(' ').append(chef.optString("suggested_action", ""));
        return hay.toString().toLowerCase().contains(needle.toLowerCase());
    }

    private String senderOf(JSONObject item) {
        String s = item.optString("sender", "");
        if (s.isEmpty()) s = item.optString("sender_raw", "");
        if (s.isEmpty()) s = item.optString("title", "");
        if (s.isEmpty()) s = item.optString("source", "");
        return s.isEmpty() ? "Unbekannt" : s;
    }

    private String previewOf(JSONObject item) {
        String p = item.optString("body_preview", "");
        if (p.isEmpty()) p = item.optString("body", "");
        if (p.isEmpty()) p = item.optString("title", "");
        return p;
    }

    private String actionOf(JSONObject item) {
        String a = item.optString("suggested_action", "");
        if (a.isEmpty()) {
            JSONObject chef = item.optJSONObject("chef_assessment");
            if (chef != null) a = chef.optString("suggested_action", "");
        }
        return a.isEmpty() ? "pruefen" : a;
    }

    private String priorityOf(JSONObject item) {
        String p = item.optString("priority_band", "");
        if (p.isEmpty()) {
            JSONObject chef = item.optJSONObject("chef_assessment");
            if (chef != null) p = chef.optString("priority_band", "");
        }
        return p.isEmpty() ? "P?" : p;
    }

    private void putContext(String sender, String preview) {
        showChefPage();
        chefInput.setText("Kontext zu Nachricht von " + sender + ":\n" + cut(preview, 220) + "\n\nMeine Einordnung: ");
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
        return "Server: " + NexusConfig.baseUrl(this) + "\nBenachrichtigungszugriff: " + (notificationAccess() ? "aktiv" : "fehlt") + " | SMS: " + (smsPermission() ? "aktiv" : "fehlt") + "\nRechte jetzt im Android-System sichtbar: NotificationListener + SMS Receiver sind im Manifest registriert.";
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
                + "Widget: " + NexusConfig.lastWidgetStatus(this) + "\n"
                + "Digi Dragon: Bridge 8777 | Lokal " + dragonStage() + " L" + companionLevel() + "\n"
                + "Theme: " + themeName();
    }

    private boolean smsPermission() { return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED; }
    private void requestSms() {
        if (!smsPermission()) {
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 1001);
        } else {
            showStatusOnly();
        }
    }
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
        e.setBackground(box(14, Color.rgb(13, 14, 14), accentDark()));
        return e;
    }

    private TextView logBox(String text) {
        TextView v = label(text, 13, false, Color.rgb(232, 226, 218));
        v.setPadding(dp(10), dp(10), dp(10), dp(10));
        v.setLineSpacing(dp(2), 1.08f);
        v.setTextIsSelectable(true);
        v.setBackground(box(14, Color.rgb(8, 9, 9), accentDark()));
        return v;
    }

    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout panel() { LinearLayout l = vertical(); l.setPadding(dp(10), dp(10), dp(10), dp(10)); l.setBackground(box(18, Color.rgb(18, 19, 17), accentDark())); l.setElevation(dp(7)); return l; }
    private LinearLayout miniCard() { LinearLayout l = vertical(); l.setPadding(dp(9), dp(8), dp(9), dp(8)); l.setBackground(box(15, Color.rgb(11, 12, 11), accentDark())); l.setElevation(dp(3)); return l; }
    private TextView section(String s) { return label(s, 12, true, orange()); }
    private TextView label(String s, int sp, boolean bold, int color) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START); v.setPadding(0, dp(4), 0, dp(5)); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private Button nav(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(10); b.setTypeface(Typeface.DEFAULT_BOLD); b.setTextColor(buttonText()); b.setMinHeight(dp(30)); b.setMinimumHeight(0); b.setPadding(dp(4), 0, dp(4), 0); b.setBackground(box(12, accentStart(), accentEnd())); b.setOnClickListener(l); return b; }
    private void row(LinearLayout parent, Button a, Button b) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lp.setMargins(0, dp(4), dp(4), 0); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); rp.setMargins(dp(4), dp(4), 0, 0); r.addView(a, lp); r.addView(b, rp); parent.addView(r); }
    private LinearLayout.LayoutParams card(int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, dp(top), 0, 0); return lp; }
    private GradientDrawable box(int radius, int fill, int stroke) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{fill, Color.rgb(4, 5, 5)}); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), stroke); return d; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int orange() { return accentText(); }
    private int sub() { return Color.rgb(185, 178, 168); }

    private String themeName() { return NexusConfig.prefs(this).getString("ui_theme", "orange"); }
    private void setTheme(String name) { NexusConfig.prefs(this).edit().putString("ui_theme", name == null ? "orange" : name).apply(); setContentView(buildUi()); }
    private int accentStart() { String t = themeName(); if ("blue".equals(t)) return Color.rgb(0, 192, 255); if ("green".equals(t)) return Color.rgb(0, 255, 102); return Color.rgb(255, 158, 38); }
    private int accentEnd() { String t = themeName(); if ("blue".equals(t)) return Color.rgb(0, 78, 255); if ("green".equals(t)) return Color.rgb(0, 150, 70); return Color.rgb(255, 180, 58); }
    private int accentText() { String t = themeName(); if ("blue".equals(t)) return Color.rgb(0, 192, 255); if ("green".equals(t)) return Color.rgb(0, 255, 102); return Color.rgb(255, 169, 54); }
    private int accentDark() { String t = themeName(); if ("blue".equals(t)) return Color.rgb(16, 53, 88); if ("green".equals(t)) return Color.rgb(18, 72, 39); return Color.rgb(72, 47, 31); }
    private int buttonText() { return "blue".equals(themeName()) ? Color.WHITE : Color.rgb(18, 13, 8); }

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


    private static String httpGetToken(String url, String headerName, String headerValue) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(4500);
            c.setReadTimeout(7000);
            c.setRequestMethod("GET");
            c.setRequestProperty("X-Nexus-Collector", "nexus-collector-app-v3");
            if (headerName != null && !headerName.isEmpty() && headerValue != null && !headerValue.isEmpty()) {
                c.setRequestProperty(headerName, headerValue);
            }
            int code = c.getResponseCode();
            String body = readAll(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " " + cut(body, 160));
            return body;
        } finally { if (c != null) c.disconnect(); }
    }

    private static String httpPostJsonToken(String url, String body, String headerName, String headerValue) throws Exception {
        HttpURLConnection c = null;
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(4500);
            c.setReadTimeout(7000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("X-Nexus-Collector", "nexus-collector-app-v3");
            if (headerName != null && !headerName.isEmpty() && headerValue != null && !headerValue.isEmpty()) {
                c.setRequestProperty(headerName, headerValue);
            }
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            int code = c.getResponseCode();
            String res = readAll(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
            if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + " " + cut(res, 160));
            return res;
        } finally { if (c != null) c.disconnect(); }
    }

    private static String readAll(InputStream s) throws Exception { if (s == null) return ""; StringBuilder sb = new StringBuilder(); try (BufferedReader r = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8))) { String line; while ((line = r.readLine()) != null) sb.append(line); } return sb.toString(); }
    private static String enc(String v) throws Exception { return URLEncoder.encode(v == null ? "" : v, "UTF-8"); }
    private static String host(String base) { try { URL u = new URL(base); return u.getHost() + (u.getPort() > 0 ? ":" + u.getPort() : ""); } catch (Exception e) { return base == null ? "" : base; } }
    private static String cut(String v, int max) { if (v == null) return ""; String c = v.replace('\n', ' ').replace('\r', ' ').trim(); return c.length() <= max ? c : c.substring(0, Math.max(0, max - 3)) + "..."; }
    private static String cutKeepLines(String v, int max) { if (v == null) return ""; String c = v.replace("\r", "").trim(); return c.length() <= max ? c : c.substring(0, Math.max(0, max - 12)) + "\n...<cut>"; }
}
