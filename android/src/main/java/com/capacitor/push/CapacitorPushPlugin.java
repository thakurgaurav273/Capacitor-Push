package com.capacitor.push;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "CapacitorPush")
public class CapacitorPushPlugin extends Plugin {

    private CapacitorPush implementation = new CapacitorPush();

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }
     private static final String TAG = "PushNotificationPlugin";
    @PluginMethod
    public void register(PluginCall call) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        call.reject("Failed to get FCM token");
                        return;
                    }
                    
                    String token = task.getResult();
                    JSObject tokenObject = new JSObject();
                    tokenObject.put("token", token);
                    
                    notifyListeners("registration", tokenObject);
                    call.resolve(tokenObject);
                });
    }
       @PluginMethod
    public void getToken(PluginCall call) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        call.reject("Failed to get FCM token");
                        return;
                    }
                    
                    String token = task.getResult();
                    JSObject tokenObject = new JSObject();
                    tokenObject.put("token", token);
                    call.resolve(tokenObject);
                });
    }
    
        @PluginMethod
    public void enableVoIP(PluginCall call) {
        // VoIP not available on Android
        call.resolve();
    }
    
    @PluginMethod
    public void setBadgeCount(PluginCall call) {
        // Badge count handling for Android
        call.resolve();
    }
    
    @PluginMethod
    public void requestPermissions(PluginCall call) {
        JSObject permissions = new JSObject();
        permissions.put("receive", "granted");
        call.resolve(permissions);
    }
    
      @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject permissions = new JSObject();
        permissions.put("receive", "granted");
        call.resolve(permissions);
    }
    
    public void handleNotificationReceived(RemoteMessage remoteMessage) {
        JSObject notification = new JSObject();
        notification.put("id", remoteMessage.getMessageId());
        notification.put("title", remoteMessage.getNotification().getTitle());
        notification.put("body", remoteMessage.getNotification().getBody());
        notification.put("data", new JSObject(remoteMessage.getData()));
        
        notifyListeners("pushNotificationReceived", notification);
    }
    
    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }
}
