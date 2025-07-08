package com.capacitor.push;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getcapacitor.JSObject;

import org.json.JSONObject;

public class VoIPCallActivity extends Activity {
    private static final String TAG = "VoIPCallActivity";
    private String sessionId;
    private JSObject voipData;

    private TextView callerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLayout();

        Intent intent = getIntent();
        String voipDataString = intent.getStringExtra("voipData");

        if (voipDataString != null) {
            try {
                voipData = JSObject.fromJSONObject(new JSONObject(voipDataString));
                sessionId = voipData.getString("sessionId", "unknown");

                updateCallInfo();

            } catch (Exception e) {
                Log.e(TAG, "❌ Error parsing VoIP data", e);
                finish();
            }
        } else {
            Log.e(TAG, "❌ No voipData in intent");
            finish();
        }
    }

    private void createLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        layout.setBackgroundColor(Color.parseColor("#1E1E1E")); // Dark background

        callerInfo = new TextView(this);
        callerInfo.setText("Incoming Call...");
        callerInfo.setTextSize(24);
        callerInfo.setTextColor(Color.WHITE);
        callerInfo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, 0, 0, 100);
        callerInfo.setLayoutParams(textParams);
        layout.addView(callerInfo);

        // Button container
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(Gravity.CENTER);

        // Reject Button
        Button rejectButton = new Button(this);
        rejectButton.setText("Reject");
        rejectButton.setTextColor(Color.WHITE);
        rejectButton.setTextSize(16);
        styleButton(rejectButton, Color.parseColor("#F44336")); // Red
        rejectButton.setOnClickListener(v -> rejectCall());

        LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(
                200, 120
        );
        rejectParams.setMargins(0, 0, 50, 0);
        rejectButton.setLayoutParams(rejectParams);
        buttonContainer.addView(rejectButton);

        // Accept Button
        Button acceptButton = new Button(this);
        acceptButton.setText("Accept");
        acceptButton.setTextColor(Color.WHITE);
        acceptButton.setTextSize(16);
        styleButton(acceptButton, Color.parseColor("#4CAF50")); // Green
        acceptButton.setOnClickListener(v -> acceptCall());

        LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(
                200, 120
        );
        acceptParams.setMargins(50, 0, 0, 0);
        acceptButton.setLayoutParams(acceptParams);
        buttonContainer.addView(acceptButton);

        layout.addView(buttonContainer);
        setContentView(layout);
    }

    private void styleButton(Button button, int backgroundColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(25); // Rounded corners
        drawable.setStroke(3, Color.WHITE); // White border
        button.setBackground(drawable);
        button.setElevation(8); // Add shadow
    }

    private void updateCallInfo() {
        if (voipData != null) {
            String callerName = voipData.getString("senderName", "Unknown Caller");
            String callType = voipData.getString("callType", "audio");

            callerInfo.setText("Incoming " + callType + " call from\n" + callerName);
        }
    }

    public void acceptCall() {
        Log.d(TAG, "✅ Call accepted");
        VoIPCallReceiver.handleCallAction(sessionId, "accept");
        finish();
    }

    public void rejectCall() {
        Log.d(TAG, "❌ Call rejected");
        VoIPCallReceiver.handleCallAction(sessionId, "reject");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🧹 VoIP call activity destroyed");
    }
}

