package com.capacitor.push;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getcapacitor.JSObject;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VoIPCallActivity extends Activity {
    private static final String TAG = "VoIPCallActivity";
    private String sessionId, name, type, avatar;
    private JSObject voipData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_voip_call);
        TextView callerName = findViewById(R.id.caller_name);
        TextView callType = findViewById(R.id.caller_type);
        ImageView imageView = findViewById(R.id.caller_image);
        Button accept = findViewById(R.id.accept_button);
        Button reject = findViewById(R.id.reject_button);

        Intent intent = getIntent();
        String voipDataString = intent.getStringExtra("voipData");


        if (voipDataString != null) {
            try {
                voipData = JSObject.fromJSONObject(new JSONObject(voipDataString));
                sessionId = voipData.getString("sessionId", "unknown");
                name = voipData.getString("senderName", "unknown");
                type = voipData.getString("callType", "unknown");
                avatar = voipData.getString("senderAvatar", "unknown");

                callerName.setText(name);
                callType.setText(type + " Call");
//                imageView.setImageBitmap(getBitmapFromURL(avatar));
                // Your existing setup code...
                if (!"unknown".equals(avatar) && !TextUtils.isEmpty(avatar)) {
                    loadAvatarAsync(avatar, imageView);
                } else {
                    imageView.setImageResource(R.drawable.circle_background);
                }
                if (sessionId != null) {
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) nm.cancel(sessionId.hashCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error parsing VoIP data", e);
                finish();
            }
        } else {
            Log.e(TAG, "❌ No voipData in intent");
            finish();
        }

        accept.setOnClickListener(v -> {
            acceptCall();
        });
        reject.setOnClickListener(v -> {
            rejectCall();
        });

    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(callCancelledReceiver, new IntentFilter("com.capacitor.push.ACTION_CALL_CANCELLED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(callCancelledReceiver);
    }

    private final BroadcastReceiver callCancelledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String cancelledSessionId = intent.getStringExtra("sessionId");
            if (cancelledSessionId != null && cancelledSessionId.equals(sessionId)) {
                finish();
            }
        }
    };
    private void bringAppToForeground() {
        Intent mainIntent = getApplicationContext()
                .getPackageManager()
                .getLaunchIntentForPackage(getApplicationContext().getPackageName());
        if (mainIntent != null) {
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
        }
    }

    public void acceptCall() {
        Log.d(TAG, "✅ Call accepted in activity");
        bringAppToForeground();
        // Optionally, use a Handler to delay firing event by 200ms to ensure JS listeners have time to attach
        new android.os.Handler().postDelayed(() -> {
            CapacitorPushPlugin.sendVoipCallAcceptedEvent(sessionId, type);
//            VoIPCallReceiver.handleCallAction(sessionId, "accept");
            finish();
        }, 150); // 100-200ms delay is common
    }
    public void rejectCall() {
        Log.d(TAG, "❌ Call rejected");
        bringAppToForeground();
        new android.os.Handler().postDelayed(() -> {
            CapacitorPushPlugin.sendVoipCallDeclineEvent(sessionId);
//            VoIPCallReceiver.handleCallAction(sessionId, "reject");
            finish();
        }, 150);
    }
    private void loadAvatarAsync(String url, ImageView imageView) {
        new Thread(() -> {
            Bitmap bmp = getBitmapFromURL(url);
            runOnUiThread(() -> {
                if (bmp != null) {
                    imageView.setImageBitmap(bmp);
                } else {
                    imageView.setImageResource(R.drawable.circle_background);
                }
            });
        }).start();
    }
    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🧹 VoIP call activity destroyed");
    }
}

