package com.capacitor.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * CapacitorPush
 * Manages plugin-level settings and state for CapacitorPush plugin,
 * such as enabling or disabling VoIP features, with persistent storage.
 */
public class CapacitorPush {
    private static final String TAG = "CapacitorPush";
    // SharedPreferences file name for storing plugin preferences
    private static final String PREFS_NAME = "CapacitorPushPrefs";
    // Key for storing the VoIP enabled flag
    private static final String VOIP_ENABLED_KEY = "voip_enabled";

    private Context context;
    private CapacitorPushPlugin plugin;
    private SharedPreferences prefs;

    /**
     * Constructs CapacitorPush with context and plugin reference.
     * Initializes SharedPreferences instance for persisting plugin settings.
     *
     * @param context Android context, used for SharedPreferences and other operations
     * @param plugin  Reference to the CapacitorPushPlugin instance for communication
     */
    public CapacitorPush(Context context, CapacitorPushPlugin plugin) {
        this.context = context;
        this.plugin = plugin;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    /**
     * Persistently sets whether VoIP functionality is enabled or disabled.
     *
     * @param enabled Boolean flag indicating if VoIP is enabled
     */
    public void setVoIPEnabled(boolean enabled) {
        prefs.edit().putBoolean(VOIP_ENABLED_KEY, enabled).apply();
        Log.d(TAG, "VoIP enabled: " + enabled);
    }
    /**
     * Retrieves whether VoIP functionality is currently enabled.
     *
     * @return true if VoIP is enabled, false otherwise
     */
    public boolean isVoIPEnabled() {
        return prefs.getBoolean(VOIP_ENABLED_KEY, false);
    }
    /**
     * An example echo method that logs and returns the input value.
     * Useful for testing plugin method calls.
     *
     * @param value Input string to echo back
     * @return The same input value
     */
    public String echo(String value) {
        Log.i(TAG, "Echo called with: " + value);
        return value;
    }
}