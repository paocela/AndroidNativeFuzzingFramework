package androidx.core.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;
/* loaded from: classes.dex */
public class ImageViewCompat {
    public static ColorStateList getImageTintList(ImageView view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getImageTintList();
        }
        if (!(view instanceof TintableImageSourceView)) {
            return null;
        }
        return ((TintableImageSourceView) view).getSupportImageTintList();
    }

    public static void setImageTintList(ImageView view, ColorStateList tintList) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setImageTintList(tintList);
            if (Build.VERSION.SDK_INT == 21) {
                Drawable imageViewDrawable = view.getDrawable();
                boolean hasTint = (view.getImageTintList() == null || view.getImageTintMode() == null) ? false : true;
                if (imageViewDrawable != null && hasTint) {
                    if (imageViewDrawable.isStateful()) {
                        imageViewDrawable.setState(view.getDrawableState());
                    }
                    view.setImageDrawable(imageViewDrawable);
                }
            }
        } else if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintList(tintList);
        }
    }

    public static PorterDuff.Mode getImageTintMode(ImageView view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getImageTintMode();
        }
        if (!(view instanceof TintableImageSourceView)) {
            return null;
        }
        return ((TintableImageSourceView) view).getSupportImageTintMode();
    }

    public static void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setImageTintMode(mode);
            if (Build.VERSION.SDK_INT == 21) {
                Drawable imageViewDrawable = view.getDrawable();
                boolean hasTint = (view.getImageTintList() == null || view.getImageTintMode() == null) ? false : true;
                if (imageViewDrawable != null && hasTint) {
                    if (imageViewDrawable.isStateful()) {
                        imageViewDrawable.setState(view.getDrawableState());
                    }
                    view.setImageDrawable(imageViewDrawable);
                }
            }
        } else if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintMode(mode);
        }
    }

    private ImageViewCompat() {
    }
}
