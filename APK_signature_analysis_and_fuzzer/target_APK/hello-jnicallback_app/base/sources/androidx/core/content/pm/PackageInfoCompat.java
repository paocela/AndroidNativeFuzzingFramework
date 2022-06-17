package androidx.core.content.pm;

import android.content.pm.PackageInfo;
import android.os.Build;
/* loaded from: classes.dex */
public final class PackageInfoCompat {
    public static long getLongVersionCode(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            return info.getLongVersionCode();
        }
        return info.versionCode;
    }

    private PackageInfoCompat() {
    }
}
