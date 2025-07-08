package com.capacitor.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class CapacitorPush {
    private static final String TAG = "CapacitorPush";
    private static final String PREFS_NAME = "CapacitorPushPrefs";
    private static final String VOIP_ENABLED_KEY = "voip_enabled";

    private Context context;
    private CapacitorPushPlugin plugin;
    private SharedPreferences prefs;

    public CapacitorPush(Context context, CapacitorPushPlugin plugin) {
        this.context = context;
        this.plugin = plugin;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setVoIPEnabled(boolean enabled) {
        prefs.edit().putBoolean(VOIP_ENABLED_KEY, enabled).apply();
        Log.d(TAG, "VoIP enabled: " + enabled);
    }

    public boolean isVoIPEnabled() {
        return prefs.getBoolean(VOIP_ENABLED_KEY, false);
    }

    public String echo(String value) {
        Log.i(TAG, "Echo called with: " + value);
        return value;
    }
}