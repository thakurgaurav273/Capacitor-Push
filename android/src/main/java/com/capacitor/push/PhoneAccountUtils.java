package com.capacitor.push;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class PhoneAccountUtils {
    public static final String PHONE_ACCOUNT_ID = "com.capacitor.push.ACCOUNT";

    public static PhoneAccountHandle getPhoneAccountHandle(Context context) {
        return new PhoneAccountHandle(
                new ComponentName(context, MyConnectionService.class),
                PHONE_ACCOUNT_ID
        );
    }

    public static void registerPhoneAccount(Context context) {
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        PhoneAccount phoneAccount = PhoneAccount.builder(
                        getPhoneAccountHandle(context),
                        "WhatsAppClone"
                )
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .build();

        telecomManager.registerPhoneAccount(phoneAccount);
    }
}
