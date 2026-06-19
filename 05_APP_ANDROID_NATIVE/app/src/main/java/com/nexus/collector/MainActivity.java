package com.nexus.collector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String PAGE_HOME = "home";
    private static final String PAGE_STATUS = "status";
    private static final String PAGE_CHEF = "chef";
    private static final String PAGE_MESSAGES = "messages";
    private static final String PAGE_FILES = "files";
    private static final String PAGE_TIMELINE = "timeline";
    private static final String PAGE_DRAGON = "dragon";
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
    private String currentFilesPath = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        installCrashGuard();
        migrateBridgeDefaults();
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

    private void migrateBridgeDefaults() {
        try {
            android.content.SharedPreferences prefs = NexusConfig.prefs(this);
            android.content.SharedPreferences.Editor edit = prefs.edit();
            boolean changed = false;

            String nexy = prefs.getString("nexy_bridge_url", "");
            if (nexy == null || nexy.trim().isEmpty()
                    || nexy.contains("192.168.1.216:8765")
                    || nexy.contains("127.0.0.1:8765")) {
                edit.putString("nexy_bridge_url", "http://100.107.24.67:8765");
                changed = true;
            }

            String dragon = prefs.getString("dragon_bridge_url", "");
            if (dragon == null || dragon.trim().isEmpty()
                    || dragon.contains(":8766")
                    || dragon.contains("192.168.1.216")
                    || dragon.contains("100.107.24.67")) {
                edit.putString("dragon_bridge_url", "http://127.0.0.1:8777");
                changed = true;
            }

            if (changed) edit.apply();
        } catch (Throwable ignored) {}
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
        FrameLayout frame = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        mainScroll = scroll;
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(Color.rgb(3, 4, 5));

        LinearLayout root = vertical();
        root.setPadding(dp(10), dp(12), dp(10), dp(18));
        scroll.addView(root);

        LinearLayout titleCol = vertical();
        topTitle = logoLabel("NEXUS");
        topSub = label("", 12, false, sub());
        topSub.setGravity(Gravity.CENTER);
        titleCol.addView(topTitle);
        topSub.setVisibility(View.GONE);
        LinearLayout header = panel();
        header.addView(titleCol);
        root.addView(header, card(0));

        content = vertical();
        root.addView(content, card(8));

        frame.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        frame.addView(dragonWanderer(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return frame;
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
        topTitle.setText(BuildConfig.DIGIPAD_ONLY ? "DIGIPAD" : (PAGE_HOME.equals(page) ? "NEXUS" : title.toUpperCase(Locale.ROOT)));
        topTitle.setTextColor(accentText());
        topSub.setText("");
        topSub.setVisibility(View.GONE);

        if (!BuildConfig.DIGIPAD_ONLY && !PAGE_HOME.equals(page)) {
            LinearLayout back = panel();
            row(back, nav("Zurueck zur Zentrale", v -> showHome()));
            content.addView(back, card(0));
        }

        LinearLayout pagePanel = panel();
        pagePanel.addView(label(title, 21, true, orange()));
        content.addView(pagePanel, card(PAGE_HOME.equals(page) ? 0 : 8));
        if (mainScroll != null) mainScroll.post(() -> mainScroll.scrollTo(0, 0));
        safeFullscreen();
    }

    private LinearLayout activePanel() {
        return (LinearLayout) content.getChildAt(content.getChildCount() - 1);
    }

    private void showHome() {
        clearPage(PAGE_HOME, "Zentrale", "Seitliche Navigation. Rechts die wichtigsten Arbeitsbereiche.");
        LinearLayout p = activePanel();
        float screenDp = getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density;
        int homePanelMinDp = Math.max(720, (int) screenDp - 170);
        p.setMinimumHeight(dp(homePanelMinDp));

        boolean slideOpen = homeSlideOpen();
        FrameLayout shell = new FrameLayout(this);
        shell.setMinimumHeight(dp(homePanelMinDp - 48));
        LinearLayout deck = vertical();
        deck.setPadding(dp(48), 0, 0, 0);
        shell.addView(deck, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        deck.addView(label("Heute", 20, true, Color.WHITE));
        deck.addView(label(shortSystemLine(), 11, false, sub()));

        homeDeckTile(deck, "Nexi", "Direkter Kanal", "Auftrag oder Kontext senden", v -> showChefPage());
        homeDeckTile(deck, "Eingang", "Nachrichten", "Suchen, markieren, entscheiden", v -> showMessagesPage());
        homeDeckTile(deck, "Chronik", "Zeitstrahl", "Was passiert ist, in Reihenfolge", v -> showTimelinePage());
        homeDeckTile(deck, "Explorer", "Dateien", "Nexus-Speicher durchsuchen", v -> showFilesPage());

        LinearLayout quick = quickPanel();
        FrameLayout.LayoutParams quickLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        quickLp.gravity = Gravity.BOTTOM;
        quickLp.setMargins(dp(54), 0, dp(18), dp(4));
        shell.addView(quick, quickLp);

        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(dp(22), dp(74));
        handleLp.gravity = Gravity.START | Gravity.TOP;
        handleLp.setMargins(0, dp(8), 0, 0);
        shell.addView(drawerHandleClean(slideOpen), handleLp);

        if (slideOpen) {
            FrameLayout.LayoutParams drawerLp = new FrameLayout.LayoutParams(dp(136), LinearLayout.LayoutParams.WRAP_CONTENT);
            drawerLp.gravity = Gravity.START | Gravity.TOP;
            shell.addView(homeDrawerClean(), drawerLp);
        }

        p.addView(shell, card(8));
    }

    private View dragonWanderer() {
        return new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private Bitmap shadowYoung;

            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (canvas == null || getWidth() <= 0 || getHeight() <= 0 || !menuPetVisible()) return;

                if (shadowYoung == null) {
                    shadowYoung = BitmapFactory.decodeResource(getResources(), R.drawable.dragon_schatten_jung_pet);
                }
                if (shadowYoung == null) return;

                float w = getWidth();
                float h = getHeight();
                float cycle = 115000f;
                long now = System.currentTimeMillis();
                float phase = (System.currentTimeMillis() % (long) cycle) / cycle;
                float pageOffset = Math.abs(currentPage == null ? 0 : currentPage.hashCode() % 1000) / 1000f;
                float t = (phase + pageOffset) % 1f;

                float topLimit = PAGE_DRAGON.equals(currentPage) ? dp(236) : dp(136);
                float size = Math.max(dp(44), Math.min(dp(62), w * 0.115f));
                float bw = shadowYoung.getWidth();
                float bh = shadowYoung.getHeight();
                float drawW = size;
                float drawH = size * (bh / Math.max(1f, bw));
                float highY = Math.max(dp(116), topLimit - dp(28));
                float midY = topLimit + Math.max(dp(36), (h - topLimit - drawH) * 0.36f);
                float lowY = h - drawH - dp(32);
                float centerX = w * 0.50f;
                float centerY = highY + Math.max(dp(80), (lowY - highY) * 0.52f);
                float radiusX = Math.max(dp(118), (w - drawW) * 0.44f);
                float radiusY = Math.max(dp(116), Math.max(dp(120), (lowY - highY) * 0.43f));
                float a = (float) (t * Math.PI * 2f);
                float x = centerX
                        + (float) Math.cos(a) * radiusX
                        + (float) Math.sin(a * 2.6f + 0.4f) * radiusX * 0.12f
                        - drawW / 2f;
                float y = centerY
                        + (float) Math.sin(a) * radiusY
                        + (float) Math.sin(a * 1.7f + 1.1f) * radiusY * 0.10f
                        - drawH / 2f;
                float dx = (float) (-Math.sin(a) * radiusX + Math.cos(a * 2.6f + 0.4f) * radiusX * 0.31f);
                float dy = (float) (Math.cos(a) * radiusY + Math.cos(a * 1.7f + 1.1f) * radiusY * 0.17f);
                boolean flip = dx > 0f;
                float tiltBase = Math.max(-18f, Math.min(18f, dy / Math.max(dp(24), Math.abs(dx)) * 14f));
                float tilt = flip ? tiltBase : -tiltBase;
                int motionMode;
                boolean lowAndLevel = y > lowY - dp(80) && Math.abs(dy) < Math.abs(dx) * 0.55f;
                boolean climbing = Math.abs(dy) > Math.abs(dx) * 1.15f && x < dp(86);
                if (lowAndLevel) {
                    motionMode = 0;
                    tilt *= 0.35f;
                } else if (climbing) {
                    motionMode = 1;
                } else {
                    motionMode = 2;
                }

                x = Math.max(dp(4), Math.min(w - drawW - dp(4), x));
                y = Math.max(topLimit, Math.min(h - drawH - dp(18), y));

                paint.setAlpha(28);
                paint.setColor(Color.rgb(112, 42, 180));
                canvas.drawOval(x + dp(4), y + drawH - dp(8), x + drawW - dp(4), y + drawH + dp(4), paint);

                canvas.save();
                canvas.rotate(tilt, x + drawW / 2f, y + drawH / 2f);
                if (flip) {
                    canvas.scale(-1f, 1f, x + drawW / 2f, y + drawH / 2f);
                }
                float gait = (now % (motionMode == 2 ? 820L : 1800L)) / (motionMode == 2 ? 820f : 1800f);
                drawWandererMotion(canvas, x, y, drawW, drawH, gait, motionMode, true);
                drawAnimatedPetBitmap(canvas, shadowYoung, x, y, drawW, drawH, gait, motionMode);
                drawWandererMotion(canvas, x, y, drawW, drawH, gait, motionMode, false);

                paint.setAlpha(24);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(Color.rgb(150, 54, 230));
                canvas.drawOval(x + dp(2), y + dp(2), x + drawW - dp(2), y + drawH - dp(2), paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.restore();

                postInvalidateOnAnimation();
            }

            @Override public boolean onTouchEvent(MotionEvent event) {
                return false;
            }

            private float smooth(float t) {
                float v = Math.max(0f, Math.min(1f, t));
                return v * v * (3f - 2f * v);
            }

            private void drawWandererMotion(Canvas canvas, float x, float y, float w, float h, float gait, int mode, boolean behind) {
                float step = (float) Math.sin(gait * Math.PI * 2f);
                float counter = (float) Math.cos(gait * Math.PI * 2f);
                float wingBeat = mode == 2 ? (float) Math.sin(gait * Math.PI * 8f) : step;
                float legSwing = mode == 0 ? step : mode == 1 ? counter * 0.55f : step * 0.18f;
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeWidth(Math.max(1f, w * 0.026f));
                paint.setColor(behind ? Color.argb(58, 42, 8, 58) : Color.argb(120, 156, 54, 228));

                float bodyX = x + w * 0.45f;
                float bodyY = y + h * 0.55f;
                float headX = x + w * 0.30f;
                float headY = y + h * 0.40f + step * h * 0.025f;

                Path wing = new Path();
                wing.moveTo(bodyX, bodyY);
                wing.quadTo(x + w * 0.55f, y + h * (behind ? 0.16f : 0.24f) + wingBeat * h * (mode == 2 ? 0.20f : 0.11f), x + w * 0.90f, y + h * 0.31f + wingBeat * h * (mode == 2 ? 0.16f : 0.07f));
                wing.quadTo(x + w * 0.65f, y + h * 0.39f + counter * h * 0.08f, bodyX, bodyY);
                canvas.drawPath(wing, paint);

                Path tail = new Path();
                tail.moveTo(x + w * 0.68f, y + h * 0.62f);
                tail.quadTo(x + w * 0.88f, y + h * (0.68f + step * 0.05f), x + w * 1.03f, y + h * (0.56f + counter * 0.07f));
                canvas.drawPath(tail, paint);

                canvas.drawLine(x + w * 0.42f, y + h * 0.70f, x + w * (0.34f + legSwing * 0.07f), y + h * (mode == 2 ? 0.84f : 0.92f), paint);
                canvas.drawLine(x + w * 0.54f, y + h * 0.70f, x + w * (0.60f - legSwing * 0.07f), y + h * (mode == 2 ? 0.85f : 0.93f), paint);
                canvas.drawLine(x + w * 0.48f, y + h * 0.64f, x + w * (0.44f - counter * 0.05f), y + h * (mode == 2 ? 0.80f : 0.86f), paint);

                if (!behind) {
                    paint.setStrokeWidth(Math.max(1f, w * 0.025f));
                    canvas.drawLine(headX, headY, headX - w * 0.10f, headY - h * (0.10f + Math.abs(step) * 0.035f), paint);
                    canvas.drawLine(headX + w * 0.04f, headY, headX + w * 0.11f, headY - h * (0.09f + Math.abs(counter) * 0.035f), paint);
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeCap(Paint.Cap.BUTT);
                paint.setStrokeJoin(Paint.Join.MITER);
            }

            private void drawAnimatedPetBitmap(Canvas canvas, Bitmap bitmap, float x, float y, float w, float h, float gait, int mode) {
                if (bitmap == null) return;
                int slices = 9;
                float walkWave = (float) Math.sin(gait * Math.PI * 2f);
                float flyWave = (float) Math.sin(gait * Math.PI * 8f);
                float breathe = (float) Math.sin(gait * Math.PI * 2f + 0.7f);
                paint.setAlpha(244);
                paint.setFilterBitmap(true);
                paint.setDither(true);

                for (int i = 0; i < slices; i++) {
                    int sx0 = Math.round(bitmap.getWidth() * i / (float) slices);
                    int sx1 = Math.round(bitmap.getWidth() * (i + 1) / (float) slices);
                    float dx0 = x + w * i / slices;
                    float dx1 = x + w * (i + 1) / slices;
                    float center = (i + 0.5f) / slices;
                    float spine = (float) Math.sin(gait * Math.PI * 2f + center * Math.PI * 1.7f);
                    float flight = mode == 2 ? flyWave * h * (0.020f + center * 0.018f) : 0f;
                    float walk = mode == 0 ? spine * h * 0.018f : mode == 1 ? spine * h * 0.012f : 0f;
                    float climb = mode == 1 ? walkWave * w * 0.010f : 0f;
                    float dx = climb + (mode == 2 ? spine * w * 0.014f : spine * w * 0.006f);
                    float dy = flight + walk;
                    float stretch = Math.abs(breathe) * h * 0.010f * (1f - Math.abs(center - 0.5f));
                    Rect src = new Rect(sx0, 0, sx1, bitmap.getHeight());
                    RectF dst = new RectF(dx0 + dx, y + dy - stretch, dx1 + dx, y + h + dy + stretch);
                    canvas.drawBitmap(bitmap, src, dst, paint);
                }
                paint.setAlpha(255);
            }
        };
    }


    private void showNexyPage() {
        clearPage(PAGE_NEXY, "Nexi", "");
        LinearLayout p = activePanel();

        p.addView(section("BRIDGE"));
        endpointInput = input("http://100.107.24.67:8765", true);
        endpointInput.setText(nexyBridgeBase());
        p.addView(endpointInput, card(8));

        TextView out = logBox("Nexi bereit.\nBridge: " + nexyBridgeBase());
        p.addView(out, card(8));

        row(p,
                nav("Lokal 127", v -> useNexyBridge(out, "http://127.0.0.1:8765", "Lokal-Termux")),
                nav("LAN 192", v -> useNexyBridge(out, "http://192.168.1.216:8765", "PC/LAN"))
        );

        row(p,
                nav("Tailscale 100", v -> useNexyBridge(out, "http://100.107.24.67:8765", "Tailscale")),
                nav("Status", v -> loadNexyEndpoint(out, "Nexi Status", "/api/nexy/status"))
        );

        row(p,
                nav("Bridge speichern", v -> { setNexyBridgeBase(endpointInput.getText().toString()); out.setText("Nexi Bridge gespeichert:\n" + nexyBridgeBase()); }),
                nav("Briefing", v -> loadNexyEndpoint(out, "Nexi Briefing", "/api/nexy/briefing"))
        );

        row(p,
                nav("Fokus", v -> loadNexyEndpoint(out, "Nexi Fokus", "/api/nexy/focus?limit=5")),
                nav("Timeline", v -> loadNexyEndpoint(out, "Nexi Timeline", "/api/nexy/timeline?limit=8"))
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
                nav("Memory", v -> loadNexyEndpoint(out, "Nexi Memory", "/api/nexy/status"))
        );
    }

    private String nexyBridgeBase() {
        return NexusConfig.prefs(this).getString("nexy_bridge_url", "http://100.107.24.67:8765");
    }

    private void setNexyBridgeBase(String value) {
        String v = value == null ? "" : value.trim();
        if (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        if (v.isEmpty()) v = "http://100.107.24.67:8765";
        NexusConfig.prefs(this).edit().putString("nexy_bridge_url", v).apply();
    }

    private void useNexyBridge(TextView out, String url, String label) {
        setNexyBridgeBase(url);
        if (endpointInput != null) endpointInput.setText(nexyBridgeBase());
        if (out != null) {
            out.setText("[OK] Nexi Bridge gesetzt\n"
                    + "Profil: " + label + "\n"
                    + "URL: " + nexyBridgeBase() + "\n\n"
                    + "Jetzt Status, Briefing, Fokus oder Suche druecken.");
        }
    }

    private String nexyRender(String title, String body, String base) {
        try {
            JSONObject root = new JSONObject(body);
            StringBuilder sb = new StringBuilder();

            sb.append("[OK] ").append(title).append("\n");
            sb.append("Quelle: ").append(host(base)).append("\n");

            if (!root.optBoolean("ok", true)) {
                sb.append("\n[FEHLER] Status: Fehler\n");
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

            if (items != null) appendNexyItems(sb, "Eintraege", items, 8);

            if (counts != null || items != null) return sb.toString().trim();

            return sb.append("\n").append(cutKeepLines(body, 3000)).toString().trim();
        } catch (Exception ex) {
            return title + "\nQuelle: " + host(base) + "\n\n" + cutKeepLines(body, 3000);
        }
    }

    private void appendNexyCounts(StringBuilder sb, JSONObject c) {
        sb.append("\nSpeicher\n");
        sb.append("Events: ").append(c.optInt("nexy_events", 0))
                .append(" | Timeline: ").append(c.optInt("nexy_timeline", 0)).append("\n");
        sb.append("Context: ").append(c.optInt("nexy_context", 0))
                .append(" | Facts: ").append(c.optInt("nexy_facts", 0)).append("\n");
        sb.append("Lessons: ").append(c.optInt("nexy_lessons", 0))
                .append(" | Fokus: ").append(c.optInt("nexy_active_focus", 0)).append("\n");
    }

    private void appendNexyFocus(StringBuilder sb, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\nAktiver Fokus\n");
        int max = Math.min(arr.length(), 6);
        for (int i = 0; i < max; i++) {
            JSONObject x = arr.optJSONObject(i);
            if (x == null) continue;
            sb.append(i + 1).append(". ").append(x.optString("focus_name", x.optString("name", "Fokus"))).append("\n");
            String next = x.optString("next_action", "");
            if (!next.isEmpty()) sb.append("   -> ").append(cut(next, 150)).append("\n");
            String desc = x.optString("description", "");
            if (!desc.isEmpty()) sb.append("   ").append(cut(desc, 180)).append("\n");
        }
    }

    private void appendNexyLessons(StringBuilder sb, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        sb.append("\nLessons\n");
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
        sb.append("\nFacts\n");
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
        sb.append("\nTimeline\n");
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
        sb.append("\n").append(label).append("\n");
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
            loadNexyEndpoint(out, "Nexi Suche: " + q, "/api/nexy/search?q=" + enc(q) + "&limit=5");
        } catch (Exception ex) {
            out.setText("Nexi Suche konnte nicht vorbereitet werden: " + ex.getClass().getSimpleName());
        }
    }

    private void loadNexyEndpoint(TextView out, String title, String path) {
        out.setText("Lade " + title + "...\n" + nexyBridgeBase() + path);
        new Thread(() -> {
            StringBuilder failures = new StringBuilder();
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    String body = httpGet(base + path);
                    setNexyBridgeBase(base);
                    String text = nexyRender(title, body, base);
                    runOnUiThread(() -> { if (PAGE_NEXY.equals(currentPage)) out.setText(text); });
                    return;
                } catch (Exception ex) {
                    failures.append(host(base)).append(": ").append(ex.getClass().getSimpleName()).append(" ").append(cut(ex.getMessage(), 70)).append('\n');
                }
            }
            String err = "Nexi nicht erreichbar.\n" + failures.toString().trim()
                    + "\n\nPC pruefen: Nexi Bridge muss auf 0.0.0.0:8765 laufen.";
            runOnUiThread(() -> { if (PAGE_NEXY.equals(currentPage)) out.setText(err); });
        }).start();
    }



    private void showDigiPadPage() {
        clearPage(PAGE_DIGIPAD, "DigiPad", "Geschuetzter Remote-Client fuer das Fiona-Profil ueber die DigiPad Container API.");
        LinearLayout p = activePanel();

        p.addView(section("VERBINDUNG"));
        endpointInput = input("http://192.168.x.x:8788 oder Tailscale:8788", true);
        endpointInput.setText(digipadBase());
        p.addView(endpointInput, card(8));

        digipadTokenInput = input("DigiPad Token", true);
        digipadTokenInput.setText(digipadToken());
        p.addView(digipadTokenInput, card(8));

        TextView out = logBox("DigiPad bereit.\nProfil: fiona\nAPI: " + digipadBase()
                + "\nToken wird lokal auf diesem Geraet gespeichert.\nKeine Nexi-, Collector-, Nachrichten- oder Datei-Endpunkte.");
        p.addView(out, card(8));

        row(p,
                nav("Speichern", v -> saveDigiPadSettings(out)),
                nav("Status", v -> loadDigiPadEndpoint(out, "Fiona Status", "/api/pad/fiona/status"))
        );

        row(p,
                nav("Fuettern", v -> postDigiPadAction(out, "Fuettern", "/api/pad/fiona/feed", "{}")),
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
        p.addView(label("Fionas Handy nutzt spaeter die WLAN- oder Tailscale-Adresse von Patricks Host auf Port 8788. 127.0.0.1 funktioniert nur auf dem Geraet, auf dem Termux laeuft.", 12, false, Color.rgb(220, 214, 206)));
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
                + "\n\nJetzt Status druecken.");
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
                        + "Pruefen: Port 8788, WLAN/Tailscale, Token.";
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
            sb.append("[OK] ").append(title).append("\n");
            sb.append("Quelle: ").append(host(base)).append("\n\n");

            if (!root.optBoolean("ok", true)) {
                sb.append("\n[FEHLER] Status: Fehler\n");
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
        clearPage(PAGE_STATUS, "Status", "Aktueller App- und Collector-Zustand.");
        activePanel().addView(label(status(), 13, false, Color.rgb(230, 225, 216)));
    }

    private void showDragonPage() {
        clearPage(PAGE_DRAGON, "Digi Dragon", "");
        LinearLayout p = activePanel();
        String mode = dragonMode();

        TextView out = logBox(dragonSummary());

        p.addView(section("MODUS"));
        row(p,
                nav(dragonModeLabel("home", "Zuhause", mode), v -> { setDragonMode("home"); showDragonPage(); }),
                nav(dragonModeLabel("evolution", "Entwicklung", mode), v -> { setDragonMode("evolution"); showDragonPage(); })
        );
        row(p,
                nav(dragonModeLabel("battle", "Kampf", mode), v -> { setDragonMode("battle"); showDragonPage(); }),
                nav(dragonModeLabel("system", "System", mode), v -> { setDragonMode("system"); showDragonPage(); })
        );

        if ("evolution".equals(mode)) {
            p.addView(section("EVOLUTION"));
            View visual = dragonVisualView(out, true);
            p.addView(visual, card(8));
            row(p,
                    nav("Evolution pruefen", v -> { dragonEvolve(out); visual.invalidate(); }),
                    nav("Codex", v -> { dragonCodex(out); visual.invalidate(); })
            );
            p.addView(out, card(8));
            return;
        }

        if ("battle".equals(mode)) {
            p.addView(section("TRAINING UND KAMPF"));
            View visual = dragonVisualView(out, false);
            p.addView(visual, card(8));
            row(p,
                    nav("Kraft", v -> { setDragonScene("training"); dragonTrain(out, "strength"); visual.invalidate(); }),
                    nav("Ausdauer", v -> { setDragonScene("training"); dragonTrain(out, "endurance"); visual.invalidate(); })
            );
            row(p,
                    nav("Flug", v -> { setDragonScene("training"); dragonTrain(out, "speed"); visual.invalidate(); }),
                    nav("Fokus", v -> { setDragonScene("training"); dragonTrain(out, "focus"); visual.invalidate(); })
            );
            row(p,
                    nav("Instinkt", v -> { setDragonScene("training"); dragonTrain(out, "instinct"); visual.invalidate(); }),
                    nav("Arena starten", v -> { setDragonScene("arena"); dragonArena(out); visual.invalidate(); })
            );
            row(p,
                    nav("Freikampf starten", v -> { setDragonScene("arena"); dragonFreeFight(out); visual.invalidate(); }),
                    nav("Pflege", v -> { dragonCare(out); visual.invalidate(); })
            );
            p.addView(section("ATTACKEN"));
            p.addView(label(dragonBattleLine(), 12, true, Color.rgb(238, 232, 224)));
            DragonMove[] moves = dragonMoves();
            row(p,
                    nav(moves[0].name, v -> { dragonUseMove(out, 0); visual.invalidate(); }),
                    nav(moves[1].name, v -> { dragonUseMove(out, 1); visual.invalidate(); })
            );
            row(p,
                    nav(moves[2].name, v -> { dragonUseMove(out, 2); visual.invalidate(); }),
                    nav(moves[3].name, v -> { dragonUseMove(out, 3); visual.invalidate(); })
            );
            row(p, nav("Rueckzug", v -> { dragonRetreat(out); visual.invalidate(); }));
            p.addView(out, card(8));
            return;
        }

        if ("system".equals(mode)) {
            p.addView(section("SYSTEM"));
            endpointInput = input("http://127.0.0.1:8777", true);
            endpointInput.setText(dragonBridgeBase());
            p.addView(endpointInput, card(8));
            row(p,
                    nav("Bridge speichern", v -> {
                        setDragonBridgeBase(endpointInput.getText().toString());
                        out.setText("Digi-Dragon-Bridge gespeichert:\n" + dragonBridgeBase());
                    }),
                    nav("Bridge Status", v -> loadDragonEndpoint(out, "Digi Dragon Status", "/api/dragon/status"))
            );
            row(p, nav("Nexi bewusst fragen", v -> dragonAskNexi()));
            p.addView(out, card(8));
            return;
        }

        p.addView(section("ZUHAUSE"));
        View visual = dragonVisualView(out, false);
        p.addView(visual, card(8));
        p.addView(label(dragonHomeLine(), 13, true, Color.rgb(238, 232, 224)));
        row(p,
                nav("Pflege", v -> { dragonCare(out); visual.invalidate(); }),
                nav("Ruhig", v -> { dragonCalm(out); visual.invalidate(); })
        );
        row(p,
                nav("Training", v -> { setDragonScene("training"); setDragonMode("battle"); showDragonPage(); }),
                nav("Entwicklung", v -> { setDragonMode("evolution"); showDragonPage(); })
        );
        p.addView(out, card(8));
    }

    private View dragonVisualView(TextView out) {
        return dragonVisualView(out, true);
    }

    private View dragonVisualView(TextView out, boolean showEvolutionBoard) {
        View view = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private float touchX = -1f;
            private float touchY = -1f;
            private long pulseUntil = 0L;
            private Bitmap dragonBitmap;
            private int dragonBitmapRes = 0;
            private Bitmap environmentBitmap;
            private int environmentBitmapRes = 0;

            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                int pad = dp(14);
                int accent = accentText();

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(9, 10, 10));
                canvas.drawRoundRect(new RectF(0, 0, w, h), dp(14), dp(14), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(accentDark());
                canvas.drawRoundRect(new RectF(dp(1), dp(1), w - dp(1), h - dp(1)), dp(14), dp(14), paint);

                if (System.currentTimeMillis() < pulseUntil && touchX >= 0f && touchY >= 0f) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(3));
                    paint.setColor(accent);
                    canvas.drawCircle(touchX, touchY, dp(20), paint);
                }

                int xp = dragonInt("xp", 0);
                int energy = dragonInt("energy", 80);
                int stress = dragonInt("stress", 15);
                int wins = dragonInt("wins", 0);
                int level = dragonLevel();
                int sceneH = showEvolutionBoard ? dp(220) : Math.max(dp(360), h - dp(8));

                String environment = dragonVisualEnvironment(showEvolutionBoard);
                if (!drawEnvironmentAsset(canvas, w, sceneH, environment)) {
                    drawHabitatScene(canvas, w, sceneH, dragonElement(), dragonHabitat());
                }
                if (!drawDragonAsset(canvas, w, sceneH, accent)) {
                    drawDragon(canvas, w, sceneH, accent, energy, stress);
                }

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(dp(18));
                canvas.drawText(dragonStage(), pad, dp(30), paint);

                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(dp(11));
                paint.setColor(sub());
                canvas.drawText("Level " + level + "  |  XP " + xp + "  |  Siege " + wins, pad, dp(49), paint);

                int barX = pad;
                int barW = Math.max(dp(120), w - pad * 2);
                drawBar(canvas, "XP", xp % 100, 100, barX, dp(82), barW, accent);
                drawBar(canvas, "Energie", energy, 100, barX, dp(108), barW, Color.rgb(78, 214, 129));
                drawBar(canvas, "Stress", stress, 100, barX, dp(134), barW, Color.rgb(255, 92, 70));
                drawBar(canvas, "Stimmung", dragonInt("mood", 60), 100, barX, dp(160), barW, Color.rgb(255, 196, 79));
                drawBar(canvas, "Bindung", dragonInt("bond", 20), 100, barX, dp(186), barW, Color.rgb(156, 119, 255));

                if (showEvolutionBoard) {
                    drawEvolutionBoard(canvas, pad, dp(220), w - pad * 2, h - dp(234), dragonElement());
                }
            }

            @Override public boolean onTouchEvent(MotionEvent event) {
                if (event == null) return false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchX = event.getX();
                    touchY = event.getY();
                    pulseUntil = System.currentTimeMillis() + 220L;
                    invalidate();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    touchX = event.getX();
                    touchY = event.getY();
                    pulseUntil = System.currentTimeMillis() + 360L;
                    applyDragonTouch(touchX, touchY, out);
                    invalidate();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    touchX = -1f;
                    touchY = -1f;
                    invalidate();
                    return true;
                }
                return true;
            }

            private void applyDragonTouch(float x, float y, TextView out) {
                int h = getHeight();
                String note;
                if (showEvolutionBoard && y >= dp(220)) {
                    String selected = selectEvolutionElement(x, y, getWidth(), getHeight());
                    setDragonElement(selected);
                    note = "Evolution: " + selected + "-Pfad markiert.";
                } else if (y >= dp(72) && y <= dp(152)) {
                    if (y < dp(100)) {
                        addDragonXp(2);
                        note = "Visual: XP-Fokus +2.";
                    } else if (y < dp(126)) {
                        setDragonInt("energy", clamp(dragonInt("energy", 80) + 8, 0, 100));
                        note = "Visual: Energie +8.";
                    } else {
                        setDragonInt("stress", clamp(dragonInt("stress", 15) - 8, 0, 100));
                        note = "Visual: Stress -8.";
                    }
                } else {
                    addDragonXp(5);
                    setDragonInt("energy", clamp(dragonInt("energy", 80) - 3, 0, 100));
                    setDragonInt("stress", clamp(dragonInt("stress", 15) - 4, 0, 100));
                    note = "Visual: Dragon reagiert. +5 XP, Energie -3, Stress -4.";
                }
                if (out != null) out.setText(dragonSummary() + "\n\n" + note);
            }

            private boolean drawDragonAsset(Canvas canvas, int w, int h, int accent) {
                int resId = dragonImageResource();
                if (resId == 0) return false;
                if (dragonBitmap == null || dragonBitmapRes != resId) {
                    dragonBitmap = transparentImageBackground(BitmapFactory.decodeResource(getResources(), resId));
                    dragonBitmapRes = resId;
                }
                if (dragonBitmap == null) return false;

                float maxW = w * 0.70f;
                float maxH = h * 0.68f;
                float scale = Math.min(maxW / dragonBitmap.getWidth(), maxH / dragonBitmap.getHeight());
                float bw = dragonBitmap.getWidth() * scale;
                float bh = dragonBitmap.getHeight() * scale;
                float left = w * 0.28f;
                float top = dp(38) + (maxH - bh) * 0.25f;
                RectF dst = new RectF(left, top, left + bw, top + bh);

                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new RadialGradient(dst.centerX(), dst.centerY(), Math.max(dst.width(), dst.height()) * 0.72f,
                        Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent)),
                        Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)),
                        Shader.TileMode.CLAMP));
                canvas.drawCircle(dst.centerX(), dst.centerY(), Math.max(dst.width(), dst.height()) * 0.72f, paint);
                paint.setShader(null);

                paint.setColor(Color.argb(135, 0, 0, 0));
                canvas.drawOval(new RectF(dst.left + dp(18), dst.bottom - dp(18), dst.right - dp(12), dst.bottom + dp(8)), paint);
                canvas.drawBitmap(dragonBitmap, null, dst, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(Color.argb(150, 255, 255, 255));
                canvas.drawArc(new RectF(dst.left + dst.width() * 0.08f, dst.top + dst.height() * 0.05f, dst.left + dst.width() * 0.45f, dst.top + dst.height() * 0.42f), 210, 80, false, paint);
                return true;
            }

            private boolean drawEnvironmentAsset(Canvas canvas, int w, int h, String environment) {
                int resId = dragonEnvironmentResource(environment);
                if (resId == 0) return false;
                if (environmentBitmap == null || environmentBitmapRes != resId) {
                    environmentBitmap = BitmapFactory.decodeResource(getResources(), resId);
                    environmentBitmapRes = resId;
                }
                if (environmentBitmap == null) return false;

                RectF dst = new RectF(dp(2), dp(2), w - dp(2), h - dp(2));
                float srcW = environmentBitmap.getWidth();
                float srcH = environmentBitmap.getHeight();
                float dstRatio = dst.width() / Math.max(1f, dst.height());
                float srcRatio = srcW / Math.max(1f, srcH);
                Rect src;
                if (srcRatio > dstRatio) {
                    float cropW = srcH * dstRatio;
                    float left = (srcW - cropW) / 2f;
                    src = new Rect(Math.round(left), 0, Math.round(left + cropW), Math.round(srcH));
                } else {
                    float cropH = srcW / dstRatio;
                    float top = (srcH - cropH) / 2f;
                    src = new Rect(0, Math.round(top), Math.round(srcW), Math.round(top + cropH));
                }

                canvas.save();
                Path clip = new Path();
                clip.addRoundRect(dst, dp(13), dp(13), Path.Direction.CW);
                canvas.clipPath(clip);
                paint.setAlpha(255);
                canvas.drawBitmap(environmentBitmap, src, dst, paint);
                paint.setShader(new LinearGradient(0, 0, 0, h,
                        new int[]{
                                Color.argb(92, 0, 0, 0),
                                Color.argb(10, 0, 0, 0),
                                Color.argb(150, 0, 0, 0)
                        },
                        new float[]{0f, 0.48f, 1f},
                        Shader.TileMode.CLAMP));
                canvas.drawRect(dst, paint);
                paint.setShader(null);
                canvas.restore();

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(Color.argb(120, Color.red(accentText()), Color.green(accentText()), Color.blue(accentText())));
                canvas.drawRoundRect(dst, dp(13), dp(13), paint);
                paint.setStyle(Paint.Style.FILL);
                return true;
            }

            private void drawDragon(Canvas canvas, int w, int h, int accent, int energy, int stress) {
                float cx = w * 0.58f;
                float cy = h * 0.47f;
                float scale = Math.max(0.75f, Math.min(1.15f, w / 360f));
                int element = elementColor(dragonElement());
                int body = energy > 35 ? element : shade(element, 0.45f);
                int wing = stress > 65 ? Color.rgb(180, 68, 58) : shade(element, 0.64f);
                int darkBody = shade(body, 0.45f);
                int lightBody = shade(body, 1.35f);

                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new RadialGradient(cx, cy, dp(112) * scale,
                        Color.argb(92, Color.red(wing), Color.green(wing), Color.blue(wing)),
                        Color.argb(0, Color.red(wing), Color.green(wing), Color.blue(wing)),
                        Shader.TileMode.CLAMP));
                canvas.drawCircle(cx, cy, dp(112) * scale, paint);
                paint.setShader(null);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(120, 0, 0, 0));
                canvas.drawOval(new RectF(cx - dp(95) * scale, cy + dp(39) * scale, cx + dp(118) * scale, cy + dp(65) * scale), paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(cx - dp(58) * scale, cy - dp(70) * scale, cx - dp(22) * scale, cy + dp(18) * scale,
                        shade(wing, 1.28f), shade(wing, 0.36f), Shader.TileMode.CLAMP));
                Path leftWing = new Path();
                leftWing.moveTo(cx - dp(22) * scale, cy - dp(16) * scale);
                leftWing.lineTo(cx - dp(80) * scale, cy - dp(68) * scale);
                leftWing.lineTo(cx - dp(72) * scale, cy - dp(16) * scale);
                leftWing.lineTo(cx - dp(44) * scale, cy + dp(10) * scale);
                leftWing.close();
                canvas.drawPath(leftWing, paint);

                paint.setShader(new LinearGradient(cx + dp(3) * scale, cy - dp(82) * scale, cx + dp(50) * scale, cy + dp(20) * scale,
                        shade(wing, 1.35f), shade(wing, 0.38f), Shader.TileMode.CLAMP));
                Path rightWing = new Path();
                rightWing.moveTo(cx + dp(4) * scale, cy - dp(19) * scale);
                rightWing.lineTo(cx + dp(72) * scale, cy - dp(78) * scale);
                rightWing.lineTo(cx + dp(66) * scale, cy - dp(18) * scale);
                rightWing.lineTo(cx + dp(35) * scale, cy + dp(11) * scale);
                rightWing.close();
                canvas.drawPath(rightWing, paint);
                paint.setShader(null);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(4) * scale);
                paint.setColor(shade(wing, 0.28f));
                canvas.drawLine(cx - dp(22) * scale, cy - dp(16) * scale, cx - dp(80) * scale, cy - dp(68) * scale, paint);
                canvas.drawLine(cx + dp(4) * scale, cy - dp(19) * scale, cx + dp(72) * scale, cy - dp(78) * scale, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(cx - dp(60) * scale, cy - dp(28) * scale, cx + dp(54) * scale, cy + dp(38) * scale,
                        lightBody, darkBody, Shader.TileMode.CLAMP));

                Path tail = new Path();
                tail.moveTo(cx - dp(48) * scale, cy + dp(8) * scale);
                tail.cubicTo(cx - dp(105) * scale, cy + dp(34) * scale, cx - dp(116) * scale, cy - dp(28) * scale, cx - dp(72) * scale, cy - dp(12) * scale);
                tail.cubicTo(cx - dp(98) * scale, cy - dp(7) * scale, cx - dp(97) * scale, cy + dp(27) * scale, cx - dp(50) * scale, cy + dp(20) * scale);
                tail.close();
                canvas.drawPath(tail, paint);

                canvas.drawOval(new RectF(cx - dp(54) * scale, cy - dp(20) * scale, cx + dp(36) * scale, cy + dp(31) * scale), paint);

                Path neck = new Path();
                neck.moveTo(cx + dp(24) * scale, cy - dp(10) * scale);
                neck.cubicTo(cx + dp(48) * scale, cy - dp(38) * scale, cx + dp(69) * scale, cy - dp(42) * scale, cx + dp(83) * scale, cy - dp(32) * scale);
                neck.lineTo(cx + dp(72) * scale, cy - dp(17) * scale);
                neck.cubicTo(cx + dp(54) * scale, cy - dp(25) * scale, cx + dp(42) * scale, cy - dp(14) * scale, cx + dp(30) * scale, cy + dp(5) * scale);
                neck.close();
                canvas.drawPath(neck, paint);

                Path head = new Path();
                head.moveTo(cx + dp(76) * scale, cy - dp(43) * scale);
                head.lineTo(cx + dp(105) * scale, cy - dp(37) * scale);
                head.lineTo(cx + dp(115) * scale, cy - dp(26) * scale);
                head.lineTo(cx + dp(100) * scale, cy - dp(17) * scale);
                head.lineTo(cx + dp(77) * scale, cy - dp(18) * scale);
                head.lineTo(cx + dp(66) * scale, cy - dp(30) * scale);
                head.close();
                canvas.drawPath(head, paint);
                paint.setShader(null);

                paint.setColor(Color.rgb(228, 218, 184));
                Path hornA = new Path();
                hornA.moveTo(cx + dp(79) * scale, cy - dp(42) * scale);
                hornA.lineTo(cx + dp(67) * scale, cy - dp(65) * scale);
                hornA.lineTo(cx + dp(91) * scale, cy - dp(47) * scale);
                hornA.close();
                canvas.drawPath(hornA, paint);
                Path hornB = new Path();
                hornB.moveTo(cx + dp(95) * scale, cy - dp(40) * scale);
                hornB.lineTo(cx + dp(104) * scale, cy - dp(61) * scale);
                hornB.lineTo(cx + dp(105) * scale, cy - dp(36) * scale);
                hornB.close();
                canvas.drawPath(hornB, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2) * scale);
                paint.setColor(Color.argb(130, 255, 255, 255));
                canvas.drawArc(new RectF(cx - dp(41) * scale, cy - dp(15) * scale, cx + dp(26) * scale, cy + dp(20) * scale), 205, 86, false, paint);
                paint.setColor(Color.argb(100, 0, 0, 0));
                canvas.drawArc(new RectF(cx - dp(40) * scale, cy - dp(16) * scale, cx + dp(34) * scale, cy + dp(32) * scale), 18, 88, false, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(7) * scale);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(body);
                canvas.drawLine(cx - dp(21) * scale, cy + dp(24) * scale, cx - dp(31) * scale, cy + dp(52) * scale, paint);
                canvas.drawLine(cx + dp(17) * scale, cy + dp(23) * scale, cx + dp(35) * scale, cy + dp(51) * scale, paint);
                paint.setStrokeWidth(dp(2) * scale);
                paint.setColor(Color.rgb(230, 220, 188));
                canvas.drawLine(cx - dp(31) * scale, cy + dp(52) * scale, cx - dp(42) * scale, cy + dp(58) * scale, paint);
                canvas.drawLine(cx - dp(31) * scale, cy + dp(52) * scale, cx - dp(24) * scale, cy + dp(61) * scale, paint);
                canvas.drawLine(cx + dp(35) * scale, cy + dp(51) * scale, cx + dp(24) * scale, cy + dp(59) * scale, paint);
                canvas.drawLine(cx + dp(35) * scale, cy + dp(51) * scale, cx + dp(48) * scale, cy + dp(58) * scale, paint);

                paint.setStrokeCap(Paint.Cap.BUTT);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(8, 10, 8));
                canvas.drawCircle(cx + dp(95) * scale, cy - dp(33) * scale, dp(3) * scale, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2) * scale);
                paint.setColor(Color.rgb(20, 14, 10));
                canvas.drawLine(cx + dp(95) * scale, cy - dp(22) * scale, cx + dp(114) * scale, cy - dp(25) * scale, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(accent);
                Path flame = new Path();
                flame.moveTo(cx + dp(117) * scale, cy - dp(27) * scale);
                flame.lineTo(cx + dp(149) * scale, cy - dp(44) * scale);
                flame.lineTo(cx + dp(137) * scale, cy - dp(27) * scale);
                flame.lineTo(cx + dp(153) * scale, cy - dp(16) * scale);
                flame.lineTo(cx + dp(128) * scale, cy - dp(18) * scale);
                flame.close();
                canvas.drawPath(flame, paint);
            }

            private void drawHabitatScene(Canvas canvas, int w, int h, String element, String habitat) {
                int base = elementColor(element);
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(0, 0, 0, h,
                        shade(base, 0.22f), Color.rgb(3, 4, 5), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(new RectF(dp(2), dp(2), w - dp(2), h - dp(2)), dp(13), dp(13), paint);
                paint.setShader(null);

                paint.setColor(Color.argb(72, Color.red(base), Color.green(base), Color.blue(base)));
                canvas.drawCircle(w * 0.74f, dp(54), dp(82), paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(32, 31, 29));
                canvas.drawRect(dp(12), h - dp(42), w - dp(12), h - dp(20), paint);
                paint.setShader(new LinearGradient(0, h - dp(52), 0, h - dp(16),
                        Color.rgb(105, 96, 78), Color.rgb(28, 25, 22), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(new RectF(dp(28), h - dp(52), w - dp(28), h - dp(16)), dp(7), dp(7), paint);
                paint.setShader(null);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(Color.argb(115, 180, 176, 164));
                for (int i = 0; i < 4; i++) {
                    float x = dp(30) + i * (w - dp(60)) / 3f;
                    canvas.drawLine(x, dp(18), x, h - dp(46), paint);
                    canvas.drawArc(new RectF(x - dp(18), dp(12), x + dp(18), dp(55)), 200, 140, false, paint);
                }

                drawHabitatStage(canvas, w, h, habitat, base);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                if ("Wasser".equals(element)) {
                    paint.setColor(Color.argb(180, 68, 225, 220));
                    for (int i = 0; i < 4; i++) canvas.drawArc(new RectF(dp(20 + i * 64), h - dp(64), dp(78 + i * 64), h - dp(32)), 195, 130, false, paint);
                } else if ("Erde".equals(element)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(95, 116, 58));
                    for (int i = 0; i < 5; i++) canvas.drawOval(new RectF(dp(18 + i * 48), h - dp(78 - (i % 2) * 8), dp(64 + i * 48), h - dp(38)), paint);
                } else if ("Feuer".equals(element)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.argb(210, 255, 88, 25));
                    for (int i = 0; i < 5; i++) {
                        Path lava = new Path();
                        float x = dp(22 + i * 54);
                        lava.moveTo(x, h - dp(42));
                        lava.lineTo(x + dp(16), h - dp(78));
                        lava.lineTo(x + dp(34), h - dp(42));
                        lava.close();
                        canvas.drawPath(lava, paint);
                    }
                } else if ("Luft".equals(element)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.argb(170, 190, 232, 244));
                    for (int i = 0; i < 4; i++) canvas.drawOval(new RectF(dp(20 + i * 70), dp(70 + (i % 2) * 18), dp(100 + i * 70), dp(96 + (i % 2) * 18)), paint);
                } else {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.argb(170, 96, 62, 148));
                    canvas.drawCircle(w - dp(72), dp(42), dp(24), paint);
                    paint.setColor(Color.rgb(5, 5, 8));
                    canvas.drawCircle(w - dp(62), dp(36), dp(24), paint);
                }

                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(dp(10));
                paint.setColor(Color.rgb(218, 210, 194));
                canvas.drawText(habitat, dp(14), h - dp(12), paint);
            }

            private void drawHabitatStage(Canvas canvas, int w, int h, String habitat, int base) {
                paint.setStyle(Paint.Style.FILL);
                if ("Kleines Nest".equals(habitat)) {
                    paint.setColor(Color.rgb(86, 58, 33));
                    for (int i = 0; i < 9; i++) {
                        float x = dp(26 + i * 18);
                        canvas.drawOval(new RectF(x, h - dp(48 + (i % 3) * 3), x + dp(44), h - dp(30 - (i % 2) * 2)), paint);
                    }
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(Color.rgb(136, 96, 54));
                    for (int i = 0; i < 7; i++) canvas.drawLine(dp(32 + i * 22), h - dp(38), dp(55 + i * 22), h - dp(54), paint);
                    return;
                }

                if ("Hoehle".equals(habitat)) {
                    paint.setColor(Color.argb(185, 0, 0, 0));
                    Path cave = new Path();
                    cave.moveTo(dp(8), h - dp(30));
                    cave.cubicTo(dp(30), dp(12), w - dp(30), dp(12), w - dp(8), h - dp(30));
                    cave.lineTo(w - dp(8), dp(8));
                    cave.lineTo(dp(8), dp(8));
                    cave.close();
                    canvas.drawPath(cave, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(3));
                    paint.setColor(Color.rgb(86, 82, 76));
                    canvas.drawArc(new RectF(dp(24), dp(18), w - dp(24), h + dp(50)), 198, 144, false, paint);
                    return;
                }

                if ("Geheiligtes Lager".equals(habitat)) {
                    paint.setColor(Color.rgb(98, 92, 82));
                    for (int i = 0; i < 3; i++) {
                        float x = dp(34 + i * 64);
                        canvas.drawRoundRect(new RectF(x, h - dp(88), x + dp(18), h - dp(38)), dp(4), dp(4), paint);
                        paint.setColor(Color.argb(160, Color.red(base), Color.green(base), Color.blue(base)));
                        canvas.drawCircle(x + dp(9), h - dp(66), dp(14), paint);
                        paint.setColor(Color.rgb(98, 92, 82));
                    }
                    return;
                }

                if ("Drachenhort".equals(habitat)) {
                    paint.setColor(Color.rgb(204, 156, 45));
                    for (int i = 0; i < 24; i++) {
                        float x = dp(24 + (i * 17) % Math.max(60, w - dp(72)));
                        float y = h - dp(46 + (i % 5) * 4);
                        canvas.drawOval(new RectF(x, y, x + dp(13), y + dp(6)), paint);
                    }
                    paint.setColor(Color.rgb(98, 58, 28));
                    canvas.drawRoundRect(new RectF(w - dp(98), h - dp(76), w - dp(38), h - dp(42)), dp(5), dp(5), paint);
                    paint.setColor(Color.rgb(232, 183, 60));
                    canvas.drawRect(w - dp(76), h - dp(68), w - dp(62), h - dp(42), paint);
                    return;
                }

                if ("Schwebende Insel".equals(habitat)) {
                    paint.setColor(Color.argb(155, 210, 230, 235));
                    for (int i = 0; i < 4; i++) canvas.drawOval(new RectF(dp(18 + i * 72), h - dp(82 + (i % 2) * 9), dp(92 + i * 72), h - dp(58 + (i % 2) * 9)), paint);
                    paint.setColor(Color.rgb(84, 82, 72));
                    Path island = new Path();
                    island.moveTo(dp(74), h - dp(60));
                    island.lineTo(w - dp(78), h - dp(60));
                    island.lineTo(w - dp(112), h - dp(24));
                    island.lineTo(dp(108), h - dp(24));
                    island.close();
                    canvas.drawPath(island, paint);
                    return;
                }

                paint.setColor(Color.argb(130, Color.red(base), Color.green(base), Color.blue(base)));
                canvas.drawCircle(w / 2f, h - dp(64), dp(32), paint);
            }

            private void drawEvolutionBoard(Canvas canvas, int x, int y, int width, int height, String activeElement) {
                String[] elements = {"Wasser", "Erde", "Feuer", "Luft", "Schatten"};
                String[] forms = {"Ei", "Jung", "Adult", "Ahn"};
                int[] colors = {
                        Color.rgb(50, 190, 180),
                        Color.rgb(120, 142, 72),
                        Color.rgb(238, 96, 35),
                        Color.rgb(155, 199, 225),
                        Color.rgb(92, 64, 142)
                };
                int boardH = Math.max(dp(190), height);
                int headerH = dp(28);
                int labelW = dp(40);
                int colW = Math.max(dp(42), (width - labelW) / elements.length);
                int rowH = Math.max(dp(33), (boardH - headerH) / forms.length);

                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(x, y, x, y + boardH,
                        Color.rgb(47, 43, 37), Color.rgb(10, 10, 9), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(new RectF(x, y, x + width, y + boardH), dp(10), dp(10), paint);
                paint.setShader(null);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(80, 255, 230, 170));
                canvas.drawRoundRect(new RectF(x + dp(2), y + dp(2), x + width - dp(2), y + dp(32)), dp(9), dp(9), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(Color.rgb(92, 77, 61));
                canvas.drawRoundRect(new RectF(x, y, x + width, y + boardH), dp(10), dp(10), paint);

                paint.setStyle(Paint.Style.FILL);
                for (int c = 0; c < elements.length; c++) {
                    int cx = x + labelW + c * colW;
                    paint.setShader(new LinearGradient(cx, y, cx + colW, y,
                            Color.argb(58, 255, 255, 255), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP));
                    canvas.drawRect(cx + dp(2), y + headerH, cx + colW - dp(2), y + boardH - dp(2), paint);
                    paint.setShader(null);
                    paint.setColor(Color.argb(85, 0, 0, 0));
                    canvas.drawRect(cx + colW - dp(4), y + headerH, cx + colW - dp(2), y + boardH - dp(2), paint);
                }

                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(dp(9));
                for (int c = 0; c < elements.length; c++) {
                    int cx = x + labelW + c * colW;
                    boolean active = elements[c].equals(activeElement);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setShader(new LinearGradient(cx, y + dp(5), cx, y + headerH,
                            active ? Color.rgb(132, 90, 30) : Color.rgb(54, 48, 40),
                            active ? Color.rgb(36, 28, 18) : Color.rgb(20, 19, 17),
                            Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(new RectF(cx + dp(2), y + dp(5), cx + colW - dp(2), y + headerH - dp(3)), dp(7), dp(7), paint);
                    paint.setShader(null);
                    paint.setColor(active ? accentText() : Color.rgb(218, 208, 190));
                    canvas.drawText(elements[c], cx + dp(5), y + dp(21), paint);
                }

                for (int r = 0; r < forms.length; r++) {
                    int ry = y + headerH + r * rowH;
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    paint.setTextSize(dp(9));
                    paint.setColor(Color.rgb(214, 204, 186));
                    canvas.drawText(forms[r], x + dp(6), ry + rowH / 2 + dp(4), paint);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setShader(new LinearGradient(x, ry, x, ry + dp(12),
                            Color.rgb(102, 93, 79), Color.rgb(38, 34, 29), Shader.TileMode.CLAMP));
                    canvas.drawRect(x + labelW - dp(1), ry + rowH - dp(8), x + width - dp(4), ry + rowH - dp(2), paint);
                    paint.setShader(null);
                    paint.setColor(Color.argb(120, 0, 0, 0));
                    canvas.drawRect(x + labelW, ry + rowH - dp(2), x + width - dp(4), ry + rowH + dp(2), paint);

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1));
                    paint.setColor(Color.rgb(68, 61, 52));
                    canvas.drawLine(x + labelW, ry, x + width - dp(4), ry, paint);

                    for (int c = 0; c < elements.length; c++) {
                        int cx = x + labelW + c * colW;
                        boolean active = elements[c].equals(activeElement);
                        drawEvolutionFigure(canvas, cx + colW / 2, ry + rowH / 2, r, colors[c], elements[c], active);
                    }
                }
            }

            private void drawEvolutionFigure(Canvas canvas, int cx, int cy, int form, int color, String element, boolean active) {
                float s = Math.max(0.55f, Math.min(1.05f, getWidth() / 420f));
                int glow = active ? accentText() : Color.rgb(74, 66, 56);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(110, 0, 0, 0));
                canvas.drawOval(new RectF(cx - dp(22) * s, cy + dp(12) * s, cx + dp(24) * s, cy + dp(23) * s), paint);
                paint.setShader(new LinearGradient(cx, cy + dp(10) * s, cx, cy + dp(19) * s,
                        Color.rgb(118, 106, 88), Color.rgb(44, 39, 33), Shader.TileMode.CLAMP));
                canvas.drawRoundRect(new RectF(cx - dp(17) * s, cy + dp(12) * s, cx + dp(17) * s, cy + dp(17) * s), dp(3), dp(3), paint);
                paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(active ? dp(2) : dp(1));
                paint.setColor(glow);
                canvas.drawRoundRect(new RectF(cx - dp(18) * s, cy + dp(11) * s, cx + dp(18) * s, cy + dp(18) * s), dp(4), dp(4), paint);

                paint.setStyle(Paint.Style.FILL);
                if (active) {
                    paint.setShader(new RadialGradient(cx, cy, dp(34) * s,
                            Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)),
                            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                            Shader.TileMode.CLAMP));
                    canvas.drawCircle(cx, cy, dp(34) * s, paint);
                    paint.setShader(null);
                }
                int dark = shade(color, 0.52f);
                int light = shade(color, 1.32f);
                paint.setShader(new LinearGradient(cx - dp(14) * s, cy - dp(16) * s, cx + dp(16) * s, cy + dp(16) * s,
                        light, dark, Shader.TileMode.CLAMP));
                if (form == 0) {
                    canvas.drawOval(new RectF(cx - dp(10) * s, cy - dp(15) * s, cx + dp(10) * s, cy + dp(10) * s), paint);
                    paint.setShader(null);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.argb(90, 255, 255, 255));
                    canvas.drawOval(new RectF(cx - dp(6) * s, cy - dp(11) * s, cx + dp(1) * s, cy - dp(1) * s), paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1));
                    paint.setColor(Color.argb(150, 255, 255, 255));
                    canvas.drawLine(cx - dp(5) * s, cy - dp(8) * s, cx + dp(6) * s, cy + dp(3) * s, paint);
                    return;
                }

                float bodyW = dp(12 + form * 5) * s;
                float bodyH = dp(9 + form * 3) * s;
                canvas.drawOval(new RectF(cx - bodyW, cy - bodyH, cx + bodyW, cy + bodyH), paint);
                canvas.drawCircle(cx + dp(12 + form * 2) * s, cy - dp(6 + form) * s, dp(5 + form * 2) * s, paint);

                paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2) * s);
                paint.setColor(shade(color, 1.25f));
                canvas.drawLine(cx + dp(12 + form * 2) * s, cy - dp(14 + form * 2) * s,
                        cx + dp(18 + form * 4) * s, cy - dp(23 + form * 4) * s, paint);
                canvas.drawLine(cx + dp(10 + form * 2) * s, cy - dp(13 + form * 2) * s,
                        cx + dp(8 + form * 2) * s, cy - dp(23 + form * 4) * s, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(cx - dp(14) * s, cy - dp(16) * s, cx + dp(16) * s, cy + dp(16) * s,
                        light, dark, Shader.TileMode.CLAMP));

                Path leftWing = new Path();
                leftWing.moveTo(cx - dp(6) * s, cy - dp(6) * s);
                leftWing.lineTo(cx - dp(18 + form * 7) * s, cy - dp(18 + form * 5) * s);
                leftWing.lineTo(cx - dp(14 + form * 5) * s, cy + dp(4 + form * 2) * s);
                leftWing.close();
                canvas.drawPath(leftWing, paint);

                if (form >= 2) {
                    Path rightWing = new Path();
                    rightWing.moveTo(cx + dp(2) * s, cy - dp(7) * s);
                    rightWing.lineTo(cx + dp(21 + form * 8) * s, cy - dp(18 + form * 6) * s);
                    rightWing.lineTo(cx + dp(14 + form * 5) * s, cy + dp(5 + form * 2) * s);
                    rightWing.close();
                    canvas.drawPath(rightWing, paint);
                }
                paint.setShader(null);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(Color.argb(150, 255, 255, 255));
                canvas.drawArc(new RectF(cx - bodyW + dp(2) * s, cy - bodyH + dp(2) * s, cx + bodyW - dp(3) * s, cy + bodyH), 205, 85, false, paint);
                paint.setColor(Color.argb(130, 0, 0, 0));
                canvas.drawArc(new RectF(cx - bodyW, cy - bodyH, cx + bodyW, cy + bodyH + dp(2) * s), 20, 80, false, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3) * s);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(color);
                if ("Wasser".equals(element) && form >= 3) {
                    canvas.drawArc(new RectF(cx - dp(40) * s, cy - dp(4) * s, cx + dp(6) * s, cy + dp(38) * s), 105, 230, false, paint);
                } else {
                    canvas.drawLine(cx - bodyW, cy + dp(2) * s, cx - dp(20 + form * 5) * s, cy + dp(9 + form * 4) * s, paint);
                }
                paint.setStrokeCap(Paint.Cap.BUTT);
            }

            private int elementColor(String element) {
                if ("Wasser".equals(element)) return Color.rgb(42, 205, 198);
                if ("Erde".equals(element)) return Color.rgb(129, 154, 78);
                if ("Luft".equals(element)) return Color.rgb(178, 229, 240);
                if ("Schatten".equals(element)) return Color.rgb(105, 70, 168);
                return Color.rgb(238, 100, 42);
            }

            private int shade(int color, float factor) {
                int r = clamp((int) (Color.red(color) * factor), 0, 255);
                int g = clamp((int) (Color.green(color) * factor), 0, 255);
                int b = clamp((int) (Color.blue(color) * factor), 0, 255);
                return Color.rgb(r, g, b);
            }

            private String selectEvolutionElement(float x, float y, int w, int h) {
                String[] elements = {"Wasser", "Erde", "Feuer", "Luft", "Schatten"};
                int pad = dp(14);
                int labelW = dp(40);
                int boardX = pad;
                int width = w - pad * 2;
                int colW = Math.max(dp(42), (width - labelW) / elements.length);
                int idx = (int) ((x - boardX - labelW) / Math.max(1, colW));
                return elements[clamp(idx, 0, elements.length - 1)];
            }

            private void drawBar(Canvas canvas, String label, int value, int max, int x, int y, int width, int color) {
                int labelW = dp(62);
                int barH = dp(10);
                int v = clamp(value, 0, max);
                float pct = max <= 0 ? 0f : (float) v / (float) max;

                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(dp(10));
                paint.setColor(Color.rgb(218, 213, 205));
                canvas.drawText(label, x, y + dp(10), paint);

                int bx = x + labelW;
                int bw = Math.max(dp(60), width - labelW - dp(42));
                paint.setColor(Color.rgb(25, 27, 27));
                canvas.drawRoundRect(new RectF(bx, y, bx + bw, y + barH), dp(6), dp(6), paint);
                paint.setColor(color);
                canvas.drawRoundRect(new RectF(bx, y, bx + (bw * pct), y + barH), dp(6), dp(6), paint);

                paint.setTypeface(Typeface.DEFAULT);
                paint.setColor(sub());
                canvas.drawText(String.valueOf(v), bx + bw + dp(8), y + dp(10), paint);
            }
        };
                int visualHeight = showEvolutionBoard ? dp(438) : dp(430);
        view.setMinimumHeight(visualHeight);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, visualHeight));
        return view;
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
                runOnUiThread(() -> { if (PAGE_DRAGON.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "Digi-Dragon-Bridge nicht erreichbar.\n"
                        + "Bridge: " + base + "\n"
                        + "Fehler: " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 160) + "\n\n"
                        + "Hinweis: Digi Dragon laeuft lokal in der App. Die externe Bridge auf 8777 ist optional.";
                runOnUiThread(() -> { if (PAGE_DRAGON.equals(currentPage)) out.setText(err); });
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
                runOnUiThread(() -> { if (PAGE_DRAGON.equals(currentPage)) out.setText(text); });
            } catch (Exception ex) {
                String err = "Digi-Dragon-Aktion fehlgeschlagen.\n"
                        + "Aktion: " + title + "\n"
                        + "Bridge: " + base + "\n"
                        + "Fehler: " + ex.getClass().getSimpleName() + " " + cut(ex.getMessage(), 160);
                runOnUiThread(() -> { if (PAGE_DRAGON.equals(currentPage)) out.setText(err); });
            }
        }).start();
    }

    private String dragonRender(String title, String body, String base) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject state = root.optJSONObject("state");
            if (state == null) state = root;

            StringBuilder sb = new StringBuilder();
            sb.append("[OK] ").append(title).append("\n");
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
            sb.append("Staerke ").append(state.optInt("strength", 0))
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

    private void dragonTrain(TextView out) {
        dragonTrain(out, "focus");
    }

    private void dragonTrain(TextView out, String type) {
        int energy = dragonInt("energy", 80);
        int stress = dragonInt("stress", 15);
        if (energy < 12) {
            dragonActionOut(out, "Training blockiert: Energie unter 12. Erst Ruhig oder Pflege nutzen.");
            return;
        }
        if (stress > 92) {
            dragonActionOut(out, "Training blockiert: Stress ueber 92. Der Drache braucht Ruhe.");
            return;
        }

        String key = type == null ? "focus" : type;
        int xpGain = 11;
        int energyCost = 12;
        int stressGain = 7;
        String result;
        if ("strength".equals(key)) {
            setDragonInt("strength", clamp(dragonInt("strength", 5) + 2, 0, 99));
            setDragonInt("willpower", clamp(dragonInt("willpower", 5) + 1, 0, 99));
            xpGain = 12;
            energyCost = 15;
            stressGain = 9;
            result = "Krafttraining abgeschlossen\n+2 Staerke\n+1 Willenskraft";
        } else if ("endurance".equals(key)) {
            setDragonInt("endurance", clamp(dragonInt("endurance", 5) + 2, 0, 99));
            xpGain = 11;
            energyCost = 13;
            stressGain = 6;
            result = "Ausdauertraining abgeschlossen\n+2 Ausdauer";
        } else if ("speed".equals(key)) {
            setDragonInt("speed", clamp(dragonInt("speed", 5) + 2, 0, 99));
            xpGain = 12;
            energyCost = 16;
            stressGain = 8;
            result = "Flugtraining abgeschlossen\n+2 Tempo";
        } else if ("instinct".equals(key)) {
            setDragonInt("instinct", clamp(dragonInt("instinct", 5) + 2, 0, 99));
            xpGain = 12;
            energyCost = 14;
            stressGain = 8;
            result = "Instinkttraining abgeschlossen\n+2 Instinkt";
        } else {
            setDragonInt("focus", clamp(dragonInt("focus", 5) + 2, 0, 99));
            setDragonInt("intelligence", clamp(dragonInt("intelligence", 5) + 1, 0, 99));
            xpGain = 11;
            energyCost = 11;
            stressGain = 4;
            result = "Fokusmeditation abgeschlossen\n+2 Fokus\n+1 Intelligenz";
        }

        setDragonInt("energy", clamp(energy - energyCost, 0, 100));
        setDragonInt("stress", clamp(stress + stressGain, 0, 100));
        setDragonInt("mood", clamp(dragonInt("mood", 60) - (stress > 70 ? 3 : 1), 0, 100));
        setDragonInt("bond", clamp(dragonInt("bond", 20) + 1, 0, 100));
        String evolution = addDragonXpResult(xpGain);
        dragonActionOut(out, result
                + "\n+" + xpGain + " XP"
                + "\n-" + energyCost + " Energie"
                + "\n+" + stressGain + " Stress"
                + "\n+1 Bindung"
                + evolution);
    }

    private void dragonArena(TextView out) {
        int energy = dragonInt("energy", 80);
        if (energy < 20) {
            dragonActionOut(out, "Arena blockiert: Energie unter 20. Erst regenerieren.");
            return;
        }
        startDragonBattle(out, true);
    }

    private void dragonFreeFight(TextView out) {
        int energy = dragonInt("energy", 80);
        if (energy < 12) {
            dragonActionOut(out, "Freikampf blockiert: Energie unter 12. Erst Pflege oder Ruhig.");
            return;
        }
        startDragonBattle(out, false);
    }

    private void dragonCare(TextView out) {
        setDragonInt("energy", clamp(dragonInt("energy", 80) + 10, 0, 100));
        setDragonInt("stress", clamp(dragonInt("stress", 15) - 12, 0, 100));
        setDragonInt("mood", clamp(dragonInt("mood", 60) + 8, 0, 100));
        setDragonInt("bond", clamp(dragonInt("bond", 20) + 3, 0, 100));
        String evolution = addDragonXpResult(2);
        dragonActionOut(out, "Pflege abgeschlossen"
                + "\n+10 Energie\n-12 Stress\n+8 Stimmung\n+3 Bindung\n+2 XP"
                + evolution);
    }

    private void dragonEvolve(TextView out) {
        int xp = dragonInt("xp", 0);
        int old = dragonInt("level", 1);
        int target = dragonLevel();
        setDragonInt("level", Math.max(old, target));
        out.setText(dragonSummary()
                + "\n\nEvolution geprueft: " + dragonStage()
                + ". Aktiver Elementpfad: " + dragonElement()
                + ". Naechste Schwelle: " + nextEvolutionText() + ".");
    }

    private void dragonCodex(TextView out) {
        out.setText(dragonSummary()
                + "\n\nCodex"
                + "\nHabitat: " + dragonHabitat()
                + "\nPfad: " + dragonPath()
                + "\nElement: " + dragonElement()
                + "\nAttacken: " + dragonAttackLine()
                + "\nBilanz: " + dragonInt("wins", 0) + " Siege | " + dragonInt("losses", 0) + " Niederlagen"
                + "\nFreigeschaltet: Kraft, Ausdauer, Flug, Fokus, Instinkt, Arena, Freikampf, Pflege, Entwicklung"
                + "\nRegel: lokal/offline; Nexi nur bewusst ueber Nexi fragen.");
    }

    private void dragonCalm(TextView out) {
        setDragonInt("energy", clamp(dragonInt("energy", 80) + 25, 0, 100));
        setDragonInt("stress", clamp(dragonInt("stress", 15) - 24, 0, 100));
        setDragonInt("mood", clamp(dragonInt("mood", 60) + 7, 0, 100));
        dragonActionOut(out, "Ruhephase abgeschlossen"
                + "\n+25 Energie\n-24 Stress\n+7 Stimmung\nKein XP-Gewinn: Ruhe ist Regeneration.");
    }

    private void dragonReset(TextView out) {
        setDragonInt("energy", 85);
        setDragonInt("stress", 15);
        setDragonInt("mood", 65);
        out.setText(dragonSummary() + "\n\nTagesreset: Energie und Stress neu gesetzt. XP und Siege bleiben erhalten.");
    }

    private void dragonAskNexi() {
        String state = dragonSummary();
        showChefPage();
        chefInput.setText("Digi-Dragon-Zustand:\n" + state + "\n\nBitte priorisiere kurz und konkret: Was ist der naechste sinnvolle Schritt?");
        if (chefLog != null) chefLog.setText("Digi-Dragon-Zustand bereit. Druecke 'An Nexi senden', wenn du Nexi wirklich fragen willst.");
    }

    private int dragonInt(String key, int fallback) {
        android.content.SharedPreferences prefs = NexusConfig.prefs(this);
        String dragonKey = "dragon_" + key;
        if (prefs.contains(dragonKey)) return prefs.getInt(dragonKey, fallback);
        return prefs.getInt(("compan" + "ion_") + key, fallback);
    }

    private void setDragonInt(String key, int value) {
        NexusConfig.prefs(this).edit().putInt("dragon_" + key, value).apply();
    }

    private void addDragonXp(int delta) {
        addDragonXpResult(delta);
    }

    private String addDragonXpResult(int delta) {
        int beforeXp = dragonInt("xp", 0);
        String beforeStage = dragonStageForXp(beforeXp);
        int xp = clamp(dragonInt("xp", 0) + delta, 0, 9999);
        setDragonInt("xp", xp);
        setDragonInt("level", Math.max(dragonInt("level", 1), dragonLevelForXp(xp)));
        String afterStage = dragonStageForXp(xp);
        if (!beforeStage.equals(afterStage)) {
            return "\nEvolution freigeschaltet: " + beforeStage + " -> " + afterStage;
        }
        return "";
    }

    private int dragonLevel() { return dragonLevelForXp(dragonInt("xp", 0)); }
    private int dragonLevelForXp(int xp) { return Math.max(1, Math.min(50, 1 + (xp / 100))); }

    private String dragonStage() {
        return dragonStageForXp(dragonInt("xp", 0));
    }

    private String dragonStageForXp(int xp) {
        if (xp >= 700) return "Nexus-Drache";
        if (xp >= 320) return "Wachdrache";
        if (xp >= 120) return "Jungdrache";
        return "Nestling";
    }

    private static final class DragonMove {
        final String name;
        final String type;
        final int power;
        final int cost;
        final String effect;

        DragonMove(String name, String type, int power, int cost, String effect) {
            this.name = name;
            this.type = type;
            this.power = power;
            this.cost = cost;
            this.effect = effect;
        }
    }

    private static final class DragonEnemy {
        final String name;
        final String arena;
        final int rating;
        final int reward;

        DragonEnemy(String name, String arena, int rating, int reward) {
            this.name = name;
            this.arena = arena;
            this.rating = rating;
            this.reward = reward;
        }
    }

    private void startDragonBattle(TextView out, boolean arena) {
        if (dragonBattleActive()) {
            dragonActionOut(out, "Aktiver Kampf laeuft bereits.\n" + dragonBattleLine() + "\nWaehle eine Attacke oder Rueckzug.");
            return;
        }
        boolean rare = !arena && ((dragonInt("instinct", 5) + dragonInt("xp", 0) + (int)(System.currentTimeMillis() / 60000L)) % 7) == 0;
        DragonEnemy enemy = dragonEnemy(arena, rare);
        int seed = dragonBattleSeed(arena ? 71 : 53);
        int dragonHp = 42 + dragonLevel() * 6 + dragonInt("endurance", 5) * 4;
        int enemyHp = 34 + enemy.rating * 2;
        int focusPool = 18 + dragonInt("focus", 5) + dragonInt("willpower", 5);
        android.content.SharedPreferences.Editor edit = NexusConfig.prefs(this).edit();
        edit.putBoolean("dragon_battle_active", true);
        edit.putString("dragon_battle_mode", arena ? "arena" : "free");
        edit.putString("dragon_battle_enemy", enemy.name);
        edit.putString("dragon_battle_place", enemy.arena);
        edit.putInt("dragon_battle_rating", enemy.rating);
        edit.putInt("dragon_battle_reward", enemy.reward);
        edit.putInt("dragon_battle_seed", seed);
        edit.putInt("dragon_battle_round", 1);
        edit.putInt("dragon_battle_dragon_hp", dragonHp);
        edit.putInt("dragon_battle_enemy_hp", enemyHp);
        edit.putInt("dragon_battle_focus", focusPool);
        edit.putString("dragon_battle_intent", dragonBattleIntent(seed, 1));
        edit.apply();
        dragonActionOut(out, (arena ? "Arena-Kampf gestartet" : rare ? "Seltener Freikampf gestartet" : "Freikampf gestartet")
                + "\nGegner: " + enemy.name + " | " + enemy.arena
                + "\nDrache HP " + dragonHp + " | Gegner HP " + enemyHp + " | Fokus " + focusPool
                + "\nGegnerabsicht: " + dragonBattleIntent(seed, 1)
                + "\nWaehle jetzt eine Attacke.");
    }

    private void dragonUseMove(TextView out, int index) {
        if (!dragonBattleActive()) {
            dragonActionOut(out, "Kein aktiver Kampf. Starte zuerst Arena oder Freikampf.");
            return;
        }
        DragonMove[] moves = dragonMoves();
        int safeIndex = Math.max(0, Math.min(moves.length - 1, index));
        DragonMove move = moves[safeIndex];
        int focus = dragonBattleInt("focus", 0);
        if (focus < move.cost) {
            dragonActionOut(out, "Nicht genug Fokus fuer " + move.name + ".\nFokus: " + focus + " | Kosten: " + move.cost
                    + "\nNutze eine guenstigere Attacke oder Rueckzug.");
            return;
        }

        int seed = dragonBattleInt("seed", dragonBattleSeed(11));
        int round = dragonBattleInt("round", 1);
        int enemyRating = dragonBattleInt("rating", 20);
        int dragonHp = dragonBattleInt("dragon_hp", 60);
        int enemyHp = dragonBattleInt("enemy_hp", 60);
        String intent = dragonBattleString("intent", dragonBattleIntent(seed, round));

        focus = Math.max(0, focus - move.cost);
        int dragonStrike = dragonMovePower(move, seed, round);
        int enemyStrike = Math.max(3, enemyRating / 2 + ((seed + round * 5) % 11) - dragonInt("speed", 5) / 3);

        if ("Schwerer Angriff".equals(intent)) {
            enemyStrike += 8;
            if ("Konter".equals(move.type)) dragonStrike += 10;
            if ("Schild".equals(move.type)) enemyStrike = Math.max(1, enemyStrike - 12 - dragonInt("endurance", 5) / 4);
        } else if ("Deckung".equals(intent)) {
            dragonStrike = Math.max(2, dragonStrike - 8);
            focus += 2;
        } else if ("Offen".equals(intent)) {
            dragonStrike += 7;
        } else if ("Schneller Schlag".equals(intent)) {
            enemyStrike += 3;
            if ("Schild".equals(move.type)) enemyStrike = Math.max(1, enemyStrike - 7);
        }

        if ("Schild".equals(move.type)) enemyStrike = Math.max(1, enemyStrike - 5 - dragonInt("endurance", 5) / 5);
        if ("Konter".equals(move.type) && enemyStrike > 7) dragonStrike += 5;
        if ("Fokus".equals(move.type)) focus += 6;

        enemyHp = Math.max(0, enemyHp - dragonStrike);
        if (enemyHp > 0) dragonHp = Math.max(0, dragonHp - enemyStrike);

        StringBuilder report = new StringBuilder();
        report.append("Runde ").append(round).append(": ").append(move.name)
                .append("\nGegnerabsicht: ").append(intent)
                .append("\n").append(move.effect)
                .append("\nSchaden: ").append(dragonStrike);
        if (enemyHp > 0) report.append("\nGegenschlag: ").append(enemyStrike);
        report.append("\nDrache HP ").append(dragonHp).append(" | Gegner HP ").append(enemyHp).append(" | Fokus ").append(focus);

        if (enemyHp <= 0 || dragonHp <= 0) {
            finishDragonBattle(out, report, enemyHp <= 0, dragonHp, enemyHp);
            return;
        }

        int nextRound = round + 1;
        String nextIntent = dragonBattleIntent(seed, nextRound);
        android.content.SharedPreferences.Editor edit = NexusConfig.prefs(this).edit();
        edit.putInt("dragon_battle_round", nextRound);
        edit.putInt("dragon_battle_dragon_hp", dragonHp);
        edit.putInt("dragon_battle_enemy_hp", enemyHp);
        edit.putInt("dragon_battle_focus", focus);
        edit.putString("dragon_battle_intent", nextIntent);
        edit.apply();
        dragonActionOut(out, report.append("\nNaechste Gegnerabsicht: ").append(nextIntent).toString());
    }

    private void finishDragonBattle(TextView out, StringBuilder report, boolean win, int dragonHp, int enemyHp) {
        boolean arena = "arena".equals(dragonBattleString("mode", "free"));
        int reward = dragonBattleInt("reward", arena ? 28 : 13);
        int xpGain = win ? reward : Math.max(5, reward / 3);
        int energyCost = arena ? 18 : 12;
        int stressGain = win ? (arena ? 10 : 6) : (arena ? 17 : 10);
        int moodDelta = win ? (arena ? 6 : 3) : -6;
        int bondDelta = win ? (arena ? 2 : 1) : 0;

        setDragonInt("energy", clamp(dragonInt("energy", 80) - energyCost, 0, 100));
        setDragonInt("stress", clamp(dragonInt("stress", 15) + stressGain, 0, 100));
        setDragonInt("mood", clamp(dragonInt("mood", 60) + moodDelta, 0, 100));
        setDragonInt("bond", clamp(dragonInt("bond", 20) + bondDelta, 0, 100));
        if (!arena) setDragonInt("instinct", clamp(dragonInt("instinct", 5) + 1, 0, 99));
        if (win) setDragonInt("wins", dragonInt("wins", 0) + 1);
        else setDragonInt("losses", dragonInt("losses", 0) + 1);

        clearDragonBattle();
        String evolution = addDragonXpResult(xpGain);
        report.append("\nErgebnis: ").append(win ? "Sieg" : "Niederlage")
                .append(" | Drache HP ").append(dragonHp)
                .append(" | Gegner HP ").append(enemyHp)
                .append("\n+").append(xpGain).append(" XP")
                .append(win ? "\n+1 Sieg" : "\n+1 Niederlage")
                .append(bondDelta > 0 ? "\n+" + bondDelta + " Bindung" : "")
                .append(!arena ? "\n+1 Instinkt" : "")
                .append("\n-").append(energyCost).append(" Energie")
                .append("\n+").append(stressGain).append(" Stress")
                .append(evolution);
        dragonActionOut(out, report.toString());
    }

    private void dragonRetreat(TextView out) {
        if (!dragonBattleActive()) {
            dragonActionOut(out, "Kein aktiver Kampf fuer Rueckzug.");
            return;
        }
        clearDragonBattle();
        setDragonInt("energy", clamp(dragonInt("energy", 80) - 5, 0, 100));
        setDragonInt("stress", clamp(dragonInt("stress", 15) + 4, 0, 100));
        dragonActionOut(out, "Rueckzug ausgefuehrt\n-5 Energie\n+4 Stress\nKeine Niederlage gespeichert.");
    }

    private String dragonBattleLine() {
        if (!dragonBattleActive()) return "Kein aktiver Kampf. Starte Arena oder Freikampf, dann waehle pro Runde eine Attacke.";
        return dragonBattleString("enemy", "?")
                + " | " + dragonBattleString("place", "?")
                + "\nRunde " + dragonBattleInt("round", 1)
                + " | Drache HP " + dragonBattleInt("dragon_hp", 0)
                + " | Gegner HP " + dragonBattleInt("enemy_hp", 0)
                + " | Fokus " + dragonBattleInt("focus", 0)
                + "\nGegnerabsicht: " + dragonBattleString("intent", "?");
    }

    private boolean dragonBattleActive() {
        return NexusConfig.prefs(this).getBoolean("dragon_battle_active", false);
    }

    private int dragonBattleInt(String key, int fallback) {
        return NexusConfig.prefs(this).getInt("dragon_battle_" + key, fallback);
    }

    private String dragonBattleString(String key, String fallback) {
        return NexusConfig.prefs(this).getString("dragon_battle_" + key, fallback);
    }

    private void clearDragonBattle() {
        NexusConfig.prefs(this).edit()
                .putBoolean("dragon_battle_active", false)
                .remove("dragon_battle_mode")
                .remove("dragon_battle_enemy")
                .remove("dragon_battle_place")
                .remove("dragon_battle_rating")
                .remove("dragon_battle_reward")
                .remove("dragon_battle_seed")
                .remove("dragon_battle_round")
                .remove("dragon_battle_dragon_hp")
                .remove("dragon_battle_enemy_hp")
                .remove("dragon_battle_focus")
                .remove("dragon_battle_intent")
                .apply();
    }

    private String dragonBattleIntent(int seed, int round) {
        int v = Math.abs(seed + round * 37 + dragonInt("wins", 0) * 5) % 4;
        if (v == 0) return "Schwerer Angriff";
        if (v == 1) return "Deckung";
        if (v == 2) return "Offen";
        return "Schneller Schlag";
    }

    private void runDragonBattle(TextView out, boolean arena) {
        boolean rare = !arena && ((dragonInt("instinct", 5) + dragonInt("xp", 0) + (int)(System.currentTimeMillis() / 60000L)) % 7) == 0;
        DragonEnemy enemy = dragonEnemy(arena, rare);
        DragonMove[] moves = dragonMoves();
        int seed = dragonBattleSeed(arena ? 31 : 17);
        int dragonHp = 42 + dragonLevel() * 6 + dragonInt("endurance", 5) * 4;
        int enemyHp = 34 + enemy.rating * 2;
        int focusPool = 18 + dragonInt("focus", 5) + dragonInt("willpower", 5);
        int stress = dragonInt("stress", 15);
        StringBuilder report = new StringBuilder();
        report.append(arena ? "Arena-Kampf" : (rare ? "Seltener Freikampf" : "Freikampf"));
        report.append("\nGegner: ").append(enemy.name).append(" | ").append(enemy.arena);

        for (int round = 1; round <= 3 && dragonHp > 0 && enemyHp > 0; round++) {
            DragonMove move = moves[Math.abs(seed + round + dragonInt("wins", 0)) % moves.length];
            if (focusPool < move.cost) move = moves[0];
            focusPool = Math.max(0, focusPool - move.cost);

            int dragonStrike = dragonMovePower(move, seed, round);
            int enemyStrike = Math.max(4, enemy.rating / 2 + ((seed + round * 5) % 11) - dragonInt("speed", 5) / 3);
            if ("Schild".equals(move.type)) enemyStrike = Math.max(1, enemyStrike - 7 - dragonInt("endurance", 5) / 4);
            if ("Konter".equals(move.type) && enemyStrike > 8) dragonStrike += 6;
            if ("Fokus".equals(move.type)) focusPool += 4;

            enemyHp = Math.max(0, enemyHp - dragonStrike);
            if (enemyHp > 0) dragonHp = Math.max(0, dragonHp - enemyStrike);

            report.append("\nR").append(round)
                    .append(": ").append(move.name)
                    .append(" (").append(move.effect).append(")")
                    .append(" -> ").append(dragonStrike).append(" Schaden");
            if (enemyHp > 0) report.append(" | Gegenschlag ").append(enemyStrike);
        }

        boolean win = enemyHp <= 0 || dragonHp >= enemyHp;
        int energyCost = arena ? 18 : 12;
        int stressGain = win ? (arena ? 10 : rare ? 4 : 6) : (arena ? 17 : 10);
        int xpGain = win ? enemy.reward : Math.max(5, enemy.reward / 3);
        int moodDelta = win ? (arena ? 6 : 3) : -6;
        int bondDelta = win ? (arena ? 2 : 1) : 0;

        setDragonInt("energy", clamp(dragonInt("energy", 80) - energyCost, 0, 100));
        setDragonInt("stress", clamp(stress + stressGain, 0, 100));
        setDragonInt("mood", clamp(dragonInt("mood", 60) + moodDelta, 0, 100));
        setDragonInt("bond", clamp(dragonInt("bond", 20) + bondDelta, 0, 100));
        if (!arena) setDragonInt("instinct", clamp(dragonInt("instinct", 5) + 1, 0, 99));
        if (win) setDragonInt("wins", dragonInt("wins", 0) + 1);
        else setDragonInt("losses", dragonInt("losses", 0) + 1);

        String evolution = addDragonXpResult(xpGain);
        report.append("\nErgebnis: ").append(win ? "Sieg" : "Niederlage")
                .append(" | Drache HP ").append(dragonHp)
                .append(" | Gegner HP ").append(enemyHp)
                .append("\n+").append(xpGain).append(" XP")
                .append(win ? "\n+1 Sieg" : "\n+1 Niederlage")
                .append(bondDelta > 0 ? "\n+" + bondDelta + " Bindung" : "")
                .append(!arena ? "\n+1 Instinkt" : "")
                .append("\n-").append(energyCost).append(" Energie")
                .append("\n+").append(stressGain).append(" Stress")
                .append(evolution);
        dragonActionOut(out, report.toString());
    }

    private int dragonBattleSeed(int salt) {
        return Math.abs((int)(System.currentTimeMillis() / 1000L)
                + dragonInt("xp", 0)
                + dragonInt("wins", 0) * 13
                + dragonInt("losses", 0) * 7
                + dragonElement().hashCode()
                + salt);
    }

    private DragonEnemy dragonEnemy(boolean arena, boolean rare) {
        int seed = dragonBattleSeed(arena ? 43 : 29);
        int level = dragonLevel();
        int wins = dragonInt("wins", 0);
        String element = dragonElement();
        String[] arenaNames;
        String[] wildNames;
        if ("Wasser".equals(element)) {
            arenaNames = new String[]{"Korallen-Waechter", "Tiefenlanzer", "Abyssal-Champion"};
            wildNames = new String[]{"Riffbeisser", "Nebelrochen", "Tiefenfunke"};
        } else if ("Erde".equals(element)) {
            arenaNames = new String[]{"Basalt-Schild", "Obsidian-Ringer", "Titanenbrecher"};
            wildNames = new String[]{"Moosgolem", "Steinlaeufer", "Kristallhorn"};
        } else if ("Luft".equals(element)) {
            arenaNames = new String[]{"Sturmduellant", "Wolkenklinge", "Himmelsrichter"};
            wildNames = new String[]{"Boenengeist", "Klippenjaeger", "Federblitz"};
        } else if ("Schatten".equals(element)) {
            arenaNames = new String[]{"Runenhenker", "Nachttribun", "Void-Hydra"};
            wildNames = new String[]{"Schattenpilger", "Nebelzahn", "Kluftschleicher"};
        } else {
            arenaNames = new String[]{"Glutritter", "Aschenkoloss", "Inferno-Waechter"};
            wildNames = new String[]{"Funkenwolf", "Lavaraupe", "Rauchklaue"};
        }
        String[] pool = arena ? arenaNames : wildNames;
        String name = rare ? "Seltene Begegnung: " + pool[Math.abs(seed) % pool.length] : pool[Math.abs(seed) % pool.length];
        int rating = arena
                ? level * 12 + wins * 3 + 24 + (seed % 9)
                : level * 9 + 18 + (seed % 11) + (rare ? 8 : 0);
        int reward = arena ? 28 + level * 2 + wins / 2 : rare ? 22 + level : 13 + level;
        return new DragonEnemy(name, arena ? "Rang-Arena" : dragonHabitat(), rating, reward);
    }

    private DragonMove[] dragonMoves() {
        String element = dragonElement();
        if ("Wasser".equals(element)) {
            return new DragonMove[]{
                    new DragonMove("Wellenbiss", "Schaden", 13, 4, "sicherer Wasserangriff"),
                    new DragonMove("Tiefenblick", "Fokus", 10, 3, "Fokus rueckgewinnen"),
                    new DragonMove("Nebelschild", "Schild", 7, 5, "Gegenschlag daempfen"),
                    new DragonMove("Abyssaler Sog", "Konter", 16, 8, "stark gegen Druck")
            };
        }
        if ("Erde".equals(element)) {
            return new DragonMove[]{
                    new DragonMove("Steinklaue", "Schaden", 14, 4, "massiver Treffer"),
                    new DragonMove("Panzerstand", "Schild", 8, 4, "Schaden blocken"),
                    new DragonMove("Kristallstoss", "Schaden", 17, 7, "hohe Durchschlagskraft"),
                    new DragonMove("Titanenkonter", "Konter", 15, 7, "Antwort auf harte Treffer")
            };
        }
        if ("Luft".equals(element)) {
            return new DragonMove[]{
                    new DragonMove("Sturmflug", "Schaden", 13, 4, "schneller Angriff"),
                    new DragonMove("Windkante", "Schaden", 16, 6, "kritische Kante"),
                    new DragonMove("Ausweichen", "Schild", 6, 3, "Gegenschlag mindern"),
                    new DragonMove("Himmelssturz", "Konter", 18, 8, "riskanter Sturzangriff")
            };
        }
        if ("Schatten".equals(element)) {
            return new DragonMove[]{
                    new DragonMove("Schattensprung", "Schaden", 14, 4, "schneller Riss"),
                    new DragonMove("Nachtklaue", "Schaden", 17, 7, "dunkler Treffer"),
                    new DragonMove("Nebelkoerper", "Schild", 7, 5, "Treffer verwischen"),
                    new DragonMove("Seelenkonter", "Konter", 18, 8, "Konter aus der Kluft")
            };
        }
        return new DragonMove[]{
                new DragonMove("Flammenstoss", "Schaden", 14, 4, "direkter Feuerdruck"),
                new DragonMove("Aschenklaue", "Schaden", 16, 6, "brennender Schnitt"),
                new DragonMove("Glutpanzer", "Schild", 7, 5, "Gegenschlag verbrennen"),
                new DragonMove("Inferno-Konter", "Konter", 18, 8, "Antwort mit Feuerkern")
        };
    }

    private int dragonMovePower(DragonMove move, int seed, int round) {
        int base = move.power
                + dragonInt("strength", 5) / 2
                + dragonInt("focus", 5) / 3
                + dragonInt("instinct", 5) / 4
                + dragonLevel();
        if ("Schaden".equals(move.type)) base += dragonInt("speed", 5) / 4;
        if ("Konter".equals(move.type)) base += dragonInt("willpower", 5) / 3;
        if ("Fokus".equals(move.type)) base += dragonInt("intelligence", 5) / 3;
        int variance = Math.abs(seed + round * 3 + move.name.hashCode()) % 7;
        int stressPenalty = dragonInt("stress", 15) / 18;
        return Math.max(3, base + variance - stressPenalty);
    }

    private int dragonBattlePower(int pressure) {
        int stat = dragonInt("strength", 5) * 3
                + dragonInt("endurance", 5) * 2
                + dragonInt("speed", 5) * 2
                + dragonInt("focus", 5) * 2
                + dragonInt("instinct", 5) * 2
                + dragonInt("willpower", 5);
        int support = dragonInt("bond", 20) / 4 + dragonInt("mood", 60) / 10;
        int penalty = dragonInt("stress", 15) / 6;
        int pulse = (int)(System.currentTimeMillis() / 1000L) % Math.max(1, pressure);
        return Math.max(1, stat + support + pulse - penalty);
    }

    private void dragonActionOut(TextView out, String result) {
        String clean = result == null ? "" : result.trim();
        NexusConfig.prefs(this).edit().putString("dragon_last_action", clean).apply();
        if (out != null) out.setText(dragonSummary() + "\n\n" + clean);
    }

    private int dragonImageResource() {
        String element = dragonElement();
        int xp = dragonInt("xp", 0);
        if ("Wasser".equals(element)) {
            if (xp >= 700) return R.drawable.dragon_wasser_ahn;
            if (xp >= 320) return R.drawable.dragon_wasser_adult;
            if (xp >= 120) return R.drawable.dragon_wasser_jung;
            return R.drawable.dragon_wasser_ei;
        }
        if ("Erde".equals(element)) {
            if (xp >= 700) return R.drawable.dragon_erde_ahn;
            if (xp >= 320) return R.drawable.dragon_erde_adult;
            if (xp >= 120) return R.drawable.dragon_erde_jung;
            return R.drawable.dragon_erde_ei;
        }
        if ("Feuer".equals(element)) {
            if (xp >= 700) return R.drawable.dragon_feuer_ahn;
            if (xp >= 320) return R.drawable.dragon_feuer_adult;
            if (xp >= 120) return R.drawable.dragon_feuer_jung;
            return R.drawable.dragon_feuer_ei;
        }
        if ("Luft".equals(element)) {
            if (xp >= 700) return R.drawable.dragon_luft_ahn;
            if (xp >= 320) return R.drawable.dragon_luft_adult;
            if (xp >= 120) return R.drawable.dragon_luft_jung;
            return R.drawable.dragon_luft_ei;
        }
        if ("Schatten".equals(element)) {
            if (xp >= 700) return R.drawable.dragon_schatten_ahn;
            if (xp >= 320) return R.drawable.dragon_schatten_adult;
            if (xp >= 120) return R.drawable.dragon_schatten_jung;
            return R.drawable.dragon_schatten_ei;
        }
        return 0;
    }

    private int dragonEnvironmentResource(String environment) {
        String element = dragonElement();
        if ("Wasser".equals(element)) {
            if ("arena".equals(environment)) return R.drawable.dragon_wasser_arena;
            if ("training".equals(environment)) return R.drawable.dragon_wasser_training;
            return R.drawable.dragon_wasser_habitat;
        }
        if ("Erde".equals(element)) {
            if ("arena".equals(environment)) return R.drawable.dragon_erde_arena;
            if ("training".equals(environment)) return R.drawable.dragon_erde_training;
            return R.drawable.dragon_erde_habitat;
        }
        if ("Feuer".equals(element)) {
            if ("arena".equals(environment)) return R.drawable.dragon_feuer_arena;
            if ("training".equals(environment)) return R.drawable.dragon_feuer_training;
            return R.drawable.dragon_feuer_habitat;
        }
        if ("Luft".equals(element)) {
            if ("arena".equals(environment)) return R.drawable.dragon_luft_arena;
            if ("training".equals(environment)) return R.drawable.dragon_luft_training;
            return R.drawable.dragon_luft_habitat;
        }
        if ("Schatten".equals(element)) {
            if ("arena".equals(environment)) return R.drawable.dragon_schatten_arena;
            if ("training".equals(environment)) return R.drawable.dragon_schatten_training;
            return R.drawable.dragon_schatten_habitat;
        }
        return 0;
    }

    private String dragonVisualEnvironment(boolean showEvolutionBoard) {
        if (showEvolutionBoard) return "habitat";
        if ("battle".equals(dragonMode())) return dragonScene();
        return "habitat";
    }

    private String dragonScene() {
        String value = NexusConfig.prefs(this).getString("dragon_scene", "training");
        if (!"habitat".equals(value) && !"training".equals(value) && !"arena".equals(value)) return "training";
        return value;
    }

    private void setDragonScene(String value) {
        String v = value == null ? "training" : value.trim();
        if (!"habitat".equals(v) && !"training".equals(v) && !"arena".equals(v)) v = "training";
        NexusConfig.prefs(this).edit().putString("dragon_scene", v).apply();
    }

    private String nextEvolutionText() {
        int xp = dragonInt("xp", 0);
        if (xp < 120) return (120 - xp) + " XP bis Jungdrache";
        if (xp < 320) return (320 - xp) + " XP bis Wachdrache";
        if (xp < 700) return (700 - xp) + " XP bis Nexus-Drache";
        return "maximale Stufe aktiv";
    }

    private String dragonPath() {
        String element = dragonElement();
        int bond = dragonInt("bond", 20);
        int focus = dragonInt("focus", 5);
        int instinct = dragonInt("instinct", 5);
        int wins = dragonInt("wins", 0);
        if (focus >= 20 && dragonInt("intelligence", 5) >= 15) return "Arkaner Pfad";
        if (wins >= 8 || dragonInt("strength", 5) >= 18) return "Kriegsdrache";
        if (instinct >= 18) return "Jaegerpfad";
        if (bond >= 45) return "Waechterpfad";
        return element + "-Nestpfad";
    }

    private String dragonElement() {
        return NexusConfig.prefs(this).getString("dragon_element", "Feuer");
    }

    private void setDragonElement(String value) {
        String v = value == null ? "" : value.trim();
        if (!"Wasser".equals(v) && !"Erde".equals(v) && !"Feuer".equals(v) && !"Luft".equals(v) && !"Schatten".equals(v)) {
            v = "Feuer";
        }
        NexusConfig.prefs(this).edit().putString("dragon_element", v).apply();
    }

    private String dragonMode() {
        String mode = NexusConfig.prefs(this).getString("dragon_mode", "home");
        if ("evolution".equals(mode) || "battle".equals(mode) || "system".equals(mode)) return mode;
        return "home";
    }

    private void setDragonMode(String value) {
        String mode = value == null ? "home" : value.trim();
        if (!"home".equals(mode) && !"evolution".equals(mode) && !"battle".equals(mode) && !"system".equals(mode)) {
            mode = "home";
        }
        NexusConfig.prefs(this).edit().putString("dragon_mode", mode).apply();
    }

    private String dragonModeLabel(String mode, String label, String activeMode) {
        return mode.equals(activeMode) ? "* " + label : label;
    }

    private String dragonHomeLine() {
        return dragonStage() + " L" + dragonLevel()
                + " | " + dragonElement()
                + " | Energie " + dragonInt("energy", 80)
                + " | Bindung " + dragonInt("bond", 20);
    }

    private String dragonHabitat() {
        int level = dragonLevel();
        if (level >= 12) return "Schwebende Insel";
        if (level >= 8) return "Drachenhort";
        if (level >= 5) return "Geheiligtes Lager";
        if (level >= 3) return "Hoehle";
        return "Kleines Nest";
    }

    private String dragonAttackLine() {
        String element = dragonElement();
        if ("Wasser".equals(element)) return "Wellenbiss, Tiefenblick, Nebelschild, Fokusatem";
        if ("Erde".equals(element)) return "Steinklaue, Panzerstand, Kristallstoss, Drachengarde";
        if ("Luft".equals(element)) return "Sturmflug, Windkante, Ausweichen, Himmelssturz";
        if ("Schatten".equals(element)) return "Schattensprung, Nachtklaue, Nebelkoerper, Seelenkonter";
        return "Flammenstoss, Aschenklaue, Glutpanzer, Drachengarde";
    }

    private String dragonSummary() {
        String last = NexusConfig.prefs(this).getString("dragon_last_action", "");
        String lastLine = last == null ? "" : last.replace("\n", " | ");
        if (lastLine.length() > 170) lastLine = lastLine.substring(0, 167) + "...";
        return "Digi Dragon: " + dragonStage()
                + "\nLevel: " + dragonLevel() + " | XP: " + dragonInt("xp", 0)
                + " | Siege: " + dragonInt("wins", 0) + " | Niederlagen: " + dragonInt("losses", 0)
                + "\nEnergie: " + dragonInt("energy", 80) + " | Stress: " + dragonInt("stress", 15)
                + "\nStimmung: " + dragonInt("mood", 60) + " | Bindung: " + dragonInt("bond", 20)
                + "\nHabitat: " + dragonHabitat() + " | Element: " + dragonElement()
                + "\nPfad: " + dragonPath()
                + "\nStats: STR " + dragonInt("strength", 5) + " | END " + dragonInt("endurance", 5)
                + " | SPD " + dragonInt("speed", 5) + " | FOK " + dragonInt("focus", 5)
                + " | INS " + dragonInt("instinct", 5)
                + "\nNaechste Evolution: " + nextEvolutionText()
                + (lastLine.trim().isEmpty() ? "" : "\nLetzte Aktion: " + lastLine);
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private void loadHomeSnapshot(TextView target) {
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    JSONObject json = new JSONObject(httpGet(base + "/api/widget/messages?limit=5"));
                    if (!json.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    setNexyBridgeBase(base);
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
        clearPage(PAGE_CHEF, "Nexi", "Direkter Nexi-Kanal. Klarer Auftrag, klare Antwort.");
        LinearLayout p = activePanel();
        chefInput = input("Nexi Kontext, Frage oder Auftrag schreiben...", false);
        chefInput.setMinLines(3);
        chefInput.setMaxLines(7);
        p.addView(chefInput, card(8));
        row(p, nav("An Nexi senden", v -> sendChef()), nav("Nexi laden", v -> loadChefLog()));
        chefLog = logBox("Nexi-Kanal wird geladen...");
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
        row(p, nav("Alle neu", v -> loadMessages(summary, list, "")), nav("Widget aktualisieren", v -> { NexusMessagesWidgetProvider.updateAll(this); loadMessages(summary, list, searchText()); }));
        p.addView(summary, card(8));
        p.addView(list);
        loadMessages(summary, list, "");
    }

    private String searchText() {
        return messageSearch == null ? "" : messageSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private void loadMessages(TextView summary, LinearLayout list, String filter) {
        final String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        summary.setText("Lade Nachrichten...");
        list.removeAllViews();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
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

                    setNexyBridgeBase(base);
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
            row(card, nav("Zeitstrahl", v -> decide(finalEventId, "timeline_focus")), nav("Nexi-Kontext", v -> putContext(finalSender, finalPreview)));
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
        return hay.toString().toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
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
        chefLog.setText("Kontext eintragen und an Nexi senden.");
    }

    private void decide(String eventId, String action) {
        if (eventId == null || eventId.isEmpty()) return;
        hideLocal(eventId);
        if (PAGE_MESSAGES.equals(currentPage)) showMessagesPage();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    String body = "event_id=" + enc(eventId) + "&action=" + enc(action) + "&scope=conversation";
                    JSONObject res = new JSONObject(httpPost(base + "/api/widget/message-action", body));
                    if (res.optBoolean("ok", false)) {
                        setNexyBridgeBase(base);
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
        clearPage(PAGE_FILES, "Dateien", "Explorer. Ordner oeffnen, Dateien pruefen, keine riskante Aktion.");
        LinearLayout p = activePanel();
        TextView info = label("Lade Dateien...", 13, true, Color.rgb(240, 235, 226));
        LinearLayout list = vertical();
        row(p, nav("Neu laden", v -> loadFiles(info, list, currentFilesPath)), nav("Root", v -> { currentFilesPath = ""; loadFiles(info, list, currentFilesPath); }));
        row(p, nav("Aufwaerts", v -> { currentFilesPath = parentPath(currentFilesPath); loadFiles(info, list, currentFilesPath); }), nav("Zeitstrahl", v -> showTimelinePage()));
        p.addView(info, card(8));
        p.addView(list);
        loadFiles(info, list, currentFilesPath);
    }

    private void loadFiles(TextView info, LinearLayout list, String path) {
        final String relPath = path == null ? "" : path;
        info.setText("Lade Dateien...");
        list.removeAllViews();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    JSONObject root = new JSONObject(httpGet(base + "/api/files/list?limit=160&path=" + enc(relPath)));
                    if (!root.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    setNexyBridgeBase(base);
                    runOnUiThread(() -> renderFiles(info, list, root, base));
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> info.setText("Dateien nicht erreichbar: " + err));
        }).start();
    }

    private void renderFiles(TextView info, LinearLayout list, JSONObject root, String base) {
        if (!PAGE_FILES.equals(currentPage)) return;
        JSONObject data = root.optJSONObject("data");
        if (data == null) { info.setText("Quelle: " + host(base) + "\nAntwort konnte nicht als Dateiliste gelesen werden."); return; }
        JSONArray items = data.optJSONArray("items");
        if (items == null) { info.setText("Quelle: " + host(base) + "\nKeine Dateiliste im Ergebnis."); return; }
        currentFilesPath = data.optString("path", currentFilesPath);
        String shownPath = currentFilesPath == null || currentFilesPath.isEmpty() ? "/" : currentFilesPath;
        info.setText("Quelle: " + host(base) + "\nPfad: " + shownPath + "\nEintraege: " + items.length() + " von " + data.optInt("count", items.length()));
        list.removeAllViews();
        if (items.length() == 0) {
            list.addView(logBox("Dieser Ordner ist leer."));
            return;
        }
        int max = Math.min(160, items.length());
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            boolean dir = item.optBoolean("is_dir", "dir".equals(item.optString("type", "")));
            final String itemPath = item.optString("path", "");
            final String itemName = item.optString("name", dir ? "Ordner" : "Datei");
            LinearLayout card = miniCard();
            card.addView(label((dir ? "[DIR] " : "[FILE] ") + itemName, 14, true, Color.WHITE));
            card.addView(label(fileMeta(item), 11, false, sub()));
            card.addView(label(cut(itemPath, 180), 11, false, Color.rgb(180, 174, 166)));
            if (dir) {
                row(card, nav("Oeffnen", v -> { currentFilesPath = itemPath; loadFiles(info, list, currentFilesPath); }), nav("Root", v -> { currentFilesPath = ""; loadFiles(info, list, currentFilesPath); }));
            } else {
                row(card, nav("Details", v -> info.setText("Datei: " + itemName + "\nPfad: " + itemPath + "\n" + fileMeta(item))), nav("Ordner", v -> loadFiles(info, list, currentFilesPath)));
            }
            list.addView(card, card(6));
        }
    }

    private String parentPath(String path) {
        if (path == null || path.trim().isEmpty()) return "";
        String clean = path.replace("\\", "/");
        int idx = clean.lastIndexOf('/');
        return idx <= 0 ? "" : clean.substring(0, idx);
    }

    private String fileMeta(JSONObject item) {
        boolean dir = item.optBoolean("is_dir", "dir".equals(item.optString("type", "")));
        if (dir) return "Ordner";
        long bytes = item.optLong("bytes", 0L);
        String ext = item.optString("extension", "");
        return "Datei | " + formatBytes(bytes) + (ext.isEmpty() ? "" : " | " + ext);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return (bytes / 1024L) + " KB";
        if (bytes < 1024L * 1024L * 1024L) return (bytes / (1024L * 1024L)) + " MB";
        return (bytes / (1024L * 1024L * 1024L)) + " GB";
    }

    private void showTimelinePage() {
        clearPage(PAGE_TIMELINE, "Zeitstrahl", "Chronik und Entscheidungen. Erledigt bleibt sichtbar, aber markiert.");
        LinearLayout p = activePanel();
        row(p, nav("Neu laden", v -> showTimelinePage()), nav("Nachrichten", v -> showMessagesPage()));
        TextView info = label("Lade Zeitstrahl...", 13, true, Color.rgb(240, 235, 226));
        LinearLayout list = vertical();
        p.addView(info, card(8));
        p.addView(list);
        loadTimeline(info, list);
    }

    private void loadTimeline(TextView info, LinearLayout list) {
        info.setText("Lade Zeitstrahl...");
        list.removeAllViews();
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    JSONObject root = new JSONObject(httpGet(base + "/api/nexy/timeline?limit=80"));
                    if (!root.optBoolean("ok", false)) { last = host(base) + ": ok=false"; continue; }
                    setNexyBridgeBase(base);
                    runOnUiThread(() -> renderTimeline(info, list, root, base));
                    return;
                } catch (Exception ex) {
                    last = host(base) + ": " + ex.getClass().getSimpleName();
                }
            }
            final String err = last;
            runOnUiThread(() -> { info.setText("Zeitstrahl nicht erreichbar: " + err); list.removeAllViews(); });
        }).start();
    }

    private void renderTimeline(TextView info, LinearLayout list, JSONObject root, String base) {
        if (!PAGE_TIMELINE.equals(currentPage)) return;
        JSONArray items = itemsArray(root);
        list.removeAllViews();
        int count = items == null ? 0 : items.length();
        info.setText("Quelle: " + host(base) + "\nEintraege: " + count + "\nSortierung: neueste zuerst");
        if (count == 0) {
            list.addView(logBox("Noch keine Zeitstrahl-Eintraege. Nachrichten koennen ueber 'Zeitstrahl' markiert werden."));
            return;
        }
        int max = Math.min(count, 80);
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String time = item.optString("timeline_time", item.optString("event_time", item.optString("created_at", "")));
            String actor = item.optString("actor", "Nexi");
            String topic = item.optString("topic", item.optString("event_type", "Ereignis"));
            String summary = item.optString("summary", item.optString("title", item.optString("body_preview", "")));
            String cause = item.optString("cause", "");
            String consequence = item.optString("consequence", "");
            String status = item.optString("status", "active");

            LinearLayout card = miniCard();
            card.addView(label(cut(time, 19) + " | " + topic, 14, true, Color.WHITE));
            card.addView(label(actor + " | Status: " + status, 11, true, orange()));
            card.addView(label(cut(summary, 360), 13, false, Color.rgb(232, 226, 216)));
            if (!cause.isEmpty()) card.addView(label("Ursache: " + cut(cause, 220), 11, false, sub()));
            if (!consequence.isEmpty()) card.addView(label("Folge: " + cut(consequence, 220), 11, false, sub()));
            list.addView(card, card(8));
        }
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
        row(p, nav("Server speichern", v -> NexusConfig.setEndpoint(this, endpointInput.getText().toString())), nav("Verbindung pruefen", v -> testConnection()));
        row(p, nav("LAN 192.168", v -> { endpointInput.setText("http://192.168.1.216:8081"); NexusConfig.setEndpoint(this, endpointInput.getText().toString()); }), nav("Tailscale 100", v -> { endpointInput.setText("http://100.107.24.67:8081"); NexusConfig.setEndpoint(this, endpointInput.getText().toString()); }));
        row(p, nav("Benachrichtigungen", v -> openNotificationAccess()), nav("SMS erlauben", v -> requestSms()));
        row(p, nav("Testevent", v -> sendTestEvent()), nav("Outbox senden", v -> NexusEventSender.retryOutbox(this)));
    }

    private void showWebPage(String path) {
        clearPage(PAGE_WEB, "Nexus Web", "Bestehendes Web-Cockpit innerhalb der App.");
        LinearLayout p = activePanel();
        row(p, nav("Cockpit", v -> loadWeb("/")), nav("Kommunikation", v -> loadWeb("/communication")));
        row(p, nav("Dateien", v -> loadWeb("/files")), nav("Nexi", v -> loadWeb("/chef")));
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
        chefLog.setText("Lade Nexi-Kanal...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    JSONObject root = new JSONObject(httpGet(base + "/api/communication/chef-log"));
                    setNexyBridgeBase(base);
                    String text = renderChefLog(root);
                    runOnUiThread(() -> { if (chefLog != null) chefLog.setText(text); });
                    return;
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> { if (chefLog != null) chefLog.setText("Nexi-Kanal nicht erreichbar: " + err); });
        }).start();
    }

    private String renderChefLog(JSONObject root) {
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) return "Noch kein Nexi-Kanal-Verlauf.";
        StringBuilder sb = new StringBuilder();
        boolean tokenErrorShown = false;
        int shown = 0;
        for (int i = Math.max(0, items.length() - 14); i < items.length(); i++) {
            JSONObject it = items.optJSONObject(i);
            if (it == null) continue;
            String text = cut(it.optString("text", it.optString("content", "")), 420);
            if (text.toLowerCase(Locale.ROOT).contains("max_output" + "_tokens")) {
                if (!tokenErrorShown) {
                    sb.append("SYSTEM: Alte Serverantwort war abgeschnitten. Nexi-Kanal ist auf die lokale Bridge umgestellt.\n\n");
                    tokenErrorShown = true;
                    shown++;
                }
                continue;
            }
            sb.append(it.optString("role", "nexi").toUpperCase(Locale.ROOT)).append(": ").append(text).append("\n\n");
            shown++;
        }
        return shown == 0 ? "Keine brauchbaren Nexi-Nachrichten im Verlauf." : sb.toString().trim();
    }

    private void sendChef() {
        if (chefInput == null || chefLog == null) return;
        String prompt = chefInput.getText().toString().trim();
        if (prompt.isEmpty()) { chefLog.setText("Schreib zuerst eine Nachricht an Nexi."); return; }
        chefLog.setText("Sende an Nexi...");
        new Thread(() -> {
            String last = "";
            for (String base : NexusConfig.nexiBaseUrlCandidates(this)) {
                try {
                    JSONObject res = new JSONObject(httpPost(base + "/api/mobile/chef-chat", "prompt=" + enc(prompt)));
                    if (res.optBoolean("ok", false)) {
                        setNexyBridgeBase(base);
                        runOnUiThread(() -> { chefInput.setText(""); chefLog.setText(res.optString("message", "Nexi-Auftrag gesendet.")); loadChefLog(); });
                        return;
                    }
                    last = host(base) + ": " + res.optString("message", "ok=false");
                } catch (Exception ex) { last = host(base) + ": " + ex.getClass().getSimpleName(); }
            }
            final String err = last;
            runOnUiThread(() -> chefLog.setText("Nexi-Chat fehlgeschlagen: " + err));
        }).start();
    }

    private String accessText() {
        return "Server: " + NexusConfig.baseUrl(this) + "\nBenachrichtigungszugriff: " + (notificationAccess() ? "aktiv" : "fehlt") + " | SMS: " + (smsPermission() ? "aktiv" : "fehlt") + "\nRechte jetzt im Android-System sichtbar: NotificationListener + SMS Receiver sind im Manifest registriert.";
    }

    private String shortSystemLine() {
        return "Server " + host(NexusConfig.baseUrl(this))
                + " | Notify " + (notificationAccess() ? "ok" : "fehlt")
                + " | SMS " + (smsPermission() ? "ok" : "fehlt")
                + " | Outbox " + NexusEventSender.outboxEvents(this);
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
                + "Digi Dragon: separat | Lokal " + dragonStage() + " L" + dragonLevel() + "\n"
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
    private boolean notificationAccess() { String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners"); return enabled != null && enabled.toLowerCase(Locale.ROOT).contains(getPackageName().toLowerCase(Locale.ROOT)); }
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
    private LinearLayout panel() { LinearLayout l = vertical(); l.setPadding(dp(10), dp(10), dp(10), dp(10)); l.setBackground(box(18, panelFill(), accentDark())); l.setElevation(dp(7)); return l; }
    private LinearLayout miniCard() { LinearLayout l = vertical(); l.setPadding(dp(9), dp(8), dp(9), dp(8)); l.setBackground(box(15, miniFill(), accentDark())); l.setElevation(dp(3)); return l; }
    private TextView section(String s) { return label(s, 12, true, orange()); }
    private TextView label(String s, int sp, boolean bold, int color) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.START); v.setPadding(0, dp(4), 0, dp(5)); if (bold) v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    private TextView logoLabel(String s) {
        TextView v = new TextView(this) {
            private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override protected void onDraw(Canvas canvas) {
                String text = getText() == null ? "" : getText().toString();
                if (text.isEmpty()) return;

                logoPaint.setTypeface(Typeface.DEFAULT_BOLD);
                logoPaint.setTextSize(getTextSize());
                logoPaint.setTextAlign(Paint.Align.LEFT);
                float textWidth = logoPaint.measureText(text);
                float x = Math.max(dp(2), (getWidth() - textWidth) / 2f);
                Paint.FontMetrics fm = logoPaint.getFontMetrics();
                float y = (getHeight() - fm.ascent - fm.descent) / 2f;

                int depth = dp(6);
                logoPaint.setStyle(Paint.Style.FILL);
                for (int i = depth; i >= 1; i--) {
                    int shade = 76 + i * 14;
                    logoPaint.setColor(Color.rgb(clamp(shade, 0, 185), clamp(shade, 0, 185), clamp(shade, 0, 185)));
                    canvas.drawText(text, x + i, y - i, logoPaint);
                    logoPaint.setColor(Color.rgb(38, 39, 40));
                    canvas.drawText(text, x + i, y + i, logoPaint);
                }

                logoPaint.setStyle(Paint.Style.STROKE);
                logoPaint.setStrokeWidth(dp(3));
                logoPaint.setColor(Color.rgb(205, 207, 202));
                canvas.drawText(text, x, y, logoPaint);
                logoPaint.setStrokeWidth(dp(1));
                logoPaint.setColor(Color.rgb(24, 24, 24));
                canvas.drawText(text, x, y, logoPaint);

                logoPaint.setStyle(Paint.Style.FILL);
                logoPaint.setShader(new LinearGradient(x, y - getTextSize(), x, y,
                        shade(accentText(), 1.35f), shade(accentText(), 0.72f), Shader.TileMode.CLAMP));
                canvas.drawText(text, x, y, logoPaint);
                logoPaint.setShader(null);

                logoPaint.setColor(Color.argb(120, 255, 255, 255));
                canvas.drawText(text, x - dp(1), y - dp(1), logoPaint);
            }
        };
        v.setText(s);
        v.setTextSize(34);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(60));
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }
    private Button nav(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(10); b.setTypeface(Typeface.DEFAULT_BOLD); b.setTextColor(buttonText()); b.setMinHeight(dp(30)); b.setMinimumHeight(0); b.setPadding(dp(4), 0, dp(4), 0); b.setBackground(box(12, accentStart(), accentEnd())); b.setOnClickListener(l); return b; }
    private boolean homeSlideOpen() {
        return NexusConfig.prefs(this).getBoolean("home_slide_open", false);
    }

    private void setHomeSlideOpen(boolean open) {
        NexusConfig.prefs(this).edit().putBoolean("home_slide_open", open).apply();
    }

    private boolean menuPetVisible() {
        return NexusConfig.prefs(this).getBoolean("menu_pet_visible", true);
    }

    private void toggleMenuPet() {
        NexusConfig.prefs(this).edit().putBoolean("menu_pet_visible", !menuPetVisible()).apply();
        setContentView(buildUi());
    }

    private TextView drawerHandleClean(boolean open) {
        TextView v = label(open ? "X" : ">", 16, true, accentText());
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, 0, 0, dp(2));
        v.setBackground(box(10, Color.rgb(7, 8, 8), accentDark()));
        v.setOnClickListener(view -> { setHomeSlideOpen(!homeSlideOpen()); showHome(); });
        return v;
    }

    private LinearLayout homeDrawerClean() {
        LinearLayout drawer = vertical();
        drawer.setPadding(dp(6), dp(6), dp(6), dp(6));
        drawer.setBackground(box(14, Color.rgb(6, 7, 7), accentDark()));
        drawer.setElevation(dp(10));
        drawer.addView(drawerItemClean("Schliessen", v -> { setHomeSlideOpen(false); showHome(); }));
        drawer.addView(drawerItemClean("Nexi", v -> showChefPage()));
        drawer.addView(drawerItemClean("Eingang", v -> showMessagesPage()));
        drawer.addView(drawerItemClean("Chronik", v -> showTimelinePage()));
        drawer.addView(drawerItemClean("Explorer", v -> showFilesPage()));
        drawer.addView(drawerItemClean("Dragon", v -> showDragonPage()));
        drawer.addView(drawerItemClean(menuPetVisible() ? "Pet aus" : "Pet an", v -> toggleMenuPet()));
        drawer.addView(drawerItemClean("DigiPad", v -> showDigiPadPage()));
        drawer.addView(drawerItemClean("Web", v -> showWebPage("/")));
        return drawer;
    }

    private TextView drawerItemClean(String title, View.OnClickListener listener) {
        TextView v = label(title, 11, true, Color.WHITE);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setMinHeight(dp(27));
        v.setPadding(dp(7), 0, dp(6), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        v.setLayoutParams(lp);
        v.setBackground(box(8, Color.rgb(12, 13, 13), accentDark()));
        v.setOnClickListener(listener);
        return v;
    }

    private LinearLayout quickPanel() {
        LinearLayout quick = vertical();
        quick.addView(label("SCHNELL", 10, true, accentText()));
        quickRow(quick, quickNav("Dragon", v -> showDragonPage()), quickNav("Verbindung", v -> testConnection()));
        quickRow(quick, quickNav("Widget", v -> { NexusMessagesWidgetProvider.updateAll(this); showHome(); }), quickNav("Status", v -> showStatusOnly()));
        quickRow(quick, quickNav("Neon", v -> setTheme("neon")), quickNav("OLED", v -> setTheme("oled")));
        return quick;
    }

    private void homeDeckTile(LinearLayout parent, String title, String tag, String detail, View.OnClickListener listener) {
        LinearLayout tile = miniCard();
        tile.setPadding(dp(12), dp(9), dp(12), dp(9));
        tile.setOnClickListener(listener);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        TextView left = label(title + "\n" + detail, 14, true, Color.WHITE);
        TextView right = label(tag, 11, true, orange());
        right.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        line.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(right, new LinearLayout.LayoutParams(dp(74), LinearLayout.LayoutParams.MATCH_PARENT));
        tile.addView(line);
        parent.addView(tile, card(7));
    }

    private void moduleTile(LinearLayout parent, String title, String tag, String detail, View.OnClickListener listener) {
        LinearLayout tile = miniCard();
        tile.setPadding(dp(12), dp(10), dp(12), dp(10));
        tile.setOnClickListener(listener);
        tile.addView(label(title, 16, true, Color.WHITE));
        tile.addView(label(tag, 11, true, orange()));
        tile.addView(label(detail, 12, false, Color.rgb(218, 212, 204)));
        parent.addView(tile, card(8));
    }
    private Button quickNav(String s, View.OnClickListener l) {
        Button b = nav(s, l);
        b.setTextSize(8);
        b.setMinHeight(dp(22));
        b.setMinimumHeight(0);
        b.setPadding(dp(3), 0, dp(3), 0);
        b.setTextColor(Color.rgb(230, 226, 218));
        b.setBackground(box(10, Color.rgb(9, 10, 10), accentDark()));
        return b;
    }
    private void quickRow(LinearLayout parent, Button a, Button b) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lp.setMargins(0, dp(3), dp(3), 0); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); rp.setMargins(dp(3), dp(3), 0, 0); r.addView(a, lp); r.addView(b, rp); parent.addView(r); }
    private View gap(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height))); return v; }
    private void row(LinearLayout parent, Button a) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, dp(4), 0, 0); parent.addView(a, lp); }
    private void row(LinearLayout parent, Button a, Button b) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); lp.setMargins(0, dp(4), dp(4), 0); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); rp.setMargins(dp(4), dp(4), 0, 0); r.addView(a, lp); r.addView(b, rp); parent.addView(r); }
    private LinearLayout.LayoutParams card(int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0, dp(top), 0, 0); return lp; }
    private GradientDrawable box(int radius, int fill, int stroke) { GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{fill, Color.rgb(4, 5, 5)}); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), stroke); return d; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private float lerp(float start, float end, float t) { return start + (end - start) * t; }
    private int orange() { return accentText(); }
    private int sub() { return Color.rgb(185, 178, 168); }

    private Bitmap transparentImageBackground(Bitmap source) {
        if (source == null) return null;
        Bitmap out = source.copy(Bitmap.Config.ARGB_8888, true);
        int width = out.getWidth();
        int height = out.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        boolean[] visited = new boolean[count];
        int[] stack = new int[count];
        int top = 0;
        out.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int x = 0; x < width; x++) {
            top = pushTransparentSeed(pixels, visited, stack, top, x, 0, width, height);
            top = pushTransparentSeed(pixels, visited, stack, top, x, height - 1, width, height);
        }
        for (int y = 0; y < height; y++) {
            top = pushTransparentSeed(pixels, visited, stack, top, 0, y, width, height);
            top = pushTransparentSeed(pixels, visited, stack, top, width - 1, y, width, height);
        }

        while (top > 0) {
            int idx = stack[--top];
            int x = idx % width;
            int y = idx / width;
            int c = pixels[idx];
            pixels[idx] = Color.argb(0, Color.red(c), Color.green(c), Color.blue(c));
            if (x > 0) top = pushTransparentSeed(pixels, visited, stack, top, x - 1, y, width, height);
            if (x < width - 1) top = pushTransparentSeed(pixels, visited, stack, top, x + 1, y, width, height);
            if (y > 0) top = pushTransparentSeed(pixels, visited, stack, top, x, y - 1, width, height);
            if (y < height - 1) top = pushTransparentSeed(pixels, visited, stack, top, x, y + 1, width, height);
        }

        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private Bitmap transparentLightPixels(Bitmap source) {
        if (source == null) return null;
        Bitmap out = source.copy(Bitmap.Config.ARGB_8888, true);
        int width = out.getWidth();
        int height = out.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        out.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < count; i++) {
            int c = pixels[i];
            int alpha = Color.alpha(c);
            if (alpha == 0) continue;
            int r = Color.red(c);
            int g = Color.green(c);
            int b = Color.blue(c);
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            int sum = r + g + b;
            boolean paleWhiteBox = sum > 520 && max - min < 92;
            boolean faintEdge = sum > 455 && max - min < 76 && alpha < 245;
            if (paleWhiteBox || faintEdge) {
                pixels[i] = Color.argb(0, r, g, b);
            }
        }

        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private Bitmap transparentPetPixels(Bitmap source) {
        if (source == null) return null;
        Bitmap out = source.copy(Bitmap.Config.ARGB_8888, true);
        int width = out.getWidth();
        int height = out.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        out.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < count; i++) {
            int c = pixels[i];
            int alpha = Color.alpha(c);
            if (alpha == 0) continue;
            int r = Color.red(c);
            int g = Color.green(c);
            int b = Color.blue(c);
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            int sum = r + g + b;
            boolean purplePixel = b > r + 14 && b > g + 10;
            boolean dragonDark = max < 118;
            boolean dragonInk = max < 154 && purplePixel;
            boolean whiteBox = r > 160 && g > 150 && b > 160;
            boolean paleLavender = r > 132 && g > 112 && b > 142 && max - min < 128;
            boolean lowContrastBright = sum > 420 && max - min < 150;
            boolean nearPaper = sum > 350 && max - min < 85;
            if (!dragonDark && !dragonInk) {
                pixels[i] = Color.argb(0, r, g, b);
            } else if (whiteBox || paleLavender || lowContrastBright || nearPaper) {
                pixels[i] = Color.argb(0, r, g, b);
            }
        }

        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private int pushTransparentSeed(int[] pixels, boolean[] visited, int[] stack, int top, int x, int y, int width, int height) {
        if (x < 0 || y < 0 || x >= width || y >= height) return top;
        int idx = y * width + x;
        if (visited[idx] || top >= stack.length) return top;
        visited[idx] = true;
        if (!isTransparentBackgroundPixel(pixels[idx])) return top;
        stack[top++] = idx;
        return top;
    }

    private boolean isTransparentBackgroundPixel(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int sum = r + g + b;
        return (r > 238 && g > 238 && b > 238)
                || (sum > 650 && max - min < 92)
                || (sum > 540 && max - min < 120)
                || (r > 198 && g > 178 && b > 205 && max - min < 116);
    }

    private String themeName() { return NexusConfig.prefs(this).getString("ui_theme", "orange"); }
    private void setTheme(String name) { NexusConfig.prefs(this).edit().putString("ui_theme", name == null ? "orange" : name).apply(); setContentView(buildUi()); }
    private int accentStart() { if (PAGE_DRAGON.equals(currentPage)) return shade(dragonElementAccent(), 1.18f); String t = themeName(); if ("blue".equals(t)) return Color.rgb(0, 192, 255); if ("green".equals(t) || "neon".equals(t)) return Color.rgb(0, 255, 102); if ("oled".equals(t)) return Color.rgb(0, 255, 176); return Color.rgb(255, 158, 38); }
    private int accentEnd() { if (PAGE_DRAGON.equals(currentPage)) return shade(dragonElementAccent(), 0.46f); String t = themeName(); if ("blue".equals(t)) return Color.rgb(0, 78, 255); if ("green".equals(t) || "neon".equals(t)) return Color.rgb(0, 130, 52); if ("oled".equals(t)) return Color.rgb(0, 52, 36); return Color.rgb(255, 180, 58); }
    private int accentText() { if (PAGE_DRAGON.equals(currentPage)) return shade(dragonElementAccent(), 1.22f); String t = themeName(); if ("blue".equals(t)) return Color.rgb(0, 192, 255); if ("green".equals(t) || "neon".equals(t)) return Color.rgb(0, 255, 102); if ("oled".equals(t)) return Color.rgb(0, 255, 176); return Color.rgb(255, 169, 54); }
    private int accentDark() { if (PAGE_DRAGON.equals(currentPage)) return shade(dragonElementAccent(), 0.38f); String t = themeName(); if ("blue".equals(t)) return Color.rgb(16, 53, 88); if ("green".equals(t) || "neon".equals(t)) return Color.rgb(12, 80, 38); if ("oled".equals(t)) return Color.rgb(0, 56, 38); return Color.rgb(72, 47, 31); }
    private int panelFill() { return "oled".equals(themeName()) ? Color.BLACK : Color.rgb(18, 19, 17); }
    private int miniFill() { return "oled".equals(themeName()) ? Color.rgb(0, 0, 0) : Color.rgb(11, 12, 11); }
    private int buttonText() { if (PAGE_DRAGON.equals(currentPage)) return Color.WHITE; String t = themeName(); return ("blue".equals(t) || "oled".equals(t)) ? Color.WHITE : Color.rgb(18, 13, 8); }
    private int dragonElementAccent() {
        String element = dragonElement();
        if ("Wasser".equals(element)) return Color.rgb(42, 205, 198);
        if ("Erde".equals(element)) return Color.rgb(120, 176, 83);
        if ("Luft".equals(element)) return Color.rgb(178, 229, 240);
        if ("Schatten".equals(element)) return Color.rgb(137, 74, 220);
        return Color.rgb(238, 100, 42);
    }
    private int shade(int color, float factor) { return Color.rgb(clamp((int) (Color.red(color) * factor), 0, 255), clamp((int) (Color.green(color) * factor), 0, 255), clamp((int) (Color.blue(color) * factor), 0, 255)); }

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
