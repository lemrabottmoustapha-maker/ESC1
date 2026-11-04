package com.esc1.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class Notifs {
    static final String CH = "retours";
    static final String PREFS = "esc1";

    static void channel(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CH) == null) {
            nm.createNotificationChannel(new NotificationChannel(CH, "Retours prévus", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    /** Programme une alarme (8h00) pour chaque date de retour prévue. */
    static void scheduleAll(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        // annuler les anciennes alarmes
        Set<String> old = sp.getStringSet("codes", new HashSet<String>());
        for (String code : old) {
            Intent i = new Intent(c, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, Integer.parseInt(code), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) { am.cancel(pi); pi.cancel(); }
        }
        Set<String> codes = new HashSet<String>();
        Set<String> fired = new HashSet<String>(sp.getStringSet("fired", new HashSet<String>()));
        try {
            JSONArray arr = new JSONArray(sp.getString("alarms", "[]"));
            long now = System.currentTimeMillis();
            for (int k = 0; k < arr.length(); k++) {
                JSONObject o = arr.getJSONObject(k);
                String date = o.getString("date");
                String title = o.getString("title");
                String key = o.getString("id") + "|" + date;
                String[] p = date.split("-");
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 8, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long at = cal.getTimeInMillis();
                Calendar end = (Calendar) cal.clone();
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                if (end.getTimeInMillis() < now) continue;      // date passée
                if (fired.contains(key)) continue;              // déjà notifié
                if (at < now) at = now + 5000;                  // aujourd'hui, après 8h : tout de suite
                int code = key.hashCode();
                codes.add(String.valueOf(code));
                Intent i = new Intent(c, AlarmReceiver.class);
                i.putExtra("title", title);
                i.putExtra("key", key);
                i.putExtra("code", code);
                PendingIntent pi = PendingIntent.getBroadcast(c, code, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception e) {
            // données illisibles : ignorer
        }
        sp.edit().putStringSet("codes", codes).apply();
    }
}
