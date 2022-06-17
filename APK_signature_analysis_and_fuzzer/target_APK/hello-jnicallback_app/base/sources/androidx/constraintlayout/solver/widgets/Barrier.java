package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;
import androidx.constraintlayout.solver.SolverVariable;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class Barrier extends Helper {
    public static final int BOTTOM = 3;
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int TOP = 2;
    private int mBarrierType = 0;
    private ArrayList<ResolutionAnchor> mNodes = new ArrayList<>(4);
    private boolean mAllowsGoneWidget = true;

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public boolean allowedInBarrier() {
        return true;
    }

    public void setBarrierType(int barrierType) {
        this.mBarrierType = barrierType;
    }

    public void setAllowsGoneWidget(boolean allowsGoneWidget) {
        this.mAllowsGoneWidget = allowsGoneWidget;
    }

    public boolean allowsGoneWidget() {
        return this.mAllowsGoneWidget;
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void resetResolutionNodes() {
        super.resetResolutionNodes();
        this.mNodes.clear();
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void analyze(int optimizationLevel) {
        ResolutionAnchor node;
        if (this.mParent == null || !((ConstraintWidgetContainer) this.mParent).optimizeFor(2)) {
            return;
        }
        switch (this.mBarrierType) {
            case 0:
                node = this.mLeft.getResolutionNode();
                break;
            case 1:
                node = this.mRight.getResolutionNode();
                break;
            case 2:
                node = this.mTop.getResolutionNode();
                break;
            case 3:
                node = this.mBottom.getResolutionNode();
                break;
            default:
                return;
        }
        node.setType(5);
        int i = this.mBarrierType;
        if (i == 0 || i == 1) {
            this.mTop.getResolutionNode().resolve(null, 0.0f);
            this.mBottom.getResolutionNode().resolve(null, 0.0f);
        } else {
            this.mLeft.getResolutionNode().resolve(null, 0.0f);
            this.mRight.getResolutionNode().resolve(null, 0.0f);
        }
        this.mNodes.clear();
        for (int i2 = 0; i2 < this.mWidgetsCount; i2++) {
            ConstraintWidget widget = this.mWidgets[i2];
            if (this.mAllowsGoneWidget || widget.allowedInBarrier()) {
                ResolutionAnchor depends = null;
                switch (this.mBarrierType) {
                    case 0:
                        depends = widget.mLeft.getResolutionNode();
                        break;
                    case 1:
                        depends = widget.mRight.getResolutionNode();
                        break;
                    case 2:
                        depends = widget.mTop.getResolutionNode();
                        break;
                    case 3:
                        depends = widget.mBottom.getResolutionNode();
                        break;
                }
                if (depends != null) {
                    this.mNodes.add(depends);
                    depends.addDependent(node);
                }
            }
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void resolve() {
        ResolutionAnchor node;
        float value = 0.0f;
        switch (this.mBarrierType) {
            case 0:
                node = this.mLeft.getResolutionNode();
                value = Float.MAX_VALUE;
                break;
            case 1:
                node = this.mRight.getResolutionNode();
                break;
            case 2:
                node = this.mTop.getResolutionNode();
                value = Float.MAX_VALUE;
                break;
            case 3:
                node = this.mBottom.getResolutionNode();
                break;
            default:
                return;
        }
        int count = this.mNodes.size();
        ResolutionAnchor resolvedTarget = null;
        for (int i = 0; i < count; i++) {
            ResolutionAnchor n = this.mNodes.get(i);
            if (n.state != 1) {
                return;
            }
            int i2 = this.mBarrierType;
            if (i2 == 0 || i2 == 2) {
                if (n.resolvedOffset < value) {
                    value = n.resolvedOffset;
                    resolvedTarget = n.resolvedTarget;
                }
            } else if (n.resolvedOffset > value) {
                value = n.resolvedOffset;
                resolvedTarget = n.resolvedTarget;
            }
        }
        if (LinearSystem.getMetrics() != null) {
            LinearSystem.getMetrics().barrierConnectionResolved++;
        }
        node.resolvedTarget = resolvedTarget;
        node.resolvedOffset = value;
        node.didResolve();
        switch (this.mBarrierType) {
            case 0:
                this.mRight.getResolutionNode().resolve(resolvedTarget, value);
                return;
            case 1:
                this.mLeft.getResolutionNode().resolve(resolvedTarget, value);
                return;
            case 2:
                this.mBottom.getResolutionNode().resolve(resolvedTarget, value);
                return;
            case 3:
                this.mTop.getResolutionNode().resolve(resolvedTarget, value);
                return;
            default:
                return;
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void addToSolver(LinearSystem system) {
        this.mListAnchors[0] = this.mLeft;
        this.mListAnchors[2] = this.mTop;
        this.mListAnchors[1] = this.mRight;
        this.mListAnchors[3] = this.mBottom;
        for (int i = 0; i < this.mListAnchors.length; i++) {
            this.mListAnchors[i].mSolverVariable = system.createObjectVariable(this.mListAnchors[i]);
        }
        int i2 = this.mBarrierType;
        if (i2 >= 0 && i2 < 4) {
            ConstraintAnchor position = this.mListAnchors[this.mBarrierType];
            boolean hasMatchConstraintWidgets = false;
            int i3 = 0;
            while (true) {
                if (i3 >= this.mWidgetsCount) {
                    break;
                }
                ConstraintWidget widget = this.mWidgets[i3];
                if (this.mAllowsGoneWidget || widget.allowedInBarrier()) {
                    int i4 = this.mBarrierType;
                    if ((i4 == 0 || i4 == 1) && widget.getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                        hasMatchConstraintWidgets = true;
                        break;
                    }
                    int i5 = this.mBarrierType;
                    if ((i5 == 2 || i5 == 3) && widget.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                        hasMatchConstraintWidgets = true;
                        break;
                    }
                }
                i3++;
            }
            int i6 = this.mBarrierType;
            if (i6 == 0 || i6 == 1) {
                if (getParent().getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                    hasMatchConstraintWidgets = false;
                }
            } else if (getParent().getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                hasMatchConstraintWidgets = false;
            }
            for (int i7 = 0; i7 < this.mWidgetsCount; i7++) {
                ConstraintWidget widget2 = this.mWidgets[i7];
                if (this.mAllowsGoneWidget || widget2.allowedInBarrier()) {
                    SolverVariable target = system.createObjectVariable(widget2.mListAnchors[this.mBarrierType]);
                    widget2.mListAnchors[this.mBarrierType].mSolverVariable = target;
                    int i8 = this.mBarrierType;
                    if (i8 == 0 || i8 == 2) {
                        system.addLowerBarrier(position.mSolverVariable, target, hasMatchConstraintWidgets);
                    } else {
                        system.addGreaterBarrier(position.mSolverVariable, target, hasMatchConstraintWidgets);
                    }
                }
            }
            int i9 = this.mBarrierType;
            if (i9 == 0) {
                system.addEquality(this.mRight.mSolverVariable, this.mLeft.mSolverVariable, 0, 6);
                if (!hasMatchConstraintWidgets) {
                    system.addEquality(this.mLeft.mSolverVariable, this.mParent.mRight.mSolverVariable, 0, 5);
                }
            } else if (i9 == 1) {
                system.addEquality(this.mLeft.mSolverVariable, this.mRight.mSolverVariable, 0, 6);
                if (!hasMatchConstraintWidgets) {
                    system.addEquality(this.mLeft.mSolverVariable, this.mParent.mLeft.mSolverVariable, 0, 5);
                }
            } else if (i9 == 2) {
                system.addEquality(this.mBottom.mSolverVariable, this.mTop.mSolverVariable, 0, 6);
                if (!hasMatchConstraintWidgets) {
                    system.addEquality(this.mTop.mSolverVariable, this.mParent.mBottom.mSolverVariable, 0, 5);
                }
            } else if (i9 == 3) {
                system.addEquality(this.mTop.mSolverVariable, this.mBottom.mSolverVariable, 0, 6);
                if (!hasMatchConstraintWidgets) {
                    system.addEquality(this.mTop.mSolverVariable, this.mParent.mTop.mSolverVariable, 0, 5);
                }
            }
        }
    }
}
