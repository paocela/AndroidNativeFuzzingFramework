package androidx.constraintlayout.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.ActivityChooserView;
import androidx.constraintlayout.solver.Metrics;
import androidx.constraintlayout.solver.widgets.Analyzer;
import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.solver.widgets.Guideline;
import androidx.constraintlayout.solver.widgets.ResolutionAnchor;
import androidx.core.internal.view.SupportMenu;
import androidx.core.view.ViewCompat;
import java.util.ArrayList;
import java.util.HashMap;
/* loaded from: classes.dex */
public class ConstraintLayout extends ViewGroup {
    static final boolean ALLOWS_EMBEDDED = false;
    private static final boolean CACHE_MEASURED_DIMENSION = false;
    private static final boolean DEBUG = false;
    public static final int DESIGN_INFO_ID = 0;
    private static final String TAG = "ConstraintLayout";
    private static final boolean USE_CONSTRAINTS_HELPER = true;
    public static final String VERSION = "ConstraintLayout-1.1.3";
    private Metrics mMetrics;
    SparseArray<View> mChildrenByIds = new SparseArray<>();
    private ArrayList<ConstraintHelper> mConstraintHelpers = new ArrayList<>(4);
    private final ArrayList<ConstraintWidget> mVariableDimensionsWidgets = new ArrayList<>(100);
    ConstraintWidgetContainer mLayoutWidget = new ConstraintWidgetContainer();
    private int mMinWidth = 0;
    private int mMinHeight = 0;
    private int mMaxWidth = ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    private int mMaxHeight = ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
    private boolean mDirtyHierarchy = USE_CONSTRAINTS_HELPER;
    private int mOptimizationLevel = 7;
    private ConstraintSet mConstraintSet = null;
    private int mConstraintSetId = -1;
    private HashMap<String, Integer> mDesignIds = new HashMap<>();
    private int mLastMeasureWidth = -1;
    private int mLastMeasureHeight = -1;
    int mLastMeasureWidthSize = -1;
    int mLastMeasureHeightSize = -1;
    int mLastMeasureWidthMode = 0;
    int mLastMeasureHeightMode = 0;

    public void setDesignInformation(int type, Object value1, Object value2) {
        if (type == 0 && (value1 instanceof String) && (value2 instanceof Integer)) {
            if (this.mDesignIds == null) {
                this.mDesignIds = new HashMap<>();
            }
            String name = (String) value1;
            int index = name.indexOf("/");
            if (index != -1) {
                name = name.substring(index + 1);
            }
            int id = ((Integer) value2).intValue();
            this.mDesignIds.put(name, Integer.valueOf(id));
        }
    }

    public Object getDesignInformation(int type, Object value) {
        if (type == 0 && (value instanceof String)) {
            String name = (String) value;
            HashMap<String, Integer> hashMap = this.mDesignIds;
            if (hashMap != null && hashMap.containsKey(name)) {
                return this.mDesignIds.get(name);
            }
            return null;
        }
        return null;
    }

    public ConstraintLayout(Context context) {
        super(context);
        init(null);
    }

    public ConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override // android.view.View
    public void setId(int id) {
        this.mChildrenByIds.remove(getId());
        super.setId(id);
        this.mChildrenByIds.put(getId(), this);
    }

    private void init(AttributeSet attrs) {
        this.mLayoutWidget.setCompanionWidget(this);
        this.mChildrenByIds.put(getId(), this);
        this.mConstraintSet = null;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_android_minWidth) {
                    this.mMinWidth = a.getDimensionPixelOffset(attr, this.mMinWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_minHeight) {
                    this.mMinHeight = a.getDimensionPixelOffset(attr, this.mMinHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxWidth) {
                    this.mMaxWidth = a.getDimensionPixelOffset(attr, this.mMaxWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxHeight) {
                    this.mMaxHeight = a.getDimensionPixelOffset(attr, this.mMaxHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_optimizationLevel) {
                    this.mOptimizationLevel = a.getInt(attr, this.mOptimizationLevel);
                } else if (attr == R.styleable.ConstraintLayout_Layout_constraintSet) {
                    int id = a.getResourceId(attr, 0);
                    try {
                        ConstraintSet constraintSet = new ConstraintSet();
                        this.mConstraintSet = constraintSet;
                        constraintSet.load(getContext(), id);
                    } catch (Resources.NotFoundException e) {
                        this.mConstraintSet = null;
                    }
                    this.mConstraintSetId = id;
                }
            }
            a.recycle();
        }
        this.mLayoutWidget.setOptimizationLevel(this.mOptimizationLevel);
    }

    @Override // android.view.ViewGroup
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (Build.VERSION.SDK_INT < 14) {
            onViewAdded(child);
        }
    }

    @Override // android.view.ViewGroup, android.view.ViewManager
    public void removeView(View view) {
        super.removeView(view);
        if (Build.VERSION.SDK_INT < 14) {
            onViewRemoved(view);
        }
    }

    @Override // android.view.ViewGroup
    public void onViewAdded(View view) {
        if (Build.VERSION.SDK_INT >= 14) {
            super.onViewAdded(view);
        }
        ConstraintWidget widget = getViewWidget(view);
        if ((view instanceof Guideline) && !(widget instanceof Guideline)) {
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            layoutParams.widget = new Guideline();
            layoutParams.isGuideline = USE_CONSTRAINTS_HELPER;
            ((Guideline) layoutParams.widget).setOrientation(layoutParams.orientation);
        }
        if (view instanceof ConstraintHelper) {
            ConstraintHelper helper = (ConstraintHelper) view;
            helper.validateParams();
            ((LayoutParams) view.getLayoutParams()).isHelper = USE_CONSTRAINTS_HELPER;
            if (!this.mConstraintHelpers.contains(helper)) {
                this.mConstraintHelpers.add(helper);
            }
        }
        this.mChildrenByIds.put(view.getId(), view);
        this.mDirtyHierarchy = USE_CONSTRAINTS_HELPER;
    }

    @Override // android.view.ViewGroup
    public void onViewRemoved(View view) {
        if (Build.VERSION.SDK_INT >= 14) {
            super.onViewRemoved(view);
        }
        this.mChildrenByIds.remove(view.getId());
        ConstraintWidget widget = getViewWidget(view);
        this.mLayoutWidget.remove(widget);
        this.mConstraintHelpers.remove(view);
        this.mVariableDimensionsWidgets.remove(widget);
        this.mDirtyHierarchy = USE_CONSTRAINTS_HELPER;
    }

    public void setMinWidth(int value) {
        if (value == this.mMinWidth) {
            return;
        }
        this.mMinWidth = value;
        requestLayout();
    }

    public void setMinHeight(int value) {
        if (value == this.mMinHeight) {
            return;
        }
        this.mMinHeight = value;
        requestLayout();
    }

    public int getMinWidth() {
        return this.mMinWidth;
    }

    public int getMinHeight() {
        return this.mMinHeight;
    }

    public void setMaxWidth(int value) {
        if (value == this.mMaxWidth) {
            return;
        }
        this.mMaxWidth = value;
        requestLayout();
    }

    public void setMaxHeight(int value) {
        if (value == this.mMaxHeight) {
            return;
        }
        this.mMaxHeight = value;
        requestLayout();
    }

    public int getMaxWidth() {
        return this.mMaxWidth;
    }

    public int getMaxHeight() {
        return this.mMaxHeight;
    }

    private void updateHierarchy() {
        int count = getChildCount();
        boolean recompute = false;
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            View child = getChildAt(i);
            if (!child.isLayoutRequested()) {
                i++;
            } else {
                recompute = USE_CONSTRAINTS_HELPER;
                break;
            }
        }
        if (recompute) {
            this.mVariableDimensionsWidgets.clear();
            setChildrenConstraints();
        }
    }

    private void setChildrenConstraints() {
        int helperCount;
        int count;
        int i;
        boolean z;
        int resolvedLeftToLeft;
        int resolveGoneLeftMargin;
        int resolvedRightToRight;
        int resolvedRightToLeft;
        int resolvedLeftToLeft2;
        LayoutParams layoutParams;
        int resolvedRightToLeft2;
        int resolvedLeftToLeft3;
        ConstraintWidget target;
        ConstraintWidget target2;
        ConstraintWidget target3;
        ConstraintWidget target4;
        int resolvedLeftToRight;
        int resolvedLeftToLeft4;
        int resolvedLeftToLeft5;
        int resolvedLeftToLeft6;
        boolean isInEditMode = isInEditMode();
        int count2 = getChildCount();
        boolean z2 = false;
        int i2 = -1;
        if (isInEditMode) {
            for (int i3 = 0; i3 < count2; i3++) {
                View view = getChildAt(i3);
                try {
                    String IdAsString = getResources().getResourceName(view.getId());
                    setDesignInformation(0, IdAsString, Integer.valueOf(view.getId()));
                    int slashIndex = IdAsString.indexOf(47);
                    if (slashIndex != -1) {
                        IdAsString = IdAsString.substring(slashIndex + 1);
                    }
                    getTargetWidget(view.getId()).setDebugName(IdAsString);
                } catch (Resources.NotFoundException e) {
                }
            }
        }
        for (int i4 = 0; i4 < count2; i4++) {
            ConstraintWidget widget = getViewWidget(getChildAt(i4));
            if (widget != null) {
                widget.reset();
            }
        }
        int i5 = this.mConstraintSetId;
        if (i5 != -1) {
            for (int i6 = 0; i6 < count2; i6++) {
                View child = getChildAt(i6);
                if (child.getId() == this.mConstraintSetId && (child instanceof Constraints)) {
                    this.mConstraintSet = ((Constraints) child).getConstraintSet();
                }
            }
        }
        ConstraintSet constraintSet = this.mConstraintSet;
        if (constraintSet != null) {
            constraintSet.applyToInternal(this);
        }
        this.mLayoutWidget.removeAllChildren();
        int helperCount2 = this.mConstraintHelpers.size();
        if (helperCount2 > 0) {
            for (int i7 = 0; i7 < helperCount2; i7++) {
                ConstraintHelper helper = this.mConstraintHelpers.get(i7);
                helper.updatePreLayout(this);
            }
        }
        for (int i8 = 0; i8 < count2; i8++) {
            View child2 = getChildAt(i8);
            if (child2 instanceof Placeholder) {
                ((Placeholder) child2).updatePreLayout(this);
            }
        }
        int i9 = 0;
        while (i9 < count2) {
            View child3 = getChildAt(i9);
            ConstraintWidget widget2 = getViewWidget(child3);
            if (widget2 == null) {
                count = count2;
                z = z2;
                i = i2;
                helperCount = helperCount2;
            } else {
                LayoutParams layoutParams2 = (LayoutParams) child3.getLayoutParams();
                layoutParams2.validate();
                if (layoutParams2.helped) {
                    layoutParams2.helped = z2;
                } else if (isInEditMode) {
                    try {
                        String IdAsString2 = getResources().getResourceName(child3.getId());
                        Object valueOf = Integer.valueOf(child3.getId());
                        int i10 = z2 ? 1 : 0;
                        int i11 = z2 ? 1 : 0;
                        setDesignInformation(i10, IdAsString2, valueOf);
                        getTargetWidget(child3.getId()).setDebugName(IdAsString2.substring(IdAsString2.indexOf("id/") + 3));
                    } catch (Resources.NotFoundException e2) {
                    }
                }
                widget2.setVisibility(child3.getVisibility());
                if (layoutParams2.isInPlaceholder) {
                    widget2.setVisibility(8);
                }
                widget2.setCompanionWidget(child3);
                this.mLayoutWidget.add(widget2);
                if (!layoutParams2.verticalDimensionFixed || !layoutParams2.horizontalDimensionFixed) {
                    this.mVariableDimensionsWidgets.add(widget2);
                }
                if (layoutParams2.isGuideline) {
                    Guideline guideline = (Guideline) widget2;
                    int resolvedGuideBegin = layoutParams2.resolvedGuideBegin;
                    int resolvedGuideEnd = layoutParams2.resolvedGuideEnd;
                    float resolvedGuidePercent = layoutParams2.resolvedGuidePercent;
                    if (Build.VERSION.SDK_INT < 17) {
                        resolvedGuideBegin = layoutParams2.guideBegin;
                        resolvedGuideEnd = layoutParams2.guideEnd;
                        resolvedGuidePercent = layoutParams2.guidePercent;
                    }
                    if (resolvedGuidePercent != -1.0f) {
                        guideline.setGuidePercent(resolvedGuidePercent);
                    } else if (resolvedGuideBegin != i2) {
                        guideline.setGuideBegin(resolvedGuideBegin);
                    } else if (resolvedGuideEnd != i2) {
                        guideline.setGuideEnd(resolvedGuideEnd);
                    }
                } else if (layoutParams2.leftToLeft != i2 || layoutParams2.leftToRight != i2 || layoutParams2.rightToLeft != i2 || layoutParams2.rightToRight != i2 || layoutParams2.startToStart != i2 || layoutParams2.startToEnd != i2 || layoutParams2.endToStart != i2 || layoutParams2.endToEnd != i2 || layoutParams2.topToTop != i2 || layoutParams2.topToBottom != i2 || layoutParams2.bottomToTop != i2 || layoutParams2.bottomToBottom != i2 || layoutParams2.baselineToBaseline != i2 || layoutParams2.editorAbsoluteX != i2 || layoutParams2.editorAbsoluteY != i2 || layoutParams2.circleConstraint != i2 || layoutParams2.width == i2 || layoutParams2.height == i2) {
                    int resolvedLeftToLeft7 = layoutParams2.resolvedLeftToLeft;
                    int resolvedLeftToRight2 = layoutParams2.resolvedLeftToRight;
                    int resolvedRightToLeft3 = layoutParams2.resolvedRightToLeft;
                    int resolvedRightToRight2 = layoutParams2.resolvedRightToRight;
                    int resolveGoneLeftMargin2 = layoutParams2.resolveGoneLeftMargin;
                    int resolveGoneRightMargin = layoutParams2.resolveGoneRightMargin;
                    float resolvedHorizontalBias = layoutParams2.resolvedHorizontalBias;
                    int resolvedLeftToLeft8 = Build.VERSION.SDK_INT;
                    if (resolvedLeftToLeft8 >= 17) {
                        resolvedLeftToLeft2 = resolvedLeftToLeft7;
                        resolvedLeftToLeft = resolveGoneLeftMargin2;
                        resolveGoneLeftMargin = resolvedLeftToRight2;
                        resolvedRightToRight = resolvedRightToLeft3;
                        resolvedRightToLeft = resolvedRightToRight2;
                    } else {
                        int resolvedLeftToLeft9 = layoutParams2.leftToLeft;
                        int resolvedLeftToRight3 = layoutParams2.leftToRight;
                        int resolvedRightToLeft4 = layoutParams2.rightToLeft;
                        int resolvedRightToRight3 = layoutParams2.rightToRight;
                        int resolveGoneLeftMargin3 = layoutParams2.goneLeftMargin;
                        resolveGoneRightMargin = layoutParams2.goneRightMargin;
                        resolvedHorizontalBias = layoutParams2.horizontalBias;
                        if (resolvedLeftToLeft9 != -1 || resolvedLeftToRight3 != -1) {
                            resolvedLeftToLeft6 = resolvedLeftToLeft9;
                        } else {
                            resolvedLeftToLeft6 = resolvedLeftToLeft9;
                            int resolvedLeftToLeft10 = layoutParams2.startToStart;
                            if (resolvedLeftToLeft10 != -1) {
                                resolvedLeftToLeft4 = layoutParams2.startToStart;
                                resolvedLeftToRight = resolvedLeftToRight3;
                            } else {
                                int resolvedLeftToLeft11 = layoutParams2.startToEnd;
                                if (resolvedLeftToLeft11 != -1) {
                                    resolvedLeftToRight = layoutParams2.startToEnd;
                                    resolvedLeftToLeft4 = resolvedLeftToLeft6;
                                }
                            }
                            if (resolvedRightToLeft4 == -1 || resolvedRightToRight3 != -1) {
                                resolvedLeftToLeft5 = resolvedLeftToLeft4;
                            } else {
                                resolvedLeftToLeft5 = resolvedLeftToLeft4;
                                int resolvedLeftToLeft12 = layoutParams2.endToStart;
                                if (resolvedLeftToLeft12 != -1) {
                                    int resolvedRightToLeft5 = layoutParams2.endToStart;
                                    resolvedLeftToLeft2 = resolvedLeftToLeft5;
                                    resolvedLeftToLeft = resolveGoneLeftMargin3;
                                    resolveGoneLeftMargin = resolvedLeftToRight;
                                    resolvedRightToRight = resolvedRightToLeft5;
                                    resolvedRightToLeft = resolvedRightToRight3;
                                } else if (layoutParams2.endToEnd != -1) {
                                    int resolvedRightToRight4 = layoutParams2.endToEnd;
                                    resolvedLeftToLeft2 = resolvedLeftToLeft5;
                                    resolvedLeftToLeft = resolveGoneLeftMargin3;
                                    resolveGoneLeftMargin = resolvedLeftToRight;
                                    resolvedRightToRight = resolvedRightToLeft4;
                                    resolvedRightToLeft = resolvedRightToRight4;
                                }
                            }
                            resolvedLeftToLeft2 = resolvedLeftToLeft5;
                            resolvedLeftToLeft = resolveGoneLeftMargin3;
                            resolveGoneLeftMargin = resolvedLeftToRight;
                            resolvedRightToRight = resolvedRightToLeft4;
                            resolvedRightToLeft = resolvedRightToRight3;
                        }
                        resolvedLeftToRight = resolvedLeftToRight3;
                        resolvedLeftToLeft4 = resolvedLeftToLeft6;
                        if (resolvedRightToLeft4 == -1) {
                        }
                        resolvedLeftToLeft5 = resolvedLeftToLeft4;
                        resolvedLeftToLeft2 = resolvedLeftToLeft5;
                        resolvedLeftToLeft = resolveGoneLeftMargin3;
                        resolveGoneLeftMargin = resolvedLeftToRight;
                        resolvedRightToRight = resolvedRightToLeft4;
                        resolvedRightToLeft = resolvedRightToRight3;
                    }
                    if (layoutParams2.circleConstraint != -1) {
                        ConstraintWidget target5 = getTargetWidget(layoutParams2.circleConstraint);
                        if (target5 == null) {
                            count = count2;
                        } else {
                            count = count2;
                            widget2.connectCircularConstraint(target5, layoutParams2.circleAngle, layoutParams2.circleRadius);
                        }
                        helperCount = helperCount2;
                        layoutParams = layoutParams2;
                    } else {
                        count = count2;
                        if (resolvedLeftToLeft2 != -1) {
                            ConstraintWidget target6 = getTargetWidget(resolvedLeftToLeft2);
                            if (target6 != null) {
                                resolvedLeftToLeft3 = resolvedRightToLeft;
                                resolvedRightToLeft2 = resolvedRightToRight;
                                helperCount = helperCount2;
                                layoutParams = layoutParams2;
                                widget2.immediateConnect(ConstraintAnchor.Type.LEFT, target6, ConstraintAnchor.Type.LEFT, layoutParams2.leftMargin, resolvedLeftToLeft);
                            } else {
                                helperCount = helperCount2;
                                resolvedLeftToLeft3 = resolvedRightToLeft;
                                resolvedRightToLeft2 = resolvedRightToRight;
                                layoutParams = layoutParams2;
                            }
                        } else {
                            helperCount = helperCount2;
                            resolvedLeftToLeft3 = resolvedRightToLeft;
                            resolvedRightToLeft2 = resolvedRightToRight;
                            int helperCount3 = resolveGoneLeftMargin;
                            layoutParams = layoutParams2;
                            if (helperCount3 != -1 && (target4 = getTargetWidget(helperCount3)) != null) {
                                widget2.immediateConnect(ConstraintAnchor.Type.LEFT, target4, ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin, resolvedLeftToLeft);
                            }
                        }
                        if (resolvedRightToLeft2 != -1) {
                            ConstraintWidget target7 = getTargetWidget(resolvedRightToLeft2);
                            if (target7 != null) {
                                widget2.immediateConnect(ConstraintAnchor.Type.RIGHT, target7, ConstraintAnchor.Type.LEFT, layoutParams.rightMargin, resolveGoneRightMargin);
                            }
                        } else if (resolvedLeftToLeft3 != -1 && (target3 = getTargetWidget(resolvedLeftToLeft3)) != null) {
                            widget2.immediateConnect(ConstraintAnchor.Type.RIGHT, target3, ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin, resolveGoneRightMargin);
                        }
                        if (layoutParams.topToTop != -1) {
                            ConstraintWidget target8 = getTargetWidget(layoutParams.topToTop);
                            if (target8 != null) {
                                widget2.immediateConnect(ConstraintAnchor.Type.TOP, target8, ConstraintAnchor.Type.TOP, layoutParams.topMargin, layoutParams.goneTopMargin);
                            }
                        } else if (layoutParams.topToBottom != -1 && (target2 = getTargetWidget(layoutParams.topToBottom)) != null) {
                            widget2.immediateConnect(ConstraintAnchor.Type.TOP, target2, ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin, layoutParams.goneTopMargin);
                        }
                        if (layoutParams.bottomToTop != -1) {
                            ConstraintWidget target9 = getTargetWidget(layoutParams.bottomToTop);
                            if (target9 != null) {
                                widget2.immediateConnect(ConstraintAnchor.Type.BOTTOM, target9, ConstraintAnchor.Type.TOP, layoutParams.bottomMargin, layoutParams.goneBottomMargin);
                            }
                        } else if (layoutParams.bottomToBottom != -1 && (target = getTargetWidget(layoutParams.bottomToBottom)) != null) {
                            widget2.immediateConnect(ConstraintAnchor.Type.BOTTOM, target, ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin, layoutParams.goneBottomMargin);
                        }
                        if (layoutParams.baselineToBaseline != -1) {
                            View view2 = this.mChildrenByIds.get(layoutParams.baselineToBaseline);
                            ConstraintWidget target10 = getTargetWidget(layoutParams.baselineToBaseline);
                            if (target10 != null && view2 != null && (view2.getLayoutParams() instanceof LayoutParams)) {
                                LayoutParams targetParams = (LayoutParams) view2.getLayoutParams();
                                layoutParams.needsBaseline = USE_CONSTRAINTS_HELPER;
                                targetParams.needsBaseline = USE_CONSTRAINTS_HELPER;
                                ConstraintAnchor baseline = widget2.getAnchor(ConstraintAnchor.Type.BASELINE);
                                ConstraintAnchor targetBaseline = target10.getAnchor(ConstraintAnchor.Type.BASELINE);
                                baseline.connect(targetBaseline, 0, -1, ConstraintAnchor.Strength.STRONG, 0, USE_CONSTRAINTS_HELPER);
                                widget2.getAnchor(ConstraintAnchor.Type.TOP).reset();
                                widget2.getAnchor(ConstraintAnchor.Type.BOTTOM).reset();
                            }
                        }
                        if (resolvedHorizontalBias >= 0.0f && resolvedHorizontalBias != 0.5f) {
                            widget2.setHorizontalBiasPercent(resolvedHorizontalBias);
                        }
                        if (layoutParams.verticalBias >= 0.0f && layoutParams.verticalBias != 0.5f) {
                            widget2.setVerticalBiasPercent(layoutParams.verticalBias);
                        }
                    }
                    if (isInEditMode && (layoutParams.editorAbsoluteX != -1 || layoutParams.editorAbsoluteY != -1)) {
                        widget2.setOrigin(layoutParams.editorAbsoluteX, layoutParams.editorAbsoluteY);
                    }
                    if (!layoutParams.horizontalDimensionFixed) {
                        if (layoutParams.width == -1) {
                            widget2.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
                            widget2.getAnchor(ConstraintAnchor.Type.LEFT).mMargin = layoutParams.leftMargin;
                            widget2.getAnchor(ConstraintAnchor.Type.RIGHT).mMargin = layoutParams.rightMargin;
                        } else {
                            widget2.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                            widget2.setWidth(0);
                        }
                    } else {
                        widget2.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                        widget2.setWidth(layoutParams.width);
                    }
                    if (!layoutParams.verticalDimensionFixed) {
                        i = -1;
                        if (layoutParams.height == -1) {
                            widget2.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
                            widget2.getAnchor(ConstraintAnchor.Type.TOP).mMargin = layoutParams.topMargin;
                            widget2.getAnchor(ConstraintAnchor.Type.BOTTOM).mMargin = layoutParams.bottomMargin;
                            z = false;
                        } else {
                            widget2.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                            z = false;
                            widget2.setHeight(0);
                        }
                    } else {
                        z = false;
                        i = -1;
                        widget2.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                        widget2.setHeight(layoutParams.height);
                    }
                    if (layoutParams.dimensionRatio != null) {
                        widget2.setDimensionRatio(layoutParams.dimensionRatio);
                    }
                    widget2.setHorizontalWeight(layoutParams.horizontalWeight);
                    widget2.setVerticalWeight(layoutParams.verticalWeight);
                    widget2.setHorizontalChainStyle(layoutParams.horizontalChainStyle);
                    widget2.setVerticalChainStyle(layoutParams.verticalChainStyle);
                    widget2.setHorizontalMatchStyle(layoutParams.matchConstraintDefaultWidth, layoutParams.matchConstraintMinWidth, layoutParams.matchConstraintMaxWidth, layoutParams.matchConstraintPercentWidth);
                    widget2.setVerticalMatchStyle(layoutParams.matchConstraintDefaultHeight, layoutParams.matchConstraintMinHeight, layoutParams.matchConstraintMaxHeight, layoutParams.matchConstraintPercentHeight);
                }
                count = count2;
                boolean z3 = z2 ? 1 : 0;
                Object[] objArr = z2 ? 1 : 0;
                z = z3;
                i = i2;
                helperCount = helperCount2;
            }
            i9++;
            z2 = z;
            i2 = i;
            count2 = count;
            helperCount2 = helperCount;
        }
    }

    private final ConstraintWidget getTargetWidget(int id) {
        if (id == 0) {
            return this.mLayoutWidget;
        }
        View view = this.mChildrenByIds.get(id);
        if (view == null && (view = findViewById(id)) != null && view != this && view.getParent() == this) {
            onViewAdded(view);
        }
        if (view == this) {
            return this.mLayoutWidget;
        }
        if (view != null) {
            return ((LayoutParams) view.getLayoutParams()).widget;
        }
        return null;
    }

    public final ConstraintWidget getViewWidget(View view) {
        if (view == this) {
            return this.mLayoutWidget;
        }
        if (view != null) {
            return ((LayoutParams) view.getLayoutParams()).widget;
        }
        return null;
    }

    private void internalMeasureChildren(int parentWidthSpec, int parentHeightSpec) {
        int baseline;
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        ConstraintLayout constraintLayout = this;
        int i = parentWidthSpec;
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();
        int widgetsCount = getChildCount();
        int i2 = 0;
        while (i2 < widgetsCount) {
            View child = constraintLayout.getChildAt(i2);
            if (child.getVisibility() != 8) {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                ConstraintWidget widget = params.widget;
                if (!params.isGuideline && !params.isHelper) {
                    widget.setVisibility(child.getVisibility());
                    int width = params.width;
                    int height = params.height;
                    boolean doMeasure = params.horizontalDimensionFixed || params.verticalDimensionFixed || (!params.horizontalDimensionFixed && params.matchConstraintDefaultWidth == 1) || params.width == -1 || (!params.verticalDimensionFixed && (params.matchConstraintDefaultHeight == 1 || params.height == -1));
                    boolean didWrapMeasureWidth = false;
                    boolean didWrapMeasureHeight = false;
                    if (doMeasure) {
                        if (width == 0) {
                            int childWidthMeasureSpec2 = getChildMeasureSpec(i, widthPadding, -2);
                            didWrapMeasureWidth = USE_CONSTRAINTS_HELPER;
                            childWidthMeasureSpec = childWidthMeasureSpec2;
                        } else if (width == -1) {
                            childWidthMeasureSpec = getChildMeasureSpec(i, widthPadding, -1);
                        } else {
                            if (width == -2) {
                                didWrapMeasureWidth = USE_CONSTRAINTS_HELPER;
                            }
                            childWidthMeasureSpec = getChildMeasureSpec(i, widthPadding, width);
                        }
                        if (height == 0) {
                            int childHeightMeasureSpec2 = getChildMeasureSpec(parentHeightSpec, heightPadding, -2);
                            didWrapMeasureHeight = USE_CONSTRAINTS_HELPER;
                            childHeightMeasureSpec = childHeightMeasureSpec2;
                        } else if (height == -1) {
                            childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, -1);
                        } else {
                            if (height == -2) {
                                didWrapMeasureHeight = USE_CONSTRAINTS_HELPER;
                            }
                            childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding, height);
                        }
                        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                        Metrics metrics = constraintLayout.mMetrics;
                        if (metrics != null) {
                            metrics.measures++;
                        }
                        widget.setWidthWrapContent(width == -2 ? USE_CONSTRAINTS_HELPER : false);
                        widget.setHeightWrapContent(height == -2 ? USE_CONSTRAINTS_HELPER : false);
                        width = child.getMeasuredWidth();
                        height = child.getMeasuredHeight();
                    }
                    widget.setWidth(width);
                    widget.setHeight(height);
                    if (didWrapMeasureWidth) {
                        widget.setWrapWidth(width);
                    }
                    if (didWrapMeasureHeight) {
                        widget.setWrapHeight(height);
                    }
                    if (params.needsBaseline && (baseline = child.getBaseline()) != -1) {
                        widget.setBaselineDistance(baseline);
                    }
                }
            }
            i2++;
            constraintLayout = this;
            i = parentWidthSpec;
        }
    }

    private void updatePostMeasures() {
        int widgetsCount = getChildCount();
        for (int i = 0; i < widgetsCount; i++) {
            View child = getChildAt(i);
            if (child instanceof Placeholder) {
                ((Placeholder) child).updatePostMeasure(this);
            }
        }
        int helperCount = this.mConstraintHelpers.size();
        if (helperCount > 0) {
            for (int i2 = 0; i2 < helperCount; i2++) {
                ConstraintHelper helper = this.mConstraintHelpers.get(i2);
                helper.updatePostMeasure(this);
            }
        }
    }

    private void internalMeasureDimensions(int parentWidthSpec, int parentHeightSpec) {
        int i;
        int widthPadding;
        int i2;
        int widgetsCount;
        int childWidthMeasureSpec;
        boolean resolveHeight;
        int childHeightMeasureSpec;
        int baseline;
        int heightPadding;
        int baseline2;
        ConstraintLayout constraintLayout = this;
        int i3 = parentWidthSpec;
        int i4 = parentHeightSpec;
        int heightPadding2 = getPaddingTop() + getPaddingBottom();
        int widthPadding2 = getPaddingLeft() + getPaddingRight();
        int widgetsCount2 = getChildCount();
        int i5 = 0;
        while (true) {
            i = 8;
            if (i5 >= widgetsCount2) {
                break;
            }
            View child = constraintLayout.getChildAt(i5);
            if (child.getVisibility() == 8) {
                heightPadding = heightPadding2;
            } else {
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                ConstraintWidget widget = params.widget;
                if (params.isGuideline) {
                    heightPadding = heightPadding2;
                } else if (params.isHelper) {
                    heightPadding = heightPadding2;
                } else {
                    widget.setVisibility(child.getVisibility());
                    int width = params.width;
                    int height = params.height;
                    if (width == 0) {
                        heightPadding = heightPadding2;
                    } else if (height == 0) {
                        heightPadding = heightPadding2;
                    } else {
                        boolean didWrapMeasureWidth = false;
                        boolean didWrapMeasureHeight = false;
                        if (width == -2) {
                            didWrapMeasureWidth = USE_CONSTRAINTS_HELPER;
                        }
                        int childWidthMeasureSpec2 = getChildMeasureSpec(i3, widthPadding2, width);
                        if (height == -2) {
                            didWrapMeasureHeight = USE_CONSTRAINTS_HELPER;
                        }
                        int childHeightMeasureSpec2 = getChildMeasureSpec(i4, heightPadding2, height);
                        child.measure(childWidthMeasureSpec2, childHeightMeasureSpec2);
                        Metrics metrics = constraintLayout.mMetrics;
                        if (metrics == null) {
                            heightPadding = heightPadding2;
                        } else {
                            heightPadding = heightPadding2;
                            metrics.measures++;
                        }
                        widget.setWidthWrapContent(width == -2 ? USE_CONSTRAINTS_HELPER : false);
                        widget.setHeightWrapContent(height == -2 ? USE_CONSTRAINTS_HELPER : false);
                        int width2 = child.getMeasuredWidth();
                        int height2 = child.getMeasuredHeight();
                        widget.setWidth(width2);
                        widget.setHeight(height2);
                        if (didWrapMeasureWidth) {
                            widget.setWrapWidth(width2);
                        }
                        if (didWrapMeasureHeight) {
                            widget.setWrapHeight(height2);
                        }
                        if (params.needsBaseline && (baseline2 = child.getBaseline()) != -1) {
                            widget.setBaselineDistance(baseline2);
                        }
                        if (params.horizontalDimensionFixed && params.verticalDimensionFixed) {
                            widget.getResolutionWidth().resolve(width2);
                            widget.getResolutionHeight().resolve(height2);
                        }
                    }
                    widget.getResolutionWidth().invalidate();
                    widget.getResolutionHeight().invalidate();
                }
            }
            i5++;
            i4 = parentHeightSpec;
            heightPadding2 = heightPadding;
        }
        int heightPadding3 = heightPadding2;
        constraintLayout.mLayoutWidget.solveGraph();
        int i6 = 0;
        while (i6 < widgetsCount2) {
            View child2 = constraintLayout.getChildAt(i6);
            if (child2.getVisibility() == i) {
                i2 = i6;
                widthPadding = widthPadding2;
                widgetsCount = widgetsCount2;
            } else {
                LayoutParams params2 = (LayoutParams) child2.getLayoutParams();
                ConstraintWidget widget2 = params2.widget;
                if (params2.isGuideline) {
                    i2 = i6;
                    widthPadding = widthPadding2;
                    widgetsCount = widgetsCount2;
                } else if (params2.isHelper) {
                    i2 = i6;
                    widthPadding = widthPadding2;
                    widgetsCount = widgetsCount2;
                } else {
                    widget2.setVisibility(child2.getVisibility());
                    int width3 = params2.width;
                    int height3 = params2.height;
                    if (width3 != 0 && height3 != 0) {
                        i2 = i6;
                        widthPadding = widthPadding2;
                        widgetsCount = widgetsCount2;
                    } else {
                        ResolutionAnchor left = widget2.getAnchor(ConstraintAnchor.Type.LEFT).getResolutionNode();
                        ResolutionAnchor right = widget2.getAnchor(ConstraintAnchor.Type.RIGHT).getResolutionNode();
                        boolean bothHorizontal = (widget2.getAnchor(ConstraintAnchor.Type.LEFT).getTarget() == null || widget2.getAnchor(ConstraintAnchor.Type.RIGHT).getTarget() == null) ? false : USE_CONSTRAINTS_HELPER;
                        ResolutionAnchor top = widget2.getAnchor(ConstraintAnchor.Type.TOP).getResolutionNode();
                        ResolutionAnchor bottom = widget2.getAnchor(ConstraintAnchor.Type.BOTTOM).getResolutionNode();
                        widgetsCount = widgetsCount2;
                        boolean bothVertical = (widget2.getAnchor(ConstraintAnchor.Type.TOP).getTarget() == null || widget2.getAnchor(ConstraintAnchor.Type.BOTTOM).getTarget() == null) ? false : USE_CONSTRAINTS_HELPER;
                        if (width3 == 0 && height3 == 0 && bothHorizontal && bothVertical) {
                            i2 = i6;
                            widthPadding = widthPadding2;
                        } else {
                            boolean didWrapMeasureWidth2 = false;
                            boolean didWrapMeasureHeight2 = false;
                            i2 = i6;
                            boolean resolveWidth = constraintLayout.mLayoutWidget.getHorizontalDimensionBehaviour() != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT ? USE_CONSTRAINTS_HELPER : false;
                            boolean resolveHeight2 = constraintLayout.mLayoutWidget.getVerticalDimensionBehaviour() != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT ? USE_CONSTRAINTS_HELPER : false;
                            if (!resolveWidth) {
                                widget2.getResolutionWidth().invalidate();
                            }
                            if (!resolveHeight2) {
                                widget2.getResolutionHeight().invalidate();
                            }
                            if (width3 == 0) {
                                if (resolveWidth && widget2.isSpreadWidth() && bothHorizontal && left.isResolved() && right.isResolved()) {
                                    width3 = (int) (right.getResolvedValue() - left.getResolvedValue());
                                    widget2.getResolutionWidth().resolve(width3);
                                    childWidthMeasureSpec = getChildMeasureSpec(i3, widthPadding2, width3);
                                } else {
                                    int childWidthMeasureSpec3 = getChildMeasureSpec(i3, widthPadding2, -2);
                                    didWrapMeasureWidth2 = USE_CONSTRAINTS_HELPER;
                                    resolveWidth = false;
                                    childWidthMeasureSpec = childWidthMeasureSpec3;
                                }
                            } else if (width3 == -1) {
                                childWidthMeasureSpec = getChildMeasureSpec(i3, widthPadding2, -1);
                            } else {
                                if (width3 == -2) {
                                    didWrapMeasureWidth2 = USE_CONSTRAINTS_HELPER;
                                }
                                childWidthMeasureSpec = getChildMeasureSpec(i3, widthPadding2, width3);
                            }
                            if (height3 == 0) {
                                if (resolveHeight2 && widget2.isSpreadHeight() && bothVertical && top.isResolved() && bottom.isResolved()) {
                                    resolveHeight = resolveHeight2;
                                    height3 = (int) (bottom.getResolvedValue() - top.getResolvedValue());
                                    widget2.getResolutionHeight().resolve(height3);
                                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding3, height3);
                                } else {
                                    int childHeightMeasureSpec3 = getChildMeasureSpec(parentHeightSpec, heightPadding3, -2);
                                    didWrapMeasureHeight2 = USE_CONSTRAINTS_HELPER;
                                    resolveHeight = false;
                                    childHeightMeasureSpec = childHeightMeasureSpec3;
                                }
                            } else {
                                resolveHeight = resolveHeight2;
                                if (height3 == -1) {
                                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding3, -1);
                                } else {
                                    if (height3 == -2) {
                                        didWrapMeasureHeight2 = USE_CONSTRAINTS_HELPER;
                                    }
                                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec, heightPadding3, height3);
                                }
                            }
                            child2.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                            constraintLayout = this;
                            Metrics metrics2 = constraintLayout.mMetrics;
                            if (metrics2 == null) {
                                widthPadding = widthPadding2;
                            } else {
                                widthPadding = widthPadding2;
                                metrics2.measures++;
                            }
                            widget2.setWidthWrapContent(width3 == -2 ? USE_CONSTRAINTS_HELPER : false);
                            widget2.setHeightWrapContent(height3 == -2 ? USE_CONSTRAINTS_HELPER : false);
                            int width4 = child2.getMeasuredWidth();
                            int height4 = child2.getMeasuredHeight();
                            widget2.setWidth(width4);
                            widget2.setHeight(height4);
                            if (didWrapMeasureWidth2) {
                                widget2.setWrapWidth(width4);
                            }
                            if (didWrapMeasureHeight2) {
                                widget2.setWrapHeight(height4);
                            }
                            if (resolveWidth) {
                                widget2.getResolutionWidth().resolve(width4);
                            } else {
                                widget2.getResolutionWidth().remove();
                            }
                            if (resolveHeight) {
                                widget2.getResolutionHeight().resolve(height4);
                            } else {
                                widget2.getResolutionHeight().remove();
                            }
                            if (params2.needsBaseline && (baseline = child2.getBaseline()) != -1) {
                                widget2.setBaselineDistance(baseline);
                            }
                        }
                    }
                }
            }
            i6 = i2 + 1;
            i3 = parentWidthSpec;
            widgetsCount2 = widgetsCount;
            widthPadding2 = widthPadding;
            i = 8;
        }
    }

    public void fillMetrics(Metrics metrics) {
        this.mMetrics = metrics;
        this.mLayoutWidget.fillMetrics(metrics);
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int REMEASURES_B;
        int childState;
        int startingWidth;
        int startingWidth2;
        boolean containerWrapWidth;
        boolean needSolverPass;
        int i;
        int startingHeight;
        int startingWidth3;
        int widthSpec;
        int heightSpec;
        int baseline;
        int i2 = widthMeasureSpec;
        int i3 = heightMeasureSpec;
        System.currentTimeMillis();
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        this.mLayoutWidget.setX(paddingLeft);
        this.mLayoutWidget.setY(paddingTop);
        this.mLayoutWidget.setMaxWidth(this.mMaxWidth);
        this.mLayoutWidget.setMaxHeight(this.mMaxHeight);
        if (Build.VERSION.SDK_INT >= 17) {
            this.mLayoutWidget.setRtl(getLayoutDirection() == 1);
        }
        setSelfDimensionBehaviour(widthMeasureSpec, heightMeasureSpec);
        int startingWidth4 = this.mLayoutWidget.getWidth();
        int startingHeight2 = this.mLayoutWidget.getHeight();
        boolean runAnalyzer = false;
        if (this.mDirtyHierarchy) {
            this.mDirtyHierarchy = false;
            updateHierarchy();
            runAnalyzer = USE_CONSTRAINTS_HELPER;
        }
        boolean optimiseDimensions = (this.mOptimizationLevel & 8) == 8 ? USE_CONSTRAINTS_HELPER : false;
        if (optimiseDimensions) {
            this.mLayoutWidget.preOptimize();
            this.mLayoutWidget.optimizeForDimensions(startingWidth4, startingHeight2);
            internalMeasureDimensions(widthMeasureSpec, heightMeasureSpec);
        } else {
            internalMeasureChildren(widthMeasureSpec, heightMeasureSpec);
        }
        updatePostMeasures();
        if (getChildCount() > 0 && runAnalyzer) {
            Analyzer.determineGroups(this.mLayoutWidget);
        }
        if (this.mLayoutWidget.mGroupsWrapOptimized) {
            if (this.mLayoutWidget.mHorizontalWrapOptimized && widthMode == Integer.MIN_VALUE) {
                if (this.mLayoutWidget.mWrapFixedWidth < widthSize) {
                    ConstraintWidgetContainer constraintWidgetContainer = this.mLayoutWidget;
                    constraintWidgetContainer.setWidth(constraintWidgetContainer.mWrapFixedWidth);
                }
                this.mLayoutWidget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            }
            if (this.mLayoutWidget.mVerticalWrapOptimized && heightMode == Integer.MIN_VALUE) {
                if (this.mLayoutWidget.mWrapFixedHeight < heightSize) {
                    ConstraintWidgetContainer constraintWidgetContainer2 = this.mLayoutWidget;
                    constraintWidgetContainer2.setHeight(constraintWidgetContainer2.mWrapFixedHeight);
                }
                this.mLayoutWidget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            }
        }
        int REMEASURES_A = 0;
        if ((this.mOptimizationLevel & 32) != 32) {
            REMEASURES_B = 0;
        } else {
            int width = this.mLayoutWidget.getWidth();
            int height = this.mLayoutWidget.getHeight();
            if (this.mLastMeasureWidth != width && widthMode == 1073741824) {
                REMEASURES_B = 0;
                Analyzer.setPosition(this.mLayoutWidget.mWidgetGroups, 0, width);
            } else {
                REMEASURES_B = 0;
            }
            if (this.mLastMeasureHeight != height && heightMode == 1073741824) {
                Analyzer.setPosition(this.mLayoutWidget.mWidgetGroups, 1, height);
            }
            if (this.mLayoutWidget.mHorizontalWrapOptimized && this.mLayoutWidget.mWrapFixedWidth > widthSize) {
                Analyzer.setPosition(this.mLayoutWidget.mWidgetGroups, 0, widthSize);
            }
            if (this.mLayoutWidget.mVerticalWrapOptimized && this.mLayoutWidget.mWrapFixedHeight > heightSize) {
                Analyzer.setPosition(this.mLayoutWidget.mWidgetGroups, 1, heightSize);
            }
        }
        if (getChildCount() > 0) {
            solveLinearSystem("First pass");
        }
        int sizeDependentWidgetsCount = this.mVariableDimensionsWidgets.size();
        int heightPadding = getPaddingBottom() + paddingTop;
        int widthPadding = paddingLeft + getPaddingRight();
        if (sizeDependentWidgetsCount <= 0) {
            childState = 0;
        } else {
            boolean containerWrapWidth2 = this.mLayoutWidget.getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT ? USE_CONSTRAINTS_HELPER : false;
            boolean containerWrapHeight = this.mLayoutWidget.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT ? USE_CONSTRAINTS_HELPER : false;
            int minWidth = Math.max(this.mLayoutWidget.getWidth(), this.mMinWidth);
            int height2 = this.mLayoutWidget.getHeight();
            int minWidth2 = this.mMinHeight;
            int minHeight = Math.max(height2, minWidth2);
            int minHeight2 = minHeight;
            int childState2 = 0;
            boolean needSolverPass2 = false;
            int i4 = 0;
            int minWidth3 = minWidth;
            while (i4 < sizeDependentWidgetsCount) {
                int paddingTop2 = paddingTop;
                ConstraintWidget widget = this.mVariableDimensionsWidgets.get(i4);
                int sizeDependentWidgetsCount2 = sizeDependentWidgetsCount;
                View child = (View) widget.getCompanionWidget();
                if (child == null) {
                    i = i4;
                    startingWidth3 = startingWidth4;
                    startingHeight = startingHeight2;
                } else {
                    startingHeight = startingHeight2;
                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    startingWidth3 = startingWidth4;
                    if (params.isHelper) {
                        i = i4;
                    } else if (params.isGuideline) {
                        i = i4;
                    } else {
                        i = i4;
                        if (child.getVisibility() != 8 && (!optimiseDimensions || !widget.getResolutionWidth().isResolved() || !widget.getResolutionHeight().isResolved())) {
                            int widthSpec2 = params.width;
                            if (widthSpec2 == -2 && params.horizontalDimensionFixed) {
                                widthSpec = getChildMeasureSpec(i2, widthPadding, params.width);
                            } else {
                                int widthSpec3 = widget.getWidth();
                                widthSpec = View.MeasureSpec.makeMeasureSpec(widthSpec3, 1073741824);
                            }
                            if (params.height == -2 && params.verticalDimensionFixed) {
                                heightSpec = getChildMeasureSpec(i3, heightPadding, params.height);
                            } else {
                                int heightSpec2 = widget.getHeight();
                                heightSpec = View.MeasureSpec.makeMeasureSpec(heightSpec2, 1073741824);
                            }
                            child.measure(widthSpec, heightSpec);
                            Metrics metrics = this.mMetrics;
                            if (metrics != null) {
                                metrics.additionalMeasures++;
                            }
                            REMEASURES_A++;
                            int measuredWidth = child.getMeasuredWidth();
                            int measuredHeight = child.getMeasuredHeight();
                            if (measuredWidth != widget.getWidth()) {
                                widget.setWidth(measuredWidth);
                                if (optimiseDimensions) {
                                    widget.getResolutionWidth().resolve(measuredWidth);
                                }
                                if (containerWrapWidth2 && widget.getRight() > minWidth3) {
                                    int w = widget.getRight() + widget.getAnchor(ConstraintAnchor.Type.RIGHT).getMargin();
                                    minWidth3 = Math.max(minWidth3, w);
                                }
                                needSolverPass2 = USE_CONSTRAINTS_HELPER;
                            }
                            if (measuredHeight != widget.getHeight()) {
                                widget.setHeight(measuredHeight);
                                if (optimiseDimensions) {
                                    widget.getResolutionHeight().resolve(measuredHeight);
                                }
                                if (containerWrapHeight && widget.getBottom() > minHeight2) {
                                    int h = widget.getBottom() + widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getMargin();
                                    minHeight2 = Math.max(minHeight2, h);
                                }
                                needSolverPass2 = USE_CONSTRAINTS_HELPER;
                            }
                            if (params.needsBaseline && (baseline = child.getBaseline()) != -1 && baseline != widget.getBaselineDistance()) {
                                widget.setBaselineDistance(baseline);
                                needSolverPass2 = USE_CONSTRAINTS_HELPER;
                            }
                            if (Build.VERSION.SDK_INT >= 11) {
                                childState2 = combineMeasuredStates(childState2, child.getMeasuredState());
                            }
                        }
                    }
                }
                i4 = i + 1;
                i2 = widthMeasureSpec;
                i3 = heightMeasureSpec;
                paddingTop = paddingTop2;
                startingWidth4 = startingWidth3;
                sizeDependentWidgetsCount = sizeDependentWidgetsCount2;
                startingHeight2 = startingHeight;
            }
            int sizeDependentWidgetsCount3 = sizeDependentWidgetsCount;
            int startingWidth5 = startingWidth4;
            int startingHeight3 = startingHeight2;
            if (!needSolverPass2) {
                startingWidth = startingWidth5;
            } else {
                startingWidth = startingWidth5;
                this.mLayoutWidget.setWidth(startingWidth);
                this.mLayoutWidget.setHeight(startingHeight3);
                if (optimiseDimensions) {
                    this.mLayoutWidget.solveGraph();
                }
                solveLinearSystem("2nd pass");
                boolean needSolverPass3 = false;
                if (this.mLayoutWidget.getWidth() < minWidth3) {
                    this.mLayoutWidget.setWidth(minWidth3);
                    needSolverPass3 = USE_CONSTRAINTS_HELPER;
                }
                if (this.mLayoutWidget.getHeight() >= minHeight2) {
                    needSolverPass = needSolverPass3;
                } else {
                    this.mLayoutWidget.setHeight(minHeight2);
                    needSolverPass = true;
                }
                if (needSolverPass) {
                    solveLinearSystem("3rd pass");
                }
            }
            int i5 = 0;
            while (true) {
                int sizeDependentWidgetsCount4 = sizeDependentWidgetsCount3;
                if (i5 >= sizeDependentWidgetsCount4) {
                    break;
                }
                ConstraintWidget widget2 = this.mVariableDimensionsWidgets.get(i5);
                View child2 = (View) widget2.getCompanionWidget();
                if (child2 == null) {
                    startingWidth2 = startingWidth;
                    containerWrapWidth = containerWrapWidth2;
                } else {
                    startingWidth2 = startingWidth;
                    if (child2.getMeasuredWidth() == widget2.getWidth() && child2.getMeasuredHeight() == widget2.getHeight()) {
                        containerWrapWidth = containerWrapWidth2;
                    } else if (widget2.getVisibility() == 8) {
                        containerWrapWidth = containerWrapWidth2;
                    } else {
                        int widthSpec4 = View.MeasureSpec.makeMeasureSpec(widget2.getWidth(), 1073741824);
                        containerWrapWidth = containerWrapWidth2;
                        int heightSpec3 = View.MeasureSpec.makeMeasureSpec(widget2.getHeight(), 1073741824);
                        child2.measure(widthSpec4, heightSpec3);
                        Metrics metrics2 = this.mMetrics;
                        if (metrics2 != null) {
                            metrics2.additionalMeasures++;
                        }
                        REMEASURES_B++;
                    }
                }
                i5++;
                sizeDependentWidgetsCount3 = sizeDependentWidgetsCount4;
                containerWrapWidth2 = containerWrapWidth;
                startingWidth = startingWidth2;
            }
            childState = childState2;
        }
        int androidLayoutWidth = this.mLayoutWidget.getWidth() + widthPadding;
        int androidLayoutHeight = this.mLayoutWidget.getHeight() + heightPadding;
        if (Build.VERSION.SDK_INT < 11) {
            setMeasuredDimension(androidLayoutWidth, androidLayoutHeight);
            this.mLastMeasureWidth = androidLayoutWidth;
            this.mLastMeasureHeight = androidLayoutHeight;
            return;
        }
        int resolvedWidthSize = resolveSizeAndState(androidLayoutWidth, widthMeasureSpec, childState);
        int resolvedHeightSize = resolveSizeAndState(androidLayoutHeight, heightMeasureSpec, childState << 16);
        int resolvedWidthSize2 = resolvedWidthSize & ViewCompat.MEASURED_SIZE_MASK;
        int resolvedHeightSize2 = resolvedHeightSize & ViewCompat.MEASURED_SIZE_MASK;
        int resolvedWidthSize3 = Math.min(this.mMaxWidth, resolvedWidthSize2);
        int resolvedHeightSize3 = Math.min(this.mMaxHeight, resolvedHeightSize2);
        if (this.mLayoutWidget.isWidthMeasuredTooSmall()) {
            resolvedWidthSize3 |= 16777216;
        }
        if (this.mLayoutWidget.isHeightMeasuredTooSmall()) {
            resolvedHeightSize3 |= 16777216;
        }
        setMeasuredDimension(resolvedWidthSize3, resolvedHeightSize3);
        this.mLastMeasureWidth = resolvedWidthSize3;
        this.mLastMeasureHeight = resolvedHeightSize3;
    }

    private void setSelfDimensionBehaviour(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();
        ConstraintWidget.DimensionBehaviour widthBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
        ConstraintWidget.DimensionBehaviour heightBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
        int desiredWidth = 0;
        int desiredHeight = 0;
        getLayoutParams();
        switch (widthMode) {
            case Integer.MIN_VALUE:
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                desiredWidth = widthSize;
                break;
            case 0:
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                break;
            case 1073741824:
                desiredWidth = Math.min(this.mMaxWidth, widthSize) - widthPadding;
                break;
        }
        switch (heightMode) {
            case Integer.MIN_VALUE:
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                desiredHeight = heightSize;
                break;
            case 0:
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                break;
            case 1073741824:
                desiredHeight = Math.min(this.mMaxHeight, heightSize) - heightPadding;
                break;
        }
        this.mLayoutWidget.setMinWidth(0);
        this.mLayoutWidget.setMinHeight(0);
        this.mLayoutWidget.setHorizontalDimensionBehaviour(widthBehaviour);
        this.mLayoutWidget.setWidth(desiredWidth);
        this.mLayoutWidget.setVerticalDimensionBehaviour(heightBehaviour);
        this.mLayoutWidget.setHeight(desiredHeight);
        this.mLayoutWidget.setMinWidth((this.mMinWidth - getPaddingLeft()) - getPaddingRight());
        this.mLayoutWidget.setMinHeight((this.mMinHeight - getPaddingTop()) - getPaddingBottom());
    }

    protected void solveLinearSystem(String reason) {
        this.mLayoutWidget.layout();
        Metrics metrics = this.mMetrics;
        if (metrics != null) {
            metrics.resolutions++;
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int widgetsCount = getChildCount();
        boolean isInEditMode = isInEditMode();
        for (int i = 0; i < widgetsCount; i++) {
            View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            ConstraintWidget widget = params.widget;
            if ((child.getVisibility() != 8 || params.isGuideline || params.isHelper || isInEditMode) && !params.isInPlaceholder) {
                int l = widget.getDrawX();
                int t = widget.getDrawY();
                int r = widget.getWidth() + l;
                int b = widget.getHeight() + t;
                child.layout(l, t, r, b);
                if (child instanceof Placeholder) {
                    Placeholder holder = (Placeholder) child;
                    View content = holder.getContent();
                    if (content != null) {
                        content.setVisibility(0);
                        content.layout(l, t, r, b);
                    }
                }
            }
        }
        int helperCount = this.mConstraintHelpers.size();
        if (helperCount > 0) {
            for (int i2 = 0; i2 < helperCount; i2++) {
                ConstraintHelper helper = this.mConstraintHelpers.get(i2);
                helper.updatePostLayout(this);
            }
        }
    }

    public void setOptimizationLevel(int level) {
        this.mLayoutWidget.setOptimizationLevel(level);
    }

    public int getOptimizationLevel() {
        return this.mLayoutWidget.getOptimizationLevel();
    }

    @Override // android.view.ViewGroup
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override // android.view.ViewGroup
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public void setConstraintSet(ConstraintSet set) {
        this.mConstraintSet = set;
    }

    public View getViewById(int id) {
        return this.mChildrenByIds.get(id);
    }

    @Override // android.view.ViewGroup, android.view.View
    public void dispatchDraw(Canvas canvas) {
        float ow;
        float ch;
        float cw;
        int count;
        super.dispatchDraw(canvas);
        if (isInEditMode()) {
            int count2 = getChildCount();
            float cw2 = getWidth();
            float ch2 = getHeight();
            float ow2 = 1080.0f;
            int i = 0;
            while (i < count2) {
                View child = getChildAt(i);
                if (child.getVisibility() == 8) {
                    count = count2;
                    cw = cw2;
                    ch = ch2;
                    ow = ow2;
                } else {
                    Object tag = child.getTag();
                    if (tag == null || !(tag instanceof String)) {
                        count = count2;
                        cw = cw2;
                        ch = ch2;
                        ow = ow2;
                    } else {
                        String coordinates = (String) tag;
                        String[] split = coordinates.split(",");
                        if (split.length != 4) {
                            count = count2;
                            cw = cw2;
                            ch = ch2;
                            ow = ow2;
                        } else {
                            int x = Integer.parseInt(split[0]);
                            int x2 = (int) ((x / ow2) * cw2);
                            int y = (int) ((Integer.parseInt(split[1]) / 1920.0f) * ch2);
                            int w = (int) ((Integer.parseInt(split[2]) / ow2) * cw2);
                            int h = (int) ((Integer.parseInt(split[3]) / 1920.0f) * ch2);
                            Paint paint = new Paint();
                            count = count2;
                            paint.setColor(SupportMenu.CATEGORY_MASK);
                            cw = cw2;
                            float cw3 = y;
                            ch = ch2;
                            ow = ow2;
                            float ow3 = y;
                            canvas.drawLine(x2, cw3, x2 + w, ow3, paint);
                            canvas.drawLine(x2 + w, y, x2 + w, y + h, paint);
                            canvas.drawLine(x2 + w, y + h, x2, y + h, paint);
                            canvas.drawLine(x2, y + h, x2, y, paint);
                            paint.setColor(-16711936);
                            canvas.drawLine(x2, y, x2 + w, y + h, paint);
                            canvas.drawLine(x2, y + h, x2 + w, y, paint);
                        }
                    }
                }
                i++;
                count2 = count;
                cw2 = cw;
                ch2 = ch;
                ow2 = ow;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public static final int BASELINE = 5;
        public static final int BOTTOM = 4;
        public static final int CHAIN_PACKED = 2;
        public static final int CHAIN_SPREAD = 0;
        public static final int CHAIN_SPREAD_INSIDE = 1;
        public static final int END = 7;
        public static final int HORIZONTAL = 0;
        public static final int LEFT = 1;
        public static final int MATCH_CONSTRAINT = 0;
        public static final int MATCH_CONSTRAINT_PERCENT = 2;
        public static final int MATCH_CONSTRAINT_SPREAD = 0;
        public static final int MATCH_CONSTRAINT_WRAP = 1;
        public static final int PARENT_ID = 0;
        public static final int RIGHT = 2;
        public static final int START = 6;
        public static final int TOP = 3;
        public static final int UNSET = -1;
        public static final int VERTICAL = 1;
        public int baselineToBaseline;
        public int bottomToBottom;
        public int bottomToTop;
        public float circleAngle;
        public int circleConstraint;
        public int circleRadius;
        public boolean constrainedHeight;
        public boolean constrainedWidth;
        public String dimensionRatio;
        int dimensionRatioSide;
        float dimensionRatioValue;
        public int editorAbsoluteX;
        public int editorAbsoluteY;
        public int endToEnd;
        public int endToStart;
        public int goneBottomMargin;
        public int goneEndMargin;
        public int goneLeftMargin;
        public int goneRightMargin;
        public int goneStartMargin;
        public int goneTopMargin;
        public int guideBegin;
        public int guideEnd;
        public float guidePercent;
        public boolean helped;
        public float horizontalBias;
        public int horizontalChainStyle;
        boolean horizontalDimensionFixed;
        public float horizontalWeight;
        boolean isGuideline;
        boolean isHelper;
        boolean isInPlaceholder;
        public int leftToLeft;
        public int leftToRight;
        public int matchConstraintDefaultHeight;
        public int matchConstraintDefaultWidth;
        public int matchConstraintMaxHeight;
        public int matchConstraintMaxWidth;
        public int matchConstraintMinHeight;
        public int matchConstraintMinWidth;
        public float matchConstraintPercentHeight;
        public float matchConstraintPercentWidth;
        boolean needsBaseline;
        public int orientation;
        int resolveGoneLeftMargin;
        int resolveGoneRightMargin;
        int resolvedGuideBegin;
        int resolvedGuideEnd;
        float resolvedGuidePercent;
        float resolvedHorizontalBias;
        int resolvedLeftToLeft;
        int resolvedLeftToRight;
        int resolvedRightToLeft;
        int resolvedRightToRight;
        public int rightToLeft;
        public int rightToRight;
        public int startToEnd;
        public int startToStart;
        public int topToBottom;
        public int topToTop;
        public float verticalBias;
        public int verticalChainStyle;
        boolean verticalDimensionFixed;
        public float verticalWeight;
        ConstraintWidget widget;

        public void reset() {
            ConstraintWidget constraintWidget = this.widget;
            if (constraintWidget != null) {
                constraintWidget.reset();
            }
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.guideBegin = -1;
            this.guideEnd = -1;
            this.guidePercent = -1.0f;
            this.leftToLeft = -1;
            this.leftToRight = -1;
            this.rightToLeft = -1;
            this.rightToRight = -1;
            this.topToTop = -1;
            this.topToBottom = -1;
            this.bottomToTop = -1;
            this.bottomToBottom = -1;
            this.baselineToBaseline = -1;
            this.circleConstraint = -1;
            this.circleRadius = 0;
            this.circleAngle = 0.0f;
            this.startToEnd = -1;
            this.startToStart = -1;
            this.endToStart = -1;
            this.endToEnd = -1;
            this.goneLeftMargin = -1;
            this.goneTopMargin = -1;
            this.goneRightMargin = -1;
            this.goneBottomMargin = -1;
            this.goneStartMargin = -1;
            this.goneEndMargin = -1;
            this.horizontalBias = 0.5f;
            this.verticalBias = 0.5f;
            this.dimensionRatio = null;
            this.dimensionRatioValue = 0.0f;
            this.dimensionRatioSide = 1;
            this.horizontalWeight = -1.0f;
            this.verticalWeight = -1.0f;
            this.horizontalChainStyle = 0;
            this.verticalChainStyle = 0;
            this.matchConstraintDefaultWidth = 0;
            this.matchConstraintDefaultHeight = 0;
            this.matchConstraintMinWidth = 0;
            this.matchConstraintMinHeight = 0;
            this.matchConstraintMaxWidth = 0;
            this.matchConstraintMaxHeight = 0;
            this.matchConstraintPercentWidth = 1.0f;
            this.matchConstraintPercentHeight = 1.0f;
            this.editorAbsoluteX = -1;
            this.editorAbsoluteY = -1;
            this.orientation = -1;
            this.constrainedWidth = false;
            this.constrainedHeight = false;
            this.horizontalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.verticalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.needsBaseline = false;
            this.isGuideline = false;
            this.isHelper = false;
            this.isInPlaceholder = false;
            this.resolvedLeftToLeft = -1;
            this.resolvedLeftToRight = -1;
            this.resolvedRightToLeft = -1;
            this.resolvedRightToRight = -1;
            this.resolveGoneLeftMargin = -1;
            this.resolveGoneRightMargin = -1;
            this.resolvedHorizontalBias = 0.5f;
            this.widget = new ConstraintWidget();
            this.helped = false;
            this.guideBegin = source.guideBegin;
            this.guideEnd = source.guideEnd;
            this.guidePercent = source.guidePercent;
            this.leftToLeft = source.leftToLeft;
            this.leftToRight = source.leftToRight;
            this.rightToLeft = source.rightToLeft;
            this.rightToRight = source.rightToRight;
            this.topToTop = source.topToTop;
            this.topToBottom = source.topToBottom;
            this.bottomToTop = source.bottomToTop;
            this.bottomToBottom = source.bottomToBottom;
            this.baselineToBaseline = source.baselineToBaseline;
            this.circleConstraint = source.circleConstraint;
            this.circleRadius = source.circleRadius;
            this.circleAngle = source.circleAngle;
            this.startToEnd = source.startToEnd;
            this.startToStart = source.startToStart;
            this.endToStart = source.endToStart;
            this.endToEnd = source.endToEnd;
            this.goneLeftMargin = source.goneLeftMargin;
            this.goneTopMargin = source.goneTopMargin;
            this.goneRightMargin = source.goneRightMargin;
            this.goneBottomMargin = source.goneBottomMargin;
            this.goneStartMargin = source.goneStartMargin;
            this.goneEndMargin = source.goneEndMargin;
            this.horizontalBias = source.horizontalBias;
            this.verticalBias = source.verticalBias;
            this.dimensionRatio = source.dimensionRatio;
            this.dimensionRatioValue = source.dimensionRatioValue;
            this.dimensionRatioSide = source.dimensionRatioSide;
            this.horizontalWeight = source.horizontalWeight;
            this.verticalWeight = source.verticalWeight;
            this.horizontalChainStyle = source.horizontalChainStyle;
            this.verticalChainStyle = source.verticalChainStyle;
            this.constrainedWidth = source.constrainedWidth;
            this.constrainedHeight = source.constrainedHeight;
            this.matchConstraintDefaultWidth = source.matchConstraintDefaultWidth;
            this.matchConstraintDefaultHeight = source.matchConstraintDefaultHeight;
            this.matchConstraintMinWidth = source.matchConstraintMinWidth;
            this.matchConstraintMaxWidth = source.matchConstraintMaxWidth;
            this.matchConstraintMinHeight = source.matchConstraintMinHeight;
            this.matchConstraintMaxHeight = source.matchConstraintMaxHeight;
            this.matchConstraintPercentWidth = source.matchConstraintPercentWidth;
            this.matchConstraintPercentHeight = source.matchConstraintPercentHeight;
            this.editorAbsoluteX = source.editorAbsoluteX;
            this.editorAbsoluteY = source.editorAbsoluteY;
            this.orientation = source.orientation;
            this.horizontalDimensionFixed = source.horizontalDimensionFixed;
            this.verticalDimensionFixed = source.verticalDimensionFixed;
            this.needsBaseline = source.needsBaseline;
            this.isGuideline = source.isGuideline;
            this.resolvedLeftToLeft = source.resolvedLeftToLeft;
            this.resolvedLeftToRight = source.resolvedLeftToRight;
            this.resolvedRightToLeft = source.resolvedRightToLeft;
            this.resolvedRightToRight = source.resolvedRightToRight;
            this.resolveGoneLeftMargin = source.resolveGoneLeftMargin;
            this.resolveGoneRightMargin = source.resolveGoneRightMargin;
            this.resolvedHorizontalBias = source.resolvedHorizontalBias;
            this.widget = source.widget;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Table {
            public static final int ANDROID_ORIENTATION = 1;
            public static final int LAYOUT_CONSTRAINED_HEIGHT = 28;
            public static final int LAYOUT_CONSTRAINED_WIDTH = 27;
            public static final int LAYOUT_CONSTRAINT_BASELINE_CREATOR = 43;
            public static final int LAYOUT_CONSTRAINT_BASELINE_TO_BASELINE_OF = 16;
            public static final int LAYOUT_CONSTRAINT_BOTTOM_CREATOR = 42;
            public static final int LAYOUT_CONSTRAINT_BOTTOM_TO_BOTTOM_OF = 15;
            public static final int LAYOUT_CONSTRAINT_BOTTOM_TO_TOP_OF = 14;
            public static final int LAYOUT_CONSTRAINT_CIRCLE = 2;
            public static final int LAYOUT_CONSTRAINT_CIRCLE_ANGLE = 4;
            public static final int LAYOUT_CONSTRAINT_CIRCLE_RADIUS = 3;
            public static final int LAYOUT_CONSTRAINT_DIMENSION_RATIO = 44;
            public static final int LAYOUT_CONSTRAINT_END_TO_END_OF = 20;
            public static final int LAYOUT_CONSTRAINT_END_TO_START_OF = 19;
            public static final int LAYOUT_CONSTRAINT_GUIDE_BEGIN = 5;
            public static final int LAYOUT_CONSTRAINT_GUIDE_END = 6;
            public static final int LAYOUT_CONSTRAINT_GUIDE_PERCENT = 7;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_DEFAULT = 32;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_MAX = 37;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_MIN = 36;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_PERCENT = 38;
            public static final int LAYOUT_CONSTRAINT_HORIZONTAL_BIAS = 29;
            public static final int LAYOUT_CONSTRAINT_HORIZONTAL_CHAINSTYLE = 47;
            public static final int LAYOUT_CONSTRAINT_HORIZONTAL_WEIGHT = 45;
            public static final int LAYOUT_CONSTRAINT_LEFT_CREATOR = 39;
            public static final int LAYOUT_CONSTRAINT_LEFT_TO_LEFT_OF = 8;
            public static final int LAYOUT_CONSTRAINT_LEFT_TO_RIGHT_OF = 9;
            public static final int LAYOUT_CONSTRAINT_RIGHT_CREATOR = 41;
            public static final int LAYOUT_CONSTRAINT_RIGHT_TO_LEFT_OF = 10;
            public static final int LAYOUT_CONSTRAINT_RIGHT_TO_RIGHT_OF = 11;
            public static final int LAYOUT_CONSTRAINT_START_TO_END_OF = 17;
            public static final int LAYOUT_CONSTRAINT_START_TO_START_OF = 18;
            public static final int LAYOUT_CONSTRAINT_TOP_CREATOR = 40;
            public static final int LAYOUT_CONSTRAINT_TOP_TO_BOTTOM_OF = 13;
            public static final int LAYOUT_CONSTRAINT_TOP_TO_TOP_OF = 12;
            public static final int LAYOUT_CONSTRAINT_VERTICAL_BIAS = 30;
            public static final int LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE = 48;
            public static final int LAYOUT_CONSTRAINT_VERTICAL_WEIGHT = 46;
            public static final int LAYOUT_CONSTRAINT_WIDTH_DEFAULT = 31;
            public static final int LAYOUT_CONSTRAINT_WIDTH_MAX = 34;
            public static final int LAYOUT_CONSTRAINT_WIDTH_MIN = 33;
            public static final int LAYOUT_CONSTRAINT_WIDTH_PERCENT = 35;
            public static final int LAYOUT_EDITOR_ABSOLUTEX = 49;
            public static final int LAYOUT_EDITOR_ABSOLUTEY = 50;
            public static final int LAYOUT_GONE_MARGIN_BOTTOM = 24;
            public static final int LAYOUT_GONE_MARGIN_END = 26;
            public static final int LAYOUT_GONE_MARGIN_LEFT = 21;
            public static final int LAYOUT_GONE_MARGIN_RIGHT = 23;
            public static final int LAYOUT_GONE_MARGIN_START = 25;
            public static final int LAYOUT_GONE_MARGIN_TOP = 22;
            public static final int UNUSED = 0;
            public static final SparseIntArray map;

            private Table() {
            }

            static {
                SparseIntArray sparseIntArray = new SparseIntArray();
                map = sparseIntArray;
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf, 8);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf, 9);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf, 10);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf, 11);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf, 12);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf, 13);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf, 14);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf, 15);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf, 16);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircle, 2);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircleRadius, 3);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircleAngle, 4);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX, 49);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY, 50);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_begin, 5);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_end, 6);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_percent, 7);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_android_orientation, 1);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf, 17);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf, 18);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf, 19);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf, 20);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginLeft, 21);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginTop, 22);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginRight, 23);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginBottom, 24);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginStart, 25);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginEnd, 26);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_bias, 29);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_bias, 30);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintDimensionRatio, 44);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_weight, 45);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_weight, 46);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_chainStyle, 47);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_chainStyle, 48);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constrainedWidth, 27);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constrainedHeight, 28);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_default, 31);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_default, 32);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_min, 33);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_max, 34);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_percent, 35);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_min, 36);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_max, 37);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_percent, 38);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_creator, 39);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_creator, 40);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_creator, 41);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_creator, 42);
                sparseIntArray.append(R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_creator, 43);
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            int value;
            int i;
            int commaIndex;
            int i2 = -1;
            this.guideBegin = -1;
            this.guideEnd = -1;
            this.guidePercent = -1.0f;
            this.leftToLeft = -1;
            this.leftToRight = -1;
            this.rightToLeft = -1;
            this.rightToRight = -1;
            this.topToTop = -1;
            this.topToBottom = -1;
            this.bottomToTop = -1;
            this.bottomToBottom = -1;
            this.baselineToBaseline = -1;
            this.circleConstraint = -1;
            int i3 = 0;
            this.circleRadius = 0;
            this.circleAngle = 0.0f;
            this.startToEnd = -1;
            this.startToStart = -1;
            this.endToStart = -1;
            this.endToEnd = -1;
            this.goneLeftMargin = -1;
            this.goneTopMargin = -1;
            this.goneRightMargin = -1;
            this.goneBottomMargin = -1;
            this.goneStartMargin = -1;
            this.goneEndMargin = -1;
            this.horizontalBias = 0.5f;
            this.verticalBias = 0.5f;
            this.dimensionRatio = null;
            this.dimensionRatioValue = 0.0f;
            this.dimensionRatioSide = 1;
            this.horizontalWeight = -1.0f;
            this.verticalWeight = -1.0f;
            this.horizontalChainStyle = 0;
            this.verticalChainStyle = 0;
            this.matchConstraintDefaultWidth = 0;
            this.matchConstraintDefaultHeight = 0;
            this.matchConstraintMinWidth = 0;
            this.matchConstraintMinHeight = 0;
            this.matchConstraintMaxWidth = 0;
            this.matchConstraintMaxHeight = 0;
            this.matchConstraintPercentWidth = 1.0f;
            this.matchConstraintPercentHeight = 1.0f;
            this.editorAbsoluteX = -1;
            this.editorAbsoluteY = -1;
            this.orientation = -1;
            this.constrainedWidth = false;
            this.constrainedHeight = false;
            this.horizontalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.verticalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.needsBaseline = false;
            this.isGuideline = false;
            this.isHelper = false;
            this.isInPlaceholder = false;
            this.resolvedLeftToLeft = -1;
            this.resolvedLeftToRight = -1;
            this.resolvedRightToLeft = -1;
            this.resolvedRightToRight = -1;
            this.resolveGoneLeftMargin = -1;
            this.resolveGoneRightMargin = -1;
            this.resolvedHorizontalBias = 0.5f;
            this.widget = new ConstraintWidget();
            this.helped = false;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            int N = a.getIndexCount();
            int i4 = 0;
            while (i4 < N) {
                int attr = a.getIndex(i4);
                int look = Table.map.get(attr);
                switch (look) {
                    case 0:
                        int i5 = i3;
                        i = i2;
                        value = i5;
                        break;
                    case 1:
                        int i6 = i3;
                        i = i2;
                        value = i6;
                        this.orientation = a.getInt(attr, this.orientation);
                        break;
                    case 2:
                        value = i3;
                        int resourceId = a.getResourceId(attr, this.circleConstraint);
                        this.circleConstraint = resourceId;
                        i = -1;
                        if (resourceId != -1) {
                            break;
                        } else {
                            this.circleConstraint = a.getInt(attr, -1);
                            break;
                        }
                    case 3:
                        value = i3;
                        this.circleRadius = a.getDimensionPixelSize(attr, this.circleRadius);
                        i = -1;
                        break;
                    case 4:
                        value = i3;
                        float f = a.getFloat(attr, this.circleAngle) % 360.0f;
                        this.circleAngle = f;
                        if (f < 0.0f) {
                            this.circleAngle = (360.0f - f) % 360.0f;
                            i = -1;
                            break;
                        } else {
                            i = -1;
                            break;
                        }
                    case 5:
                        value = i3;
                        this.guideBegin = a.getDimensionPixelOffset(attr, this.guideBegin);
                        i = -1;
                        break;
                    case 6:
                        value = i3;
                        this.guideEnd = a.getDimensionPixelOffset(attr, this.guideEnd);
                        i = -1;
                        break;
                    case 7:
                        value = i3;
                        this.guidePercent = a.getFloat(attr, this.guidePercent);
                        i = -1;
                        break;
                    case 8:
                        int i7 = i3;
                        int i8 = i2;
                        value = i7;
                        int resourceId2 = a.getResourceId(attr, this.leftToLeft);
                        this.leftToLeft = resourceId2;
                        if (resourceId2 == i8) {
                            this.leftToLeft = a.getInt(attr, i8);
                            i = -1;
                            break;
                        } else {
                            i = -1;
                            break;
                        }
                    case 9:
                        int i9 = i3;
                        i = i2;
                        value = i9;
                        int resourceId3 = a.getResourceId(attr, this.leftToRight);
                        this.leftToRight = resourceId3;
                        if (resourceId3 != i) {
                            break;
                        } else {
                            this.leftToRight = a.getInt(attr, i);
                            break;
                        }
                    case 10:
                        int i10 = i3;
                        i = i2;
                        value = i10;
                        int resourceId4 = a.getResourceId(attr, this.rightToLeft);
                        this.rightToLeft = resourceId4;
                        if (resourceId4 != i) {
                            break;
                        } else {
                            this.rightToLeft = a.getInt(attr, i);
                            break;
                        }
                    case 11:
                        int i11 = i3;
                        i = i2;
                        value = i11;
                        int resourceId5 = a.getResourceId(attr, this.rightToRight);
                        this.rightToRight = resourceId5;
                        if (resourceId5 != i) {
                            break;
                        } else {
                            this.rightToRight = a.getInt(attr, i);
                            break;
                        }
                    case 12:
                        int i12 = i3;
                        i = i2;
                        value = i12;
                        int resourceId6 = a.getResourceId(attr, this.topToTop);
                        this.topToTop = resourceId6;
                        if (resourceId6 != i) {
                            break;
                        } else {
                            this.topToTop = a.getInt(attr, i);
                            break;
                        }
                    case 13:
                        int i13 = i3;
                        i = i2;
                        value = i13;
                        int resourceId7 = a.getResourceId(attr, this.topToBottom);
                        this.topToBottom = resourceId7;
                        if (resourceId7 != i) {
                            break;
                        } else {
                            this.topToBottom = a.getInt(attr, i);
                            break;
                        }
                    case 14:
                        int i14 = i3;
                        i = i2;
                        value = i14;
                        int resourceId8 = a.getResourceId(attr, this.bottomToTop);
                        this.bottomToTop = resourceId8;
                        if (resourceId8 != i) {
                            break;
                        } else {
                            this.bottomToTop = a.getInt(attr, i);
                            break;
                        }
                    case 15:
                        int i15 = i3;
                        i = i2;
                        value = i15;
                        int resourceId9 = a.getResourceId(attr, this.bottomToBottom);
                        this.bottomToBottom = resourceId9;
                        if (resourceId9 != i) {
                            break;
                        } else {
                            this.bottomToBottom = a.getInt(attr, i);
                            break;
                        }
                    case 16:
                        int i16 = i3;
                        i = i2;
                        value = i16;
                        int resourceId10 = a.getResourceId(attr, this.baselineToBaseline);
                        this.baselineToBaseline = resourceId10;
                        if (resourceId10 != i) {
                            break;
                        } else {
                            this.baselineToBaseline = a.getInt(attr, i);
                            break;
                        }
                    case 17:
                        int i17 = i3;
                        i = i2;
                        value = i17;
                        int resourceId11 = a.getResourceId(attr, this.startToEnd);
                        this.startToEnd = resourceId11;
                        if (resourceId11 != i) {
                            break;
                        } else {
                            this.startToEnd = a.getInt(attr, i);
                            break;
                        }
                    case 18:
                        int i18 = i3;
                        i = i2;
                        value = i18;
                        int resourceId12 = a.getResourceId(attr, this.startToStart);
                        this.startToStart = resourceId12;
                        if (resourceId12 != i) {
                            break;
                        } else {
                            this.startToStart = a.getInt(attr, i);
                            break;
                        }
                    case 19:
                        int i19 = i3;
                        i = i2;
                        value = i19;
                        int resourceId13 = a.getResourceId(attr, this.endToStart);
                        this.endToStart = resourceId13;
                        if (resourceId13 != i) {
                            break;
                        } else {
                            this.endToStart = a.getInt(attr, i);
                            break;
                        }
                    case 20:
                        value = i3;
                        int resourceId14 = a.getResourceId(attr, this.endToEnd);
                        this.endToEnd = resourceId14;
                        i = -1;
                        if (resourceId14 != -1) {
                            break;
                        } else {
                            this.endToEnd = a.getInt(attr, -1);
                            break;
                        }
                    case 21:
                        value = i3;
                        this.goneLeftMargin = a.getDimensionPixelSize(attr, this.goneLeftMargin);
                        i = -1;
                        break;
                    case 22:
                        value = i3;
                        this.goneTopMargin = a.getDimensionPixelSize(attr, this.goneTopMargin);
                        i = -1;
                        break;
                    case 23:
                        value = i3;
                        this.goneRightMargin = a.getDimensionPixelSize(attr, this.goneRightMargin);
                        i = -1;
                        break;
                    case 24:
                        value = i3;
                        this.goneBottomMargin = a.getDimensionPixelSize(attr, this.goneBottomMargin);
                        i = -1;
                        break;
                    case 25:
                        value = i3;
                        this.goneStartMargin = a.getDimensionPixelSize(attr, this.goneStartMargin);
                        i = -1;
                        break;
                    case 26:
                        value = i3;
                        this.goneEndMargin = a.getDimensionPixelSize(attr, this.goneEndMargin);
                        i = -1;
                        break;
                    case 27:
                        value = i3;
                        this.constrainedWidth = a.getBoolean(attr, this.constrainedWidth);
                        i = -1;
                        break;
                    case 28:
                        value = i3;
                        this.constrainedHeight = a.getBoolean(attr, this.constrainedHeight);
                        i = -1;
                        break;
                    case 29:
                        value = i3;
                        this.horizontalBias = a.getFloat(attr, this.horizontalBias);
                        i = -1;
                        break;
                    case 30:
                        value = i3;
                        this.verticalBias = a.getFloat(attr, this.verticalBias);
                        i = -1;
                        break;
                    case 31:
                        value = i3;
                        int i20 = a.getInt(attr, value);
                        this.matchConstraintDefaultWidth = i20;
                        if (i20 == 1) {
                            Log.e(ConstraintLayout.TAG, "layout_constraintWidth_default=\"wrap\" is deprecated.\nUse layout_width=\"WRAP_CONTENT\" and layout_constrainedWidth=\"true\" instead.");
                            i = -1;
                            break;
                        } else {
                            i = -1;
                            break;
                        }
                    case 32:
                        value = 0;
                        int i21 = a.getInt(attr, 0);
                        this.matchConstraintDefaultHeight = i21;
                        if (i21 == 1) {
                            Log.e(ConstraintLayout.TAG, "layout_constraintHeight_default=\"wrap\" is deprecated.\nUse layout_height=\"WRAP_CONTENT\" and layout_constrainedHeight=\"true\" instead.");
                            i = -1;
                            break;
                        } else {
                            i = -1;
                            break;
                        }
                    case 33:
                        try {
                            this.matchConstraintMinWidth = a.getDimensionPixelSize(attr, this.matchConstraintMinWidth);
                            value = 0;
                            i = -1;
                            break;
                        } catch (Exception e) {
                            int value2 = a.getInt(attr, this.matchConstraintMinWidth);
                            if (value2 == -2) {
                                this.matchConstraintMinWidth = -2;
                            }
                            value = 0;
                            i = -1;
                            break;
                        }
                    case 34:
                        try {
                            this.matchConstraintMaxWidth = a.getDimensionPixelSize(attr, this.matchConstraintMaxWidth);
                            value = 0;
                            i = -1;
                            break;
                        } catch (Exception e2) {
                            int value3 = a.getInt(attr, this.matchConstraintMaxWidth);
                            if (value3 == -2) {
                                this.matchConstraintMaxWidth = -2;
                            }
                            value = 0;
                            i = -1;
                            break;
                        }
                    case 35:
                        this.matchConstraintPercentWidth = Math.max(0.0f, a.getFloat(attr, this.matchConstraintPercentWidth));
                        value = 0;
                        i = -1;
                        break;
                    case 36:
                        try {
                            this.matchConstraintMinHeight = a.getDimensionPixelSize(attr, this.matchConstraintMinHeight);
                            value = 0;
                            i = -1;
                            break;
                        } catch (Exception e3) {
                            int value4 = a.getInt(attr, this.matchConstraintMinHeight);
                            if (value4 == -2) {
                                this.matchConstraintMinHeight = -2;
                            }
                            value = 0;
                            i = -1;
                            break;
                        }
                    case 37:
                        try {
                            this.matchConstraintMaxHeight = a.getDimensionPixelSize(attr, this.matchConstraintMaxHeight);
                            value = 0;
                            i = -1;
                            break;
                        } catch (Exception e4) {
                            int value5 = a.getInt(attr, this.matchConstraintMaxHeight);
                            if (value5 == -2) {
                                this.matchConstraintMaxHeight = -2;
                            }
                            value = 0;
                            i = -1;
                            break;
                        }
                    case 38:
                        this.matchConstraintPercentHeight = Math.max(0.0f, a.getFloat(attr, this.matchConstraintPercentHeight));
                        value = 0;
                        i = -1;
                        break;
                    case 39:
                        value = 0;
                        i = -1;
                        break;
                    case 40:
                        value = 0;
                        i = -1;
                        break;
                    case 41:
                        value = 0;
                        i = -1;
                        break;
                    case 42:
                        value = 0;
                        i = -1;
                        break;
                    case 43:
                    default:
                        int i22 = i3;
                        i = i2;
                        value = i22;
                        break;
                    case 44:
                        String string = a.getString(attr);
                        this.dimensionRatio = string;
                        this.dimensionRatioValue = Float.NaN;
                        this.dimensionRatioSide = i2;
                        if (string == null) {
                            value = 0;
                            i = -1;
                            break;
                        } else {
                            int len = string.length();
                            int commaIndex2 = this.dimensionRatio.indexOf(44);
                            if (commaIndex2 > 0 && commaIndex2 < len - 1) {
                                String dimension = this.dimensionRatio.substring(i3, commaIndex2);
                                if (dimension.equalsIgnoreCase("W")) {
                                    this.dimensionRatioSide = i3;
                                } else if (dimension.equalsIgnoreCase("H")) {
                                    this.dimensionRatioSide = 1;
                                }
                                commaIndex = commaIndex2 + 1;
                            } else {
                                commaIndex = 0;
                            }
                            int colonIndex = this.dimensionRatio.indexOf(58);
                            if (colonIndex >= 0 && colonIndex < len - 1) {
                                String nominator = this.dimensionRatio.substring(commaIndex, colonIndex);
                                String denominator = this.dimensionRatio.substring(colonIndex + 1);
                                if (nominator.length() > 0 && denominator.length() > 0) {
                                    try {
                                        float nominatorValue = Float.parseFloat(nominator);
                                        float denominatorValue = Float.parseFloat(denominator);
                                        if (nominatorValue > 0.0f && denominatorValue > 0.0f) {
                                            if (this.dimensionRatioSide == 1) {
                                                this.dimensionRatioValue = Math.abs(denominatorValue / nominatorValue);
                                            } else {
                                                this.dimensionRatioValue = Math.abs(nominatorValue / denominatorValue);
                                            }
                                        }
                                    } catch (NumberFormatException e5) {
                                    }
                                }
                            } else {
                                String r = this.dimensionRatio.substring(commaIndex);
                                if (r.length() > 0) {
                                    try {
                                        this.dimensionRatioValue = Float.parseFloat(r);
                                    } catch (NumberFormatException e6) {
                                    }
                                }
                            }
                            value = 0;
                            i = -1;
                            break;
                        }
                        break;
                    case 45:
                        this.horizontalWeight = a.getFloat(attr, this.horizontalWeight);
                        int i23 = i3;
                        i = i2;
                        value = i23;
                        break;
                    case 46:
                        this.verticalWeight = a.getFloat(attr, this.verticalWeight);
                        int i24 = i3;
                        i = i2;
                        value = i24;
                        break;
                    case 47:
                        this.horizontalChainStyle = a.getInt(attr, i3);
                        int i25 = i3;
                        i = i2;
                        value = i25;
                        break;
                    case 48:
                        this.verticalChainStyle = a.getInt(attr, i3);
                        int i26 = i3;
                        i = i2;
                        value = i26;
                        break;
                    case 49:
                        this.editorAbsoluteX = a.getDimensionPixelOffset(attr, this.editorAbsoluteX);
                        int i27 = i3;
                        i = i2;
                        value = i27;
                        break;
                    case 50:
                        this.editorAbsoluteY = a.getDimensionPixelOffset(attr, this.editorAbsoluteY);
                        int i28 = i3;
                        i = i2;
                        value = i28;
                        break;
                }
                i4++;
                int i29 = i;
                i3 = value;
                i2 = i29;
            }
            a.recycle();
            validate();
        }

        public void validate() {
            this.isGuideline = false;
            this.horizontalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.verticalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            if (this.width == -2 && this.constrainedWidth) {
                this.horizontalDimensionFixed = false;
                this.matchConstraintDefaultWidth = 1;
            }
            if (this.height == -2 && this.constrainedHeight) {
                this.verticalDimensionFixed = false;
                this.matchConstraintDefaultHeight = 1;
            }
            if (this.width == 0 || this.width == -1) {
                this.horizontalDimensionFixed = false;
                if (this.width == 0 && this.matchConstraintDefaultWidth == 1) {
                    this.width = -2;
                    this.constrainedWidth = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                }
            }
            if (this.height == 0 || this.height == -1) {
                this.verticalDimensionFixed = false;
                if (this.height == 0 && this.matchConstraintDefaultHeight == 1) {
                    this.height = -2;
                    this.constrainedHeight = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                }
            }
            if (this.guidePercent != -1.0f || this.guideBegin != -1 || this.guideEnd != -1) {
                this.isGuideline = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                this.horizontalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                this.verticalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                if (!(this.widget instanceof Guideline)) {
                    this.widget = new Guideline();
                }
                ((Guideline) this.widget).setOrientation(this.orientation);
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.guideBegin = -1;
            this.guideEnd = -1;
            this.guidePercent = -1.0f;
            this.leftToLeft = -1;
            this.leftToRight = -1;
            this.rightToLeft = -1;
            this.rightToRight = -1;
            this.topToTop = -1;
            this.topToBottom = -1;
            this.bottomToTop = -1;
            this.bottomToBottom = -1;
            this.baselineToBaseline = -1;
            this.circleConstraint = -1;
            this.circleRadius = 0;
            this.circleAngle = 0.0f;
            this.startToEnd = -1;
            this.startToStart = -1;
            this.endToStart = -1;
            this.endToEnd = -1;
            this.goneLeftMargin = -1;
            this.goneTopMargin = -1;
            this.goneRightMargin = -1;
            this.goneBottomMargin = -1;
            this.goneStartMargin = -1;
            this.goneEndMargin = -1;
            this.horizontalBias = 0.5f;
            this.verticalBias = 0.5f;
            this.dimensionRatio = null;
            this.dimensionRatioValue = 0.0f;
            this.dimensionRatioSide = 1;
            this.horizontalWeight = -1.0f;
            this.verticalWeight = -1.0f;
            this.horizontalChainStyle = 0;
            this.verticalChainStyle = 0;
            this.matchConstraintDefaultWidth = 0;
            this.matchConstraintDefaultHeight = 0;
            this.matchConstraintMinWidth = 0;
            this.matchConstraintMinHeight = 0;
            this.matchConstraintMaxWidth = 0;
            this.matchConstraintMaxHeight = 0;
            this.matchConstraintPercentWidth = 1.0f;
            this.matchConstraintPercentHeight = 1.0f;
            this.editorAbsoluteX = -1;
            this.editorAbsoluteY = -1;
            this.orientation = -1;
            this.constrainedWidth = false;
            this.constrainedHeight = false;
            this.horizontalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.verticalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.needsBaseline = false;
            this.isGuideline = false;
            this.isHelper = false;
            this.isInPlaceholder = false;
            this.resolvedLeftToLeft = -1;
            this.resolvedLeftToRight = -1;
            this.resolvedRightToLeft = -1;
            this.resolvedRightToRight = -1;
            this.resolveGoneLeftMargin = -1;
            this.resolveGoneRightMargin = -1;
            this.resolvedHorizontalBias = 0.5f;
            this.widget = new ConstraintWidget();
            this.helped = false;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.guideBegin = -1;
            this.guideEnd = -1;
            this.guidePercent = -1.0f;
            this.leftToLeft = -1;
            this.leftToRight = -1;
            this.rightToLeft = -1;
            this.rightToRight = -1;
            this.topToTop = -1;
            this.topToBottom = -1;
            this.bottomToTop = -1;
            this.bottomToBottom = -1;
            this.baselineToBaseline = -1;
            this.circleConstraint = -1;
            this.circleRadius = 0;
            this.circleAngle = 0.0f;
            this.startToEnd = -1;
            this.startToStart = -1;
            this.endToStart = -1;
            this.endToEnd = -1;
            this.goneLeftMargin = -1;
            this.goneTopMargin = -1;
            this.goneRightMargin = -1;
            this.goneBottomMargin = -1;
            this.goneStartMargin = -1;
            this.goneEndMargin = -1;
            this.horizontalBias = 0.5f;
            this.verticalBias = 0.5f;
            this.dimensionRatio = null;
            this.dimensionRatioValue = 0.0f;
            this.dimensionRatioSide = 1;
            this.horizontalWeight = -1.0f;
            this.verticalWeight = -1.0f;
            this.horizontalChainStyle = 0;
            this.verticalChainStyle = 0;
            this.matchConstraintDefaultWidth = 0;
            this.matchConstraintDefaultHeight = 0;
            this.matchConstraintMinWidth = 0;
            this.matchConstraintMinHeight = 0;
            this.matchConstraintMaxWidth = 0;
            this.matchConstraintMaxHeight = 0;
            this.matchConstraintPercentWidth = 1.0f;
            this.matchConstraintPercentHeight = 1.0f;
            this.editorAbsoluteX = -1;
            this.editorAbsoluteY = -1;
            this.orientation = -1;
            this.constrainedWidth = false;
            this.constrainedHeight = false;
            this.horizontalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.verticalDimensionFixed = ConstraintLayout.USE_CONSTRAINTS_HELPER;
            this.needsBaseline = false;
            this.isGuideline = false;
            this.isHelper = false;
            this.isInPlaceholder = false;
            this.resolvedLeftToLeft = -1;
            this.resolvedLeftToRight = -1;
            this.resolvedRightToLeft = -1;
            this.resolvedRightToRight = -1;
            this.resolveGoneLeftMargin = -1;
            this.resolveGoneRightMargin = -1;
            this.resolvedHorizontalBias = 0.5f;
            this.widget = new ConstraintWidget();
            this.helped = false;
        }

        @Override // android.view.ViewGroup.MarginLayoutParams, android.view.ViewGroup.LayoutParams
        public void resolveLayoutDirection(int layoutDirection) {
            int preLeftMargin = this.leftMargin;
            int preRightMargin = this.rightMargin;
            super.resolveLayoutDirection(layoutDirection);
            this.resolvedRightToLeft = -1;
            this.resolvedRightToRight = -1;
            this.resolvedLeftToLeft = -1;
            this.resolvedLeftToRight = -1;
            this.resolveGoneLeftMargin = -1;
            this.resolveGoneRightMargin = -1;
            this.resolveGoneLeftMargin = this.goneLeftMargin;
            this.resolveGoneRightMargin = this.goneRightMargin;
            this.resolvedHorizontalBias = this.horizontalBias;
            this.resolvedGuideBegin = this.guideBegin;
            this.resolvedGuideEnd = this.guideEnd;
            this.resolvedGuidePercent = this.guidePercent;
            boolean isRtl = 1 == getLayoutDirection();
            if (isRtl) {
                boolean startEndDefined = false;
                int i = this.startToEnd;
                if (i != -1) {
                    this.resolvedRightToLeft = i;
                    startEndDefined = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                } else {
                    int i2 = this.startToStart;
                    if (i2 != -1) {
                        this.resolvedRightToRight = i2;
                        startEndDefined = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                    }
                }
                int i3 = this.endToStart;
                if (i3 != -1) {
                    this.resolvedLeftToRight = i3;
                    startEndDefined = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                }
                int i4 = this.endToEnd;
                if (i4 != -1) {
                    this.resolvedLeftToLeft = i4;
                    startEndDefined = ConstraintLayout.USE_CONSTRAINTS_HELPER;
                }
                int i5 = this.goneStartMargin;
                if (i5 != -1) {
                    this.resolveGoneRightMargin = i5;
                }
                int i6 = this.goneEndMargin;
                if (i6 != -1) {
                    this.resolveGoneLeftMargin = i6;
                }
                if (startEndDefined) {
                    this.resolvedHorizontalBias = 1.0f - this.horizontalBias;
                }
                if (this.isGuideline && this.orientation == 1) {
                    float f = this.guidePercent;
                    if (f != -1.0f) {
                        this.resolvedGuidePercent = 1.0f - f;
                        this.resolvedGuideBegin = -1;
                        this.resolvedGuideEnd = -1;
                    } else {
                        int i7 = this.guideBegin;
                        if (i7 != -1) {
                            this.resolvedGuideEnd = i7;
                            this.resolvedGuideBegin = -1;
                            this.resolvedGuidePercent = -1.0f;
                        } else {
                            int i8 = this.guideEnd;
                            if (i8 != -1) {
                                this.resolvedGuideBegin = i8;
                                this.resolvedGuideEnd = -1;
                                this.resolvedGuidePercent = -1.0f;
                            }
                        }
                    }
                }
            } else {
                int i9 = this.startToEnd;
                if (i9 != -1) {
                    this.resolvedLeftToRight = i9;
                }
                int i10 = this.startToStart;
                if (i10 != -1) {
                    this.resolvedLeftToLeft = i10;
                }
                int i11 = this.endToStart;
                if (i11 != -1) {
                    this.resolvedRightToLeft = i11;
                }
                int i12 = this.endToEnd;
                if (i12 != -1) {
                    this.resolvedRightToRight = i12;
                }
                int i13 = this.goneStartMargin;
                if (i13 != -1) {
                    this.resolveGoneLeftMargin = i13;
                }
                int i14 = this.goneEndMargin;
                if (i14 != -1) {
                    this.resolveGoneRightMargin = i14;
                }
            }
            if (this.endToStart == -1 && this.endToEnd == -1 && this.startToStart == -1 && this.startToEnd == -1) {
                int i15 = this.rightToLeft;
                if (i15 != -1) {
                    this.resolvedRightToLeft = i15;
                    if (this.rightMargin <= 0 && preRightMargin > 0) {
                        this.rightMargin = preRightMargin;
                    }
                } else {
                    int i16 = this.rightToRight;
                    if (i16 != -1) {
                        this.resolvedRightToRight = i16;
                        if (this.rightMargin <= 0 && preRightMargin > 0) {
                            this.rightMargin = preRightMargin;
                        }
                    }
                }
                int i17 = this.leftToLeft;
                if (i17 != -1) {
                    this.resolvedLeftToLeft = i17;
                    if (this.leftMargin <= 0 && preLeftMargin > 0) {
                        this.leftMargin = preLeftMargin;
                        return;
                    }
                    return;
                }
                int i18 = this.leftToRight;
                if (i18 != -1) {
                    this.resolvedLeftToRight = i18;
                    if (this.leftMargin <= 0 && preLeftMargin > 0) {
                        this.leftMargin = preLeftMargin;
                    }
                }
            }
        }
    }

    @Override // android.view.View, android.view.ViewParent
    public void requestLayout() {
        super.requestLayout();
        this.mDirtyHierarchy = USE_CONSTRAINTS_HELPER;
        this.mLastMeasureWidth = -1;
        this.mLastMeasureHeight = -1;
        this.mLastMeasureWidthSize = -1;
        this.mLastMeasureHeightSize = -1;
        this.mLastMeasureWidthMode = 0;
        this.mLastMeasureHeightMode = 0;
    }

    @Override // android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return false;
    }
}
