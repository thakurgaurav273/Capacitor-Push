public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("FCM", "Message received: " + remoteMessage.getData());

        // Forward to your plugin (if app is open)
        if (getPlugin() instanceof CapacitorPushPlugin) {
            ((CapacitorPushPlugin) getPlugin()).handleNotificationReceived(remoteMessage);
        }

        // If call type notification, trigger full-screen intent
        if (remoteMessage.getData().containsKey("callType")) {
            showIncomingCallNotification(remoteMessage);
        }
    }

    private void showIncomingCallNotification(RemoteMessage remoteMessage) {
        // Build a high-priority notification with fullScreenIntent
        // Launch an IncomingCallActivity to show UI
    }
}
