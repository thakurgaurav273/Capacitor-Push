package com.capacitor.push;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
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

import java.util.List;
import java.util.Objects;

/**
 * CapacitorPushPlugin
 * Main plugin class for handling push notifications, VoIP, and related permissions.
 * Exposes methods for registering, permission management, and notification channel setup.
 */
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
    // Logging TAG and notification channel IDs
    private static final String TAG = "CapacitorPush";
    private static final String CHANNEL_ID = "capacitor_push_notifications";
    private static final String VOIP_CHANNEL_ID = "capacitor_voip_notifications";
    // Push implementation instance
    private CapacitorPush implementation;
    // Key mappings for push notification payload fields
    protected static String KEY_SESSION_ID = "sessionId";
    protected static String KEY_TYPE = "type";
    protected static String KEY_CALL_TYPE = "callType";
    protected static String KEY_CALL_ACTION = "callAction";
    protected static String KEY_SENDER_NAME = "senderName";
    protected static String KEY_TITLE = "title";
    protected static String KEY_BODY = "body";
    protected static String KEY_SENDER_AVATAR = "senderAvatar";
    protected static String KEY_RECEIVER_TYPE = "receiverType";
    protected static String KEY_SENDER = "sender";
    protected static String KEY_RECEIVER = "receiver";
    protected static String KEY_TAG = "tag";
    protected static String EVENT_NOTIFICATION_CLICKED = "NOTIFICATION_TAPPED";
    private PluginCall savedCall;


    /**
     * Dynamically sets key mappings used for parsing notification payloads.
     * Allows JS side to adjust field names used for sessionId, sender, title, etc.
     *
     * @param call Capacitor PluginCall containing mapping keys as strings.
     */

    @PluginMethod
    public void setKeyMappings(PluginCall call) {
        String sessionKey = call.getString("sessionKey");
        if (sessionKey != null && !sessionKey.isEmpty()) { KEY_SESSION_ID = sessionKey; }

        String senderKey = call.getString("senderKey");
        if (senderKey != null && !senderKey.isEmpty()) { KEY_SENDER_NAME = senderKey; }

        String titleKey = call.getString("titleKey");
        if (titleKey != null && !titleKey.isEmpty()) { KEY_TITLE = titleKey; }

        String bodyKey = call.getString("bodyKey");
        if (bodyKey != null && !bodyKey.isEmpty()) { KEY_BODY = bodyKey; }

        String senderAvatarKey = call.getString("senderAvatarKey");
        if (senderAvatarKey != null && !senderAvatarKey.isEmpty()) { KEY_SENDER_AVATAR = senderAvatarKey; }

        String receiverTypeKey = call.getString("receiverTypeKey");
        if (receiverTypeKey != null && !receiverTypeKey.isEmpty()) { KEY_RECEIVER_TYPE = receiverTypeKey; }

        String sender = call.getString("senderKey");
        if (sender != null && !sender.isEmpty()) { KEY_SENDER = sender; }

        String receiver = call.getString("receiverKey");
        if (receiver != null && !receiver.isEmpty()) { KEY_RECEIVER = receiver; }

        String tagKey = call.getString("tagKey");
        if (tagKey != null && !tagKey.isEmpty()) { KEY_TAG = tagKey; }

        JSObject result = new JSObject();
        result.put("sessionKey", KEY_SESSION_ID);
        result.put("senderKey", KEY_SENDER_NAME);
        result.put("titleKey", KEY_TITLE);
        result.put("bodyKey", KEY_BODY);
        result.put("senderAvatarKey", KEY_SENDER_AVATAR);
        result.put("receiverTypeKey", KEY_RECEIVER_TYPE);
        result.put("senderKey", KEY_SENDER);
        result.put("receiverKey", KEY_RECEIVER);
        result.put("tagKey", KEY_TAG);

        call.resolve(result);
    }

    /**
     * Called when the plugin is loaded by Capacitor.
     * Initializes Firebase, registers phone account, sets up notification channels, and connects bridge.
     */
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
    }
    // Singleton instance for plugin cross-callbacks
    private static CapacitorPushPlugin instance;
    public CapacitorPushPlugin() {
        instance = this;
    }
    /**
     * Dispatches event when a VoIP Call is accepted.
     *
     * @param sessionId The identifier for the call session.
     * @param type The call type or category.
     */
    public static void sendVoipCallAcceptedEvent(String sessionId, String type) {
        if (instance != null) {
            JSObject callAction = new JSObject();
            callAction.put(KEY_SESSION_ID, sessionId);
            callAction.put(KEY_TYPE, type);
            Log.d("TAG", "sendVoipCallAcceptedEvent: "+ type);
            instance.notifyListeners("voipCallAccepted", callAction, true); // true = deliver even if in background
        }
    }

    /**
     * Dispatches event when a VoIP Call is rejected.
     *
     * @param sessionId The identifier for the call session.
     */
    public static void sendVoipCallDeclineEvent(String sessionId) {
        if (instance != null) {
            JSObject callAction = new JSObject();
            callAction.put(KEY_SESSION_ID, sessionId);
            callAction.put(KEY_TYPE, "voip");
            instance.notifyListeners("voipCallRejected", callAction, true); // true = deliver even if in background
        }
    }

    /**
     * Dispatches event when a notification is received.
     *
     * @param id The identifier for the message.
     * @param title The identifier for the message.
     * @param body The identifier for the message.
     * @param data The identifier for the message.

     */
    public static void sendPushNotificationReceivedEvent(String id, String title, String body, JSObject data) {
        if (instance != null) {
            JSObject notification = new JSObject();
            notification.put("id", id);
            notification.put("title", title);
            notification.put("body", body);
            notification.put("data", data);
            instance.notifyListeners("pushNotificationReceived", notification, true);
        }
    }
    /**
     * Dispatches event when a push notification is clicked.
     *
     * @param id       Identifier linked to the notification (e.g. conversation ID).
     * @param convType Type of conversation ("user" or "group").
     */
    public static void sendNotificationClicked(String id, String convType){
        if (instance != null) {
            JSObject onTap = new JSObject();
            onTap.put("id", id);
            onTap.put("convType", convType);
            instance.notifyListeners("pushNotificationActionPerformed", onTap, true); // true = deliver even if in background
        }
    }
    /**
     * Initializes Firebase if not already initialized.
     */
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
    /**
     * Optionally fetches the initial FCM registration token to send to the JS side.
     * Invoked on UI thread.
     */
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

    /**
     * Creates required notification channels used for push and VoIP notifications (Android 13+).
     */
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    /**
     * Echo test method, returns input string to verify communication.
     *
     * @param call The PluginCall from JS with "value" property.
     */
    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }

    /**
     * Returns singleton instance of this plugin.
     *
     * @return Current CapacitorPushPlugin instance.
     */
    public static CapacitorPushPlugin getInstance() {
        return instance;
    }

    /**
     * Registers for push notifications and returns FCM token in callback.
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void register(PluginCall call) {
        Log.d(TAG, "Register method called");

        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            call.reject("Registration failed: " + Objects.requireNonNull(task.getException()).getMessage());
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
    /**
     * Flushes any pending notifications (stub method).
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void flushPending(PluginCall call) {
        // Implementation for flushing pending notifications
        call.resolve();
    }
    /**
     * Retrieves the current FCM token.
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void getToken(PluginCall call) {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Failed to get token", task.getException());
                            call.reject("Failed to get token: " + Objects.requireNonNull(task.getException()).getMessage());
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
    /**
     * Simulated method to get VoIP token on Android.
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void getVoIPToken(PluginCall call) {
        // VoIP token simulation for Android
        JSObject ret = new JSObject();
        ret.put("voipToken", "android_voip_token_simulation");
        call.resolve(ret);
    }

    /**
     * Enable or disable VoIP functionality.
     *
     * @param call The PluginCall from JS. Accepts boolean "enable".
     */
    @PluginMethod
    public void enableVoIP(PluginCall call) {
        Boolean enable = call.getBoolean("enable", true);
        implementation.setVoIPEnabled(Boolean.TRUE.equals(enable));
        call.resolve();
    }

    /**
     * Sets the badge count on app icon (Android requires additional libs).
     *
     * @param call The PluginCall from JS with "count" integer param.
     */
    @PluginMethod
    public void setBadgeCount(PluginCall call) {
        Integer count = call.getInt("count", 0);
        // Android doesn't have native badge count, but you can use ShortcutBadger library
        Log.d(TAG, "Badge count set to: " + count);
        call.resolve();
    }

    /**
     * Requests permission for notifications, phone, camera, and mic on Android 13+.
     * Resolves granted immediately on older versions.
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Store the call for the final callback
            this.savedCall = call;

            // Start the sequential permission request chain
            requestNotificationPermission();

        } else {
            JSObject ret = new JSObject();
            ret.put("receive", "granted");
            ret.put("phone", "granted");
            ret.put("cameraAndMic", "granted");
            call.resolve(ret);
        }
    }
    /**
     * First step: Request notification permissions
     */
    private void requestNotificationPermission() {
        requestPermissionForAlias("notifications", savedCall, "notificationPermissionCallback");
    }
    /**
     * Second step: Request phone permissions after notifications are handled
     */
    @PermissionCallback
    private void notificationPermissionCallback(PluginCall call) {
        Log.d(TAG, "Notification permission callback completed");
        // Don't resolve the call yet, continue to next permission
        requestPermissionForAlias("phone", savedCall, "phonePermissionCallback");
    }

    /**
     * Third step: Request camera and mic permissions after phone permissions are handled
     */
    @PermissionCallback
    private void phonePermissionCallback(PluginCall call) {
        Log.d(TAG, "Phone permission callback completed");
        // Don't resolve the call yet, continue to next permission
        requestPermissionForAlias("cameraAndMic", savedCall, "cameraAndMicPermissionCallback");
    }

    /**
     * Final step: Handle the last permission and check phone account
     */
    @PermissionCallback
    private void cameraAndMicPermissionCallback(PluginCall call) {
        Log.d(TAG, "Camera and mic permission callback completed");

        // All permissions requested, now check phone account
        new android.os.Handler().postDelayed(this::checkAndPromptPhoneAccount, 500);

        // Return the final permission states
        JSObject ret = new JSObject();
        String notificationPermission = getPermissionState("notifications").toString();
        String phonePermission = getPermissionState("phone").toString();
        String cameraAndMicPermission = getPermissionState("cameraAndMic").toString();

        ret.put("receive", notificationPermission);
        ret.put("phone", phonePermission);
        ret.put("cameraAndMic", cameraAndMicPermission);

        call.resolve(ret);

        // Clear the saved call
        savedCall = null;
    }

    /**
     * Checks if the app's phone account is enabled and prompts user to enable it if not.
     * This is required for VoIP functionality to work properly.
     */
    private void checkAndPromptPhoneAccount() {
        try {
            TelecomManager telecomManager = (TelecomManager) getContext().getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager == null) {
                Log.e(TAG, "TelecomManager is null");
                return;
            }

            boolean enabled = false;
            try {
                List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
                for (PhoneAccountHandle handle : accounts) {
                    if (PhoneAccountUtils.PHONE_ACCOUNT_ID.equals(handle.getId())) {
                        enabled = true;
                        break;
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException checking phone accounts - permission may not be granted yet", e);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error checking phone accounts", e);
                return;
            }

            if (!enabled) {
                // Get the activity context for showing the dialog
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            String appName = getContext().getApplicationInfo().loadLabel(getContext().getPackageManager()).toString();

                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Enable Call Account")
                                    .setMessage("To receive calls, please enable '" + appName +
                                            "' in the phone accounts settings.")
                                    .setPositiveButton("Open Settings", (dialog, which) -> {
                                        try {
                                            Intent settingsIntent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                                            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            getActivity().startActivity(settingsIntent);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to open phone account settings", e);
                                            // Fallback to general settings if specific intent fails
                                            try {
                                                Intent generalSettings = new Intent(android.provider.Settings.ACTION_SETTINGS);
                                                generalSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                getActivity().startActivity(generalSettings);
                                            } catch (Exception fallbackException) {
                                                Log.e(TAG, "Failed to open any settings", fallbackException);
                                            }
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .setCancelable(true)
                                    .show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error showing phone account dialog", e);
                        }
                    });
                } else {
                    Log.w(TAG, "Activity is null, cannot show phone account dialog");
                }
            } else {
                Log.d(TAG, "Phone account is already enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in checkAndPromptPhoneAccount", e);
        }
    }
    /**
     * Checks permission states for notifications.
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject ret = new JSObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String notificationPermission = getPermissionState("notifications").toString();
            String phonePermission = getPermissionState("phone").toString();
            String cameraAndMicPermission = getPermissionState("cameraAndMic").toString();

            ret.put("receive", notificationPermission);
            ret.put("phone", phonePermission);
            ret.put("cameraAndMic", cameraAndMicPermission);
        } else {
            ret.put("receive", "granted");
            ret.put("phone", "granted");
            ret.put("cameraAndMic", "granted");
        }

        call.resolve(ret);
    }    /**
     * Test method to verify VoIP setup availability in JS bridge.
     *
     * @param call The PluginCall from JS.
     */
    @PluginMethod
    public void testVoIPSetup(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("isVoIPSupported", true);
        ret.put("hasVoIPToken", true);
        ret.put("voipToken", "android_voip_token_simulation");
        call.resolve(ret);
    }

    /**
     * Handles token refresh event and notifies JS listeners.
     *
     * @param tokenData JSObject containing new token information.
     */
    public void handleTokenRefresh(JSObject tokenData) {
        Log.d(TAG, "Token refreshed: " + tokenData.toString());
        notifyListeners("registration", tokenData);
    }
}