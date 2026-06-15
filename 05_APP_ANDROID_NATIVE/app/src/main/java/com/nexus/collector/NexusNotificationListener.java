package com.nexus.collector;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class NexusNotificationListener extends NotificationListenerService {
    private static final Set<String> PACKAGES = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "org.telegram.messenger",
            "org.thoughtcrime.securesms",
            "com.facebook.orca",
            "com.google.android.gm"
    ));

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !NexusConfig.enabled(this)) return;
        String pkg = sbn.getPackageName();
        if (!PACKAGES.contains(pkg)) return;
        Notification n = sbn.getNotification();
        if (n == null) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        CharSequence titleCs = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigTextCs = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence subTextCs = n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        String title = clean(titleCs);
        String body = clean(bigTextCs);
        if (body.isEmpty()) body = clean(textCs);
        if (body.isEmpty()) return;

        String appName = appName(pkg);
        String source = sourceName(pkg);
        String sender = title.isEmpty() ? appName : title;
        String channel = "com.google.android.gm".equals(pkg) ? "email" : "messenger";

        try {
            JSONObject event = NexusJson.baseEvent(NexusJson.iso(sbn.getPostTime()), source, channel, sender, title.isEmpty() ? appName : title, body);
            JSONObject visual = new JSONObject();
            visual.put("app_name", appName);
            visual.put("package", pkg);
            visual.put("notification_id", sbn.getId());
            visual.put("notification_tag", sbn.getTag() == null ? JSONObject.NULL : sbn.getTag());
            visual.put("notification_key", sbn.getKey());
            visual.put("notification_priority", n.priority);
            visual.put("notification_category", n.category == null ? JSONObject.NULL : n.category);
            visual.put("sub_text", subTextCs == null ? JSONObject.NULL : clean(subTextCs));
            visual.put("has_attachment_hint", body.toLowerCase().contains("bild") || body.toLowerCase().contains("foto") || body.toLowerCase().contains("datei"));
            event.put("visual", visual);
            NexusConfig.increment(this, "notification_count");
            NexusEventSender.sendAsync(this, event.toString());
        } catch (Exception ignored) {}
    }

    private static String clean(CharSequence value) {
        if (value == null) return "";
        return value.toString().replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String appName(String pkg) {
        if ("com.whatsapp".equals(pkg)) return "WhatsApp";
        if ("org.telegram.messenger".equals(pkg)) return "Telegram";
        if ("org.thoughtcrime.securesms".equals(pkg)) return "Signal";
        if ("com.facebook.orca".equals(pkg)) return "Messenger";
        if ("com.google.android.gm".equals(pkg)) return "Gmail";
        return pkg;
    }

    private static String sourceName(String pkg) { return appName(pkg); }
}
