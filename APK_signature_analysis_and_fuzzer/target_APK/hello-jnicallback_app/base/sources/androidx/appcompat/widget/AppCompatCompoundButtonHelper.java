package androidx.appcompat.widget;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;
/* loaded from: classes.dex */
class AppCompatCompoundButtonHelper {
    private ColorStateList mButtonTintList = null;
    private PorterDuff.Mode mButtonTintMode = null;
    private boolean mHasButtonTint = false;
    private boolean mHasButtonTintMode = false;
    private boolean mSkipNextApply;
    private final CompoundButton mView;

    /* loaded from: classes.dex */
    interface DirectSetButtonDrawableInterface {
        void setButtonDrawable(Drawable drawable);
    }

    public AppCompatCompoundButtonHelper(CompoundButton view) {
        this.mView = view;
    }

    public void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        int resourceId;
        TypedArray a = this.mView.getContext().obtainStyledAttributes(attrs, R.styleable.CompoundButton, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.CompoundButton_android_button) && (resourceId = a.getResourceId(R.styleable.CompoundButton_android_button, 0)) != 0) {
                CompoundButton compoundButton = this.mView;
                compoundButton.setButtonDrawable(AppCompatResources.getDrawable(compoundButton.getContext(), resourceId));
            }
            if (a.hasValue(R.styleable.CompoundButton_buttonTint)) {
                CompoundButtonCompat.setButtonTintList(this.mView, a.getColorStateList(R.styleable.CompoundButton_buttonTint));
            }
            if (a.hasValue(R.styleable.CompoundButton_buttonTintMode)) {
                CompoundButtonCompat.setButtonTintMode(this.mView, DrawableUtils.parseTintMode(a.getInt(R.styleable.CompoundButton_buttonTintMode, -1), null));
            }
        } finally {
            a.recycle();
        }
    }

    public void setSupportButtonTintList(ColorStateList tint) {
        this.mButtonTintList = tint;
        this.mHasButtonTint = true;
        applyButtonTint();
    }

    public ColorStateList getSupportButtonTintList() {
        return this.mButtonTintList;
    }

    public void setSupportButtonTintMode(PorterDuff.Mode tintMode) {
        this.mButtonTintMode = tintMode;
        this.mHasButtonTintMode = true;
        applyButtonTint();
    }

    public PorterDuff.Mode getSupportButtonTintMode() {
        return this.mButtonTintMode;
    }

    public void onSetButtonDrawable() {
        if (this.mSkipNextApply) {
            this.mSkipNextApply = false;
            return;
        }
        this.mSkipNextApply = true;
        applyButtonTint();
    }

    void applyButtonTint() {
        Drawable buttonDrawable = CompoundButtonCompat.getButtonDrawable(this.mView);
        if (buttonDrawable != null) {
            if (this.mHasButtonTint || this.mHasButtonTintMode) {
                Drawable buttonDrawable2 = DrawableCompat.wrap(buttonDrawable).mutate();
                if (this.mHasButtonTint) {
                    DrawableCompat.setTintList(buttonDrawable2, this.mButtonTintList);
                }
                if (this.mHasButtonTintMode) {
                    DrawableCompat.setTintMode(buttonDrawable2, this.mButtonTintMode);
                }
                if (buttonDrawable2.isStateful()) {
                    buttonDrawable2.setState(this.mView.getDrawableState());
                }
                this.mView.setButtonDrawable(buttonDrawable2);
            }
        }
    }

    public int getCompoundPaddingLeft(int superValue) {
        Drawable buttonDrawable;
        if (Build.VERSION.SDK_INT < 17 && (buttonDrawable = CompoundButtonCompat.getButtonDrawable(this.mView)) != null) {
            return superValue + buttonDrawable.getIntrinsicWidth();
        }
        return superValue;
    }
}
