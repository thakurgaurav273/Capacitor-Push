package com.capacitor.push;

// Static imports for payload keys
import static com.capacitor.push.CapacitorPushPlugin.EVENT_NOTIFICATION_CLICKED;
import static com.capacitor.push.CapacitorPushPlugin.KEY_BODY;
import static com.capacitor.push.CapacitorPushPlugin.KEY_CALL_ACTION;
import static com.capacitor.push.CapacitorPushPlugin.KEY_CALL_TYPE;
import static com.capacitor.push.CapacitorPushPlugin.KEY_RECEIVER;
import static com.capacitor.push.CapacitorPushPlugin.KEY_RECEIVER_TYPE;
import static com.capacitor.push.CapacitorPushPlugin.KEY_SENDER;
import static com.capacitor.push.CapacitorPushPlugin.KEY_SENDER_AVATAR;
import static com.capacitor.push.CapacitorPushPlugin.KEY_SENDER_NAME;
import static com.capacitor.push.CapacitorPushPlugin.KEY_SESSION_ID;
import static com.capacitor.push.CapacitorPushPlugin.KEY_TAG;
import static com.capacitor.push.CapacitorPushPlugin.KEY_TITLE;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.RemoteMessage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Utility class for creating and displaying push notification UIs,
 * including handling incoming VOIP calls and regular message notifications.
 */
public class NotificationUtils {

    public static final String CHANNEL_MESSAGES = "messages_channel";
    /**
     * Download a Bitmap image from a URL.
     *
     * @param src The image URL.
     * @return Bitmap, or null on error.
     */
    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Show a native Android telecom call screen using TelecomManager.
     * Ensures phone account is enabled and call subject/number are registered in call logs.
     *
     * @param context   Context to use for Telecom APIs.
     * @param senderName Display name for caller.
     * @param sessionId Unique session ID for the call.
     * @param callType  Call type (audio/video).
     */
    public static void showNativeAndroidCallScreen(Context context, String senderName, String sessionId, String callType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PhoneAccountUtils.registerPhoneAccount(context);
                Bundle extras = new Bundle();
                extras.putString("senderName", senderName);
                extras.putString("sessionId", sessionId);
                extras.putString("callType", callType);

                // Format phone number for call logs; fallback to synthetic number if needed
                String phoneNumber = extractPhoneNumberFromName(senderName);
                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    int hash = Math.abs(senderName != null ? senderName.hashCode() : sessionId.hashCode());
                    phoneNumber = "+1" + String.format("%010d", hash);
                }

                Uri callerUri = Uri.fromParts(PhoneAccount.SCHEME_TEL, phoneNumber, null);
                extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, callerUri);

                // Set display name in call logs and telecom UI
                extras.putString(TelecomManager.EXTRA_CALL_SUBJECT, senderName);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    extras.putString("android.telecom.extra.CALLER_DISPLAY_NAME", senderName);
                }

                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                PhoneAccountHandle handle = PhoneAccountUtils.getPhoneAccountHandle(context);

                // Check if phone account enabled for incoming calls
                boolean enabled = false;
                try {
                    List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
                    for (PhoneAccountHandle h : accounts) {
                        if (PhoneAccountUtils.PHONE_ACCOUNT_ID.equals(h.getId())) {
                            enabled = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e("TAG", "Error checking phone accounts", e);
                }
                // Prompt user to enable phone account if needed
                if (!enabled) {
                    Log.w("TAG", "Phone account not enabled, opening settings");
                    Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return;
                }
                // Check if incoming calls are permitted on this account
                boolean permitted = true;
                try {
                    permitted = telecomManager.isIncomingCallPermitted(handle);
                } catch (Exception e) {
                    Log.e("TAG", "Error checking call permission", e);
                }
                // Trigger incoming call screen if permitted
                if (enabled && permitted) {
                    Log.d("TAG", "Adding incoming call for: " + senderName + " with number: " + phoneNumber);
                    telecomManager.addNewIncomingCall(handle, extras);
                } else {
                    Log.w("TAG", "Call not permitted - enabled: " + enabled + ", permitted: " + permitted);
                }
            }
        } catch (SecurityException e) {
            Log.e("TAG", "Security exception showing call screen", e);
        } catch (Exception e) {
            Log.e("TAG", "Error showing native call screen", e);
        }
    }
    /**
     * Helper method to extract a phone number from the sender name if present.
     *
     * @param senderName The sender's display name.
     * @return Formatted phone number string or null.
     */
    private static String extractPhoneNumberFromName(String senderName) {
        if (senderName == null) return null;

        // Extract phone number if present in name
        String phoneNumber = senderName.replaceAll("[^0-9+]", "");

        // Use only if number seems valid (at least 7 digits)
        if (phoneNumber.length() >= 7) {
            return phoneNumber.startsWith("+") ? phoneNumber : "+" + phoneNumber;
        }

        return null;
    }
    /**
     * Show incoming call UI when receiving a call payload via RemoteMessage.
     * Cancels notification and ends call if call has ended or was rejected/accepted.
     *
     * @param context      Context for UI and system services.
     * @param remoteMessage Payload with call details.
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static void showIncomingCallUI(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String sessionId = dataMap.get(KEY_SESSION_ID);
        String senderName = dataMap.get(KEY_SENDER_NAME);
        String callType = dataMap.get(KEY_CALL_TYPE);
        String senderAvatar = dataMap.get(KEY_SENDER_AVATAR);
        String callAction = dataMap.get(KEY_CALL_ACTION);

        if (sessionId == null || senderName == null) return;

        // Cancel notification & end the call for terminal states
        if ("cancelled".equalsIgnoreCase(callAction) ||
                "rejected".equalsIgnoreCase(callAction) ||
                "accepted".equalsIgnoreCase(callAction) ||
                "ongoing".equalsIgnoreCase(callAction) ||
                "ended".equalsIgnoreCase(callAction)) {

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(sessionId.hashCode());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MyConnectionService.endCall(sessionId);
            }
            return;
        }
        // Show the native call screen if compatible

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("TAG", "Showing native call screen for: " + senderName);
            showNativeAndroidCallScreen(context, senderName, sessionId, callType);
        }

    }
    /**
     * Show a standard message notification (not a call) for message payloads.
     * Handles avatar loading, click intents, and notification channel registration.
     *
     * @param context       Context for notification services.
     * @param remoteMessage Payload with message details.
     */
    public static void showTextNotificationUI(Context context, RemoteMessage remoteMessage) {
        Map<String, String> dataMap = remoteMessage.getData();
        String title = dataMap.get(KEY_TITLE);
        String body = dataMap.get(KEY_BODY);
        String senderAvatar = dataMap.get(KEY_SENDER_AVATAR);
        String receiverType = dataMap.get(KEY_RECEIVER_TYPE);
        String sender = dataMap.get(KEY_SENDER);
        String receiver = dataMap.get(KEY_RECEIVER);

        JSObject data = new JSObject();
        for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
        // send the pushnotification received event... 

        sendPushNotificationReceivedEvent(msgID, title, body, data);

        createMessageChannel(context);
        // Prepare intent for click/tap handling
        Intent tapIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assert tapIntent != null;
        // Identify conversation type and ID
        String id = "user".equalsIgnoreCase(receiverType) ? sender : receiver;
        String convType = "user".equalsIgnoreCase(receiverType) ? "user" : "group";

        tapIntent.setAction(EVENT_NOTIFICATION_CLICKED);
        tapIntent.putExtra("id", id);
        tapIntent.putExtra("convType", convType);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // Build notification UI, set avatar if available
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body != null ? body : "")
                .setSmallIcon(context.getApplicationInfo().icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (!TextUtils.isEmpty(senderAvatar)) {
            Bitmap avatar = getBitmapFromURL(senderAvatar);
            if (avatar != null) {
                builder.setLargeIcon(avatar);
            }
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_person));
        }
        // Show notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int msgId;
            try {
                msgId = Integer.parseInt(Objects.requireNonNull(dataMap.get(KEY_TAG)));
            } catch (Exception e) {
                msgId = new Random().nextInt();
            }
            manager.notify(msgId, builder.build());
        }
    }
    /**
     * Register a notification channel for message notifications on Android 13+.
     *
     * @param context The app context.
     */
    private static void createMessageChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_MESSAGES) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Notifications for incoming messages");

                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                manager.createNotificationChannel(channel);
            }
        }
    }
}
