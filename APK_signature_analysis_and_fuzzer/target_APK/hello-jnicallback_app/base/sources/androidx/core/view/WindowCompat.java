package androidx.core.view;

import android.os.Build;
import android.view.View;
import android.view.Window;
/* loaded from: classes.dex */
public final class WindowCompat {
    public static final int FEATURE_ACTION_BAR = 8;
    public static final int FEATURE_ACTION_BAR_OVERLAY = 9;
    public static final int FEATURE_ACTION_MODE_OVERLAY = 10;

    private WindowCompat() {
    }

    public static <T extends View> T requireViewById(Window window, int id) {
        if (Build.VERSION.SDK_INT >= 28) {
            return (T) window.requireViewById(id);
        }
        T view = (T) window.findViewById(id);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Window");
        }
        return view;
    }
}
