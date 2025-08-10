package com.capacitor.push;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
@CapacitorPlugin(
        name = "CapacitorPush",
        permissions = {
                @Permission(
                        alias = "notifications",
                        strings = {
                                Manifest.permission.POST_NOTIFICATIONS,
                                Manifest.permission.WAKE_LOCK,
                                Manifest.permission.VIBRATE
                        }
                ),
                @Permission(
                        alias = "phone",
                        strings = {
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.MANAGE_OWN_CALLS,
                                Manifest.permission.ANSWER_PHONE_CALLS,
                                Manifest.permission.CALL_PHONE,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.SYSTEM_ALERT_WINDOW,
                                Manifest.permission.USE_FULL_SCREEN_INTENT
                        }
                ),
                @Permission(
                        alias = "cameraAndMic",  // new alias for camera + mic
                        strings = {
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                        }
                )
        }
)
public class CapacitorPushPlugin extends Plugin {
    private static final String TAG = "CapacitorPush";
    private static final String CHANNEL_ID = "capacitor_push_notifications";
    private static final String VOIP_CHANNEL_ID = "capacitor_voip_notifications";

    private CapacitorPush implementation;

    @Override
    public void load() {
        Log.d(TAG, "Loading CapacitorPush plugin");

        // Initialize Firebase first
        initializeFirebase();
        
        implementation = new CapacitorPush(getContext(), this);
        createNotificationChannels();

        // Set this plugin instance in the bridge
        VoIPCallReceiver.setPluginInstance(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PhoneAccountUtils.registerPhoneAccount(getContext());
            }

        // Get initial token after Firebase initialization
//        getInitialToken();
    }
//        following code provides the plugin instance to the app for launching from background and triggering listeners
    private static CapacitorPushPlugin instance;
    public CapacitorPushPlugin() {
        instance = this;
    }
    public static void sendVoipCallAcceptedEvent(String sessionId, String type) {
        if (instance != null) {
            JSObject callAction = new JSObject();
            callAction.put("sessionId", sessionId);
            callAction.put("type", type);
            Log.d("TAG", "sendVoipCallAcceptedEvent: "+ type);
            instance.notifyListeners("voipCallAccepted", callAction, true); // true = deliver even if in background
        }
    }

    public static void sendVoipCallDeclineEvent(String sessionId) {
        if (instance != null) {
            JSObject callAction = new JSObject();
            callAction.put("sessionId", sessionId);
            callAction.put("type", "voip");
            instance.notifyListeners("voipCallRejected", callAction, true); // true = deliver even if in background
        }
    }

    public static void sendNotificationClicked(String id, String convType){
        if (instance != null) {
            JSObject onTap = new JSObject();
            onTap.put("id", id);
            onTap.put("convType", convType);
            instance.notifyListeners("pushNotificationActionPerformed", onTap, true); // true = deliver even if in background
        }
    }
    private void initializeFirebase() {
        try {
            if (FirebaseApp.getApps(getContext()).isEmpty()) {
                FirebaseApp.initializeApp(getContext());
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }

    private void getInitialToken() {
        // Add a small delay to ensure Firebase is fully initialized
        getActivity().runOnUiThread(() -> {
            try {
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "Fetching initial FCM registration token failed", task.getException());
                                return;
                            }

                            String token = task.getResult();
                            if (token != null && !token.isEmpty()) {
                                Log.d(TAG, "Initial FCM Registration Token: " + token);
                                JSObject tokenData = new JSObject();
                                tokenData.put("token", token);
                                notifyListeners("registration", tokenData);
                            } else {
                                Log.w(TAG, "FCM token is null or empty");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get initial FCM token", e);
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception getting initial token", e);
            }
        });
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);

            // Regular notifications channel
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Push Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Push notifications from the app");
            notificationManager.createNotificationChannel(channel);

            // VoIP notifications channel
            NotificationChannel voipChannel = new NotificationChannel(
                    VOIP_CHANNEL_ID,
                    "VoIP Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            voipChannel.setDescription("Incoming VoIP calls");
            voipChannel.enableVibration(true);
            voipChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationManager.createNotificationChannel(voipChannel);
        }
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }

    public static CapacitorPushPlugin getInstance() {
        return instance;
    }

    @PluginMethod
    public void register(PluginCall call) {
        Log.d(TAG, "Register method called");

        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            call.reject("Registration failed: " + task.getException().getMessage());
                            return;
                        }

                        String token = task.getResult();
                        if (token != null && !token.isEmpty()) {
                            Log.d(TAG, "FCM Registration Token: " + token);

                            JSObject tokenData = new JSObject();
                            tokenData.put("token", token);
                            notifyListeners("registration", tokenData);

                            call.resolve(tokenData);
                        } else {
                            Log.w(TAG, "FCM token is null or empty");
                            call.reject("Token is null or empty");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get FCM token", e);
                        call.reject("Failed to get token: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in register method", e);
            call.reject("Exception: " + e.getMessage());
        }
    }

    @PluginMethod
    public void flushPending(PluginCall call) {
        // Implementation for flushing pending notifications
        call.resolve();
    }

    @PluginMethod
    public void getToken(PluginCall call) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Failed to get token", task.getException());
                            call.reject("Failed to get token: " + task.getException().getMessage());
                            return;
                        }

                        String token = task.getResult();
                        if (token != null && !token.isEmpty()) {
                            Log.d(TAG, "FCM Token retrieved: " + token);
                            JSObject ret = new JSObject();
                            ret.put("fcmToken", token);
                            ret.put("apnsToken", ""); // Not applicable for Android
                            call.resolve(ret);
                        } else {
                            Log.w(TAG, "FCM token is null or empty");
                            call.reject("Token is null or empty");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get FCM token", e);
                        call.reject("Failed to get token: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getToken method", e);
            call.reject("Exception: " + e.getMessage());
        }
    }

    @PluginMethod
    public void getVoIPToken(PluginCall call) {
        // VoIP token simulation for Android
        JSObject ret = new JSObject();
        ret.put("voipToken", "android_voip_token_simulation");
        call.resolve(ret);
    }

    @PluginMethod
    public void enableVoIP(PluginCall call) {
        Boolean enable = call.getBoolean("enable", true);
        implementation.setVoIPEnabled(enable);
        call.resolve();
    }

    @PluginMethod
    public void setBadgeCount(PluginCall call) {
        Integer count = call.getInt("count", 0);
        // Android doesn't have native badge count, but you can use ShortcutBadger library
        Log.d(TAG, "Badge count set to: " + count);
        call.resolve();
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionForAlias("notifications", call, "permissionCallback");
            requestPermissionForAlias("phone", call, "permissionCallback");

        } else {
            JSObject ret = new JSObject();
            ret.put("receive", "granted");
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = getPermissionState("notifications").toString();
            ret.put("receive", permission);
        } else {
            ret.put("receive", "granted");
        }

        call.resolve(ret);
    }

    @PluginMethod
    public void testVoIPSetup(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("isVoIPSupported", true);
        ret.put("hasVoIPToken", true);
        ret.put("voipToken", "android_voip_token_simulation");
        call.resolve(ret);
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = getPermissionState("notifications").toString();
            ret.put("receive", permission);
        } else {
            ret.put("receive", "granted");
        }

        call.resolve(ret);
    }

    // Method to handle VoIP call actions

    // PUBLIC METHOD FOR TOKEN REFRESH
    public void handleTokenRefresh(JSObject tokenData) {
        Log.d(TAG, "Token refreshed: " + tokenData.toString());
        notifyListeners("registration", tokenData);
    }
}