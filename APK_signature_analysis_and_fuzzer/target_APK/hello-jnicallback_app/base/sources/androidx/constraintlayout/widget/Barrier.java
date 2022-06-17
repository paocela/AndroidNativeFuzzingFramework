package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
/* loaded from: classes.dex */
public class Barrier extends ConstraintHelper {
    public static final int BOTTOM = 3;
    public static final int END = 6;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int START = 5;
    public static final int TOP = 2;
    private androidx.constraintlayout.solver.widgets.Barrier mBarrier;
    private int mIndicatedType;
    private int mResolvedType;

    public Barrier(Context context) {
        super(context);
        super.setVisibility(8);
    }

    public Barrier(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(8);
    }

    public Barrier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(8);
    }

    public int getType() {
        return this.mIndicatedType;
    }

    public void setType(int type) {
        this.mIndicatedType = type;
        this.mResolvedType = type;
        if (Build.VERSION.SDK_INT < 17) {
            int i = this.mIndicatedType;
            if (i == 5) {
                this.mResolvedType = 0;
            } else if (i == 6) {
                this.mResolvedType = 1;
            }
        } else {
            Configuration config = getResources().getConfiguration();
            boolean isRtl = 1 == config.getLayoutDirection();
            if (isRtl) {
                int i2 = this.mIndicatedType;
                if (i2 == 5) {
                    this.mResolvedType = 1;
                } else if (i2 == 6) {
                    this.mResolvedType = 0;
                }
            } else {
                int i3 = this.mIndicatedType;
                if (i3 == 5) {
                    this.mResolvedType = 0;
                } else if (i3 == 6) {
                    this.mResolvedType = 1;
                }
            }
        }
        this.mBarrier.setBarrierType(this.mResolvedType);
    }

    @Override // androidx.constraintlayout.widget.ConstraintHelper
    public void init(AttributeSet attrs) {
        super.init(attrs);
        this.mBarrier = new androidx.constraintlayout.solver.widgets.Barrier();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_barrierDirection) {
                    setType(a.getInt(attr, 0));
                } else if (attr == R.styleable.ConstraintLayout_Layout_barrierAllowsGoneWidgets) {
                    this.mBarrier.setAllowsGoneWidget(a.getBoolean(attr, true));
                }
            }
        }
        this.mHelperWidget = this.mBarrier;
        validateParams();
    }

    public void setAllowsGoneWidget(boolean supportGone) {
        this.mBarrier.setAllowsGoneWidget(supportGone);
    }

    public boolean allowsGoneWidget() {
        return this.mBarrier.allowsGoneWidget();
    }
}
