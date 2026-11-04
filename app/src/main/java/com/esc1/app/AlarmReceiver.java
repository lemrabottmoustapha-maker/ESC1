package com.esc1.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String title = in.getStringExtra("title");
        String key = in.getStringExtra("key");
        int code = in.getIntExtra("code", 0);
        Notifs.channel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, code, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, Notifs.CH)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Retour prévu aujourd’hui")
            .setContentText(title == null ? "" : title)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        try {
            ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(code, n);
        } catch (Exception e) {
            // permission de notification refusée
        }
        if (key != null) {
            SharedPreferences sp = c.getSharedPreferences(Notifs.PREFS, Context.MODE_PRIVATE);
            Set<String> fired = new HashSet<String>(sp.getStringSet("fired", new HashSet<String>()));
            fired.add(key);
            sp.edit().putStringSet("fired", fired).apply();
        }
    }
}
