package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.solver.widgets.Helper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.R;
import java.lang.reflect.Field;
import java.util.Arrays;
/* loaded from: classes.dex */
public abstract class ConstraintHelper extends View {
    protected int mCount;
    protected Helper mHelperWidget;
    protected int[] mIds;
    private String mReferenceIds;
    protected boolean mUseViewMeasure;
    protected Context myContext;

    public ConstraintHelper(Context context) {
        super(context);
        this.mIds = new int[32];
        this.mUseViewMeasure = false;
        this.myContext = context;
        init(null);
    }

    public ConstraintHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIds = new int[32];
        this.mUseViewMeasure = false;
        this.myContext = context;
        init(attrs);
    }

    public ConstraintHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mIds = new int[32];
        this.mUseViewMeasure = false;
        this.myContext = context;
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_constraint_referenced_ids) {
                    String string = a.getString(attr);
                    this.mReferenceIds = string;
                    setIds(string);
                }
            }
        }
    }

    public int[] getReferencedIds() {
        return Arrays.copyOf(this.mIds, this.mCount);
    }

    public void setReferencedIds(int[] ids) {
        this.mCount = 0;
        for (int i : ids) {
            setTag(i, null);
        }
    }

    @Override // android.view.View
    public void setTag(int tag, Object value) {
        int i = this.mCount + 1;
        int[] iArr = this.mIds;
        if (i > iArr.length) {
            this.mIds = Arrays.copyOf(iArr, iArr.length * 2);
        }
        int[] iArr2 = this.mIds;
        int i2 = this.mCount;
        iArr2[i2] = tag;
        this.mCount = i2 + 1;
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mUseViewMeasure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    public void validateParams() {
        if (this.mHelperWidget == null) {
            return;
        }
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) params;
            layoutParams.widget = this.mHelperWidget;
        }
    }

    private void addID(String idString) {
        if (idString == null || this.myContext == null) {
            return;
        }
        String idString2 = idString.trim();
        int tag = 0;
        try {
            Field field = R.id.class.getField(idString2);
            tag = field.getInt(null);
        } catch (Exception e) {
        }
        if (tag == 0) {
            tag = this.myContext.getResources().getIdentifier(idString2, "id", this.myContext.getPackageName());
        }
        if (tag == 0 && isInEditMode() && (getParent() instanceof ConstraintLayout)) {
            ConstraintLayout constraintLayout = (ConstraintLayout) getParent();
            Object value = constraintLayout.getDesignInformation(0, idString2);
            if (value != null && (value instanceof Integer)) {
                tag = ((Integer) value).intValue();
            }
        }
        if (tag != 0) {
            setTag(tag, null);
        } else {
            Log.w("ConstraintHelper", "Could not find id of \"" + idString2 + "\"");
        }
    }

    private void setIds(String idList) {
        if (idList == null) {
            return;
        }
        int begin = 0;
        while (true) {
            int end = idList.indexOf(44, begin);
            if (end == -1) {
                addID(idList.substring(begin));
                return;
            } else {
                addID(idList.substring(begin, end));
                begin = end + 1;
            }
        }
    }

    public void updatePreLayout(ConstraintLayout container) {
        if (isInEditMode()) {
            setIds(this.mReferenceIds);
        }
        Helper helper = this.mHelperWidget;
        if (helper == null) {
            return;
        }
        helper.removeAllIds();
        for (int i = 0; i < this.mCount; i++) {
            int id = this.mIds[i];
            View view = container.getViewById(id);
            if (view != null) {
                this.mHelperWidget.add(container.getViewWidget(view));
            }
        }
    }

    public void updatePostLayout(ConstraintLayout container) {
    }

    public void updatePostMeasure(ConstraintLayout container) {
    }
}
