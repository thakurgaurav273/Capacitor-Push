package com.capacitor.push;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import androidx.annotation.RequiresApi;

/**
 * PhoneAccountUtils
 * Utility class for managing Telecom PhoneAccount used for custom VoIP call integration,
 * enabling the app to appear as a call provider in the Android dialer and call logs.
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU) // Ensures these APIs are only available on Android 13+
public class PhoneAccountUtils {
    // Unique identifier for the custom PhoneAccount
    public static final String PHONE_ACCOUNT_ID = "com.capacitor.push.ACCOUNT";
    /**
     * Builds and returns a PhoneAccountHandle for the custom connection service.
     *
     * @param context The application context
     * @return PhoneAccountHandle associated with MyConnectionService and the defined account ID
     */
    public static PhoneAccountHandle getPhoneAccountHandle(Context context) {
        return new PhoneAccountHandle(
                new ComponentName(context, MyConnectionService.class),
                PHONE_ACCOUNT_ID  // Unique PhoneAccount ID
        );
    }
    /**
     * Registers the custom PhoneAccount with the Android TelecomManager
     * so app can handle and place VoIP calls like a native dialer.
     *
     * @param context The application context
     */
    public static void registerPhoneAccount(Context context) {
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

        // Create an icon to display for the PhoneAccount in system UI
        Icon accountIcon = Icon.createWithResource(context, R.drawable.ic_whatsapp_small);
        // Build PhoneAccount with required capabilities and metadata
        PhoneAccount phoneAccount = PhoneAccount.builder(
                        getPhoneAccountHandle(context),
                        "WhatsApp Clone" // Visible name in system phone UI
                )
                .setCapabilities(
                        PhoneAccount.CAPABILITY_CALL_PROVIDER | // Required for call provider
                                PhoneAccount.CAPABILITY_CONNECTION_MANAGER | // Allows managing calls
                                PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS // Allows emergency calls
                )
                .setIcon(accountIcon)  // Displayed in phone app and call logs
                .setShortDescription("VoIP Calls")  // Short summary shown in UI
                .setSupportedUriSchemes(java.util.Arrays.asList(
                        PhoneAccount.SCHEME_TEL,  // Standard tel: numbers
                        PhoneAccount.SCHEME_SIP  // SIP calls support
                ))
                .build();

        try {
            // Register the phone account with the telecom system
            telecomManager.registerPhoneAccount(phoneAccount);
        } catch (Exception e) {
            android.util.Log.e("PhoneAccountUtils", "Failed to register phone account", e);
        }
    }
}
