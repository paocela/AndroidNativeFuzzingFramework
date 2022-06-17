package com.example.hellojnicallback;

import android.os.Build;
import android.util.Log;
/* loaded from: classes3.dex */
public class JniHandler {
    private void updateStatus(String msg) {
        if (msg.toLowerCase().contains("error")) {
            Log.e("JniHandler", "Native Err: " + msg);
        } else {
            Log.i("JniHandler", "Native Msg: " + msg);
        }
    }

    public static String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    public long getRuntimeMemorySize() {
        return Runtime.getRuntime().freeMemory();
    }
}
