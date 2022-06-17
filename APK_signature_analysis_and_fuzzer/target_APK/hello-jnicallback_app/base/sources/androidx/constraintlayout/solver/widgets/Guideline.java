package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;
import androidx.constraintlayout.solver.SolverVariable;
import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class Guideline extends ConstraintWidget {
    public static final int HORIZONTAL = 0;
    public static final int RELATIVE_BEGIN = 1;
    public static final int RELATIVE_END = 2;
    public static final int RELATIVE_PERCENT = 0;
    public static final int RELATIVE_UNKNWON = -1;
    public static final int VERTICAL = 1;
    protected float mRelativePercent = -1.0f;
    protected int mRelativeBegin = -1;
    protected int mRelativeEnd = -1;
    private ConstraintAnchor mAnchor = this.mTop;
    private int mOrientation = 0;
    private boolean mIsPositionRelaxed = false;
    private int mMinimumPosition = 0;
    private Rectangle mHead = new Rectangle();
    private int mHeadSize = 8;

    public Guideline() {
        this.mAnchors.clear();
        this.mAnchors.add(this.mAnchor);
        int count = this.mListAnchors.length;
        for (int i = 0; i < count; i++) {
            this.mListAnchors[i] = this.mAnchor;
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public boolean allowedInBarrier() {
        return true;
    }

    public int getRelativeBehaviour() {
        if (this.mRelativePercent != -1.0f) {
            return 0;
        }
        if (this.mRelativeBegin != -1) {
            return 1;
        }
        return this.mRelativeEnd != -1 ? 2 : -1;
    }

    public Rectangle getHead() {
        Rectangle rectangle = this.mHead;
        int drawX = getDrawX() - this.mHeadSize;
        int drawY = getDrawY();
        int i = this.mHeadSize;
        rectangle.setBounds(drawX, drawY - (i * 2), i * 2, i * 2);
        if (getOrientation() == 0) {
            Rectangle rectangle2 = this.mHead;
            int drawX2 = getDrawX() - (this.mHeadSize * 2);
            int drawY2 = getDrawY();
            int i2 = this.mHeadSize;
            rectangle2.setBounds(drawX2, drawY2 - i2, i2 * 2, i2 * 2);
        }
        return this.mHead;
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation == orientation) {
            return;
        }
        this.mOrientation = orientation;
        this.mAnchors.clear();
        if (this.mOrientation == 1) {
            this.mAnchor = this.mLeft;
        } else {
            this.mAnchor = this.mTop;
        }
        this.mAnchors.add(this.mAnchor);
        int count = this.mListAnchors.length;
        for (int i = 0; i < count; i++) {
            this.mListAnchors[i] = this.mAnchor;
        }
    }

    public ConstraintAnchor getAnchor() {
        return this.mAnchor;
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public String getType() {
        return "Guideline";
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setMinimumPosition(int minimum) {
        this.mMinimumPosition = minimum;
    }

    public void setPositionRelaxed(boolean value) {
        if (this.mIsPositionRelaxed == value) {
            return;
        }
        this.mIsPositionRelaxed = value;
    }

    /* renamed from: androidx.constraintlayout.solver.widgets.Guideline$1 */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type;

        static {
            int[] iArr = new int[ConstraintAnchor.Type.values().length];
            $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type = iArr;
            try {
                iArr[ConstraintAnchor.Type.LEFT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.RIGHT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.TOP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.BOTTOM.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.BASELINE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.CENTER.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.CENTER_X.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.CENTER_Y.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[ConstraintAnchor.Type.NONE.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public ConstraintAnchor getAnchor(ConstraintAnchor.Type anchorType) {
        switch (AnonymousClass1.$SwitchMap$androidx$constraintlayout$solver$widgets$ConstraintAnchor$Type[anchorType.ordinal()]) {
            case 1:
            case 2:
                if (this.mOrientation == 1) {
                    return this.mAnchor;
                }
                break;
            case 3:
            case 4:
                if (this.mOrientation == 0) {
                    return this.mAnchor;
                }
                break;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return null;
        }
        throw new AssertionError(anchorType.name());
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public ArrayList<ConstraintAnchor> getAnchors() {
        return this.mAnchors;
    }

    public void setGuidePercent(int value) {
        setGuidePercent(value / 100.0f);
    }

    public void setGuidePercent(float value) {
        if (value > -1.0f) {
            this.mRelativePercent = value;
            this.mRelativeBegin = -1;
            this.mRelativeEnd = -1;
        }
    }

    public void setGuideBegin(int value) {
        if (value > -1) {
            this.mRelativePercent = -1.0f;
            this.mRelativeBegin = value;
            this.mRelativeEnd = -1;
        }
    }

    public void setGuideEnd(int value) {
        if (value > -1) {
            this.mRelativePercent = -1.0f;
            this.mRelativeBegin = -1;
            this.mRelativeEnd = value;
        }
    }

    public float getRelativePercent() {
        return this.mRelativePercent;
    }

    public int getRelativeBegin() {
        return this.mRelativeBegin;
    }

    public int getRelativeEnd() {
        return this.mRelativeEnd;
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void analyze(int optimizationLevel) {
        ConstraintWidget constraintWidgetContainer = getParent();
        if (constraintWidgetContainer == null) {
            return;
        }
        if (getOrientation() == 1) {
            this.mTop.getResolutionNode().dependsOn(1, constraintWidgetContainer.mTop.getResolutionNode(), 0);
            this.mBottom.getResolutionNode().dependsOn(1, constraintWidgetContainer.mTop.getResolutionNode(), 0);
            if (this.mRelativeBegin != -1) {
                this.mLeft.getResolutionNode().dependsOn(1, constraintWidgetContainer.mLeft.getResolutionNode(), this.mRelativeBegin);
                this.mRight.getResolutionNode().dependsOn(1, constraintWidgetContainer.mLeft.getResolutionNode(), this.mRelativeBegin);
                return;
            } else if (this.mRelativeEnd != -1) {
                this.mLeft.getResolutionNode().dependsOn(1, constraintWidgetContainer.mRight.getResolutionNode(), -this.mRelativeEnd);
                this.mRight.getResolutionNode().dependsOn(1, constraintWidgetContainer.mRight.getResolutionNode(), -this.mRelativeEnd);
                return;
            } else if (this.mRelativePercent != -1.0f && constraintWidgetContainer.getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.FIXED) {
                int position = (int) (constraintWidgetContainer.mWidth * this.mRelativePercent);
                this.mLeft.getResolutionNode().dependsOn(1, constraintWidgetContainer.mLeft.getResolutionNode(), position);
                this.mRight.getResolutionNode().dependsOn(1, constraintWidgetContainer.mLeft.getResolutionNode(), position);
                return;
            } else {
                return;
            }
        }
        this.mLeft.getResolutionNode().dependsOn(1, constraintWidgetContainer.mLeft.getResolutionNode(), 0);
        this.mRight.getResolutionNode().dependsOn(1, constraintWidgetContainer.mLeft.getResolutionNode(), 0);
        if (this.mRelativeBegin != -1) {
            this.mTop.getResolutionNode().dependsOn(1, constraintWidgetContainer.mTop.getResolutionNode(), this.mRelativeBegin);
            this.mBottom.getResolutionNode().dependsOn(1, constraintWidgetContainer.mTop.getResolutionNode(), this.mRelativeBegin);
        } else if (this.mRelativeEnd != -1) {
            this.mTop.getResolutionNode().dependsOn(1, constraintWidgetContainer.mBottom.getResolutionNode(), -this.mRelativeEnd);
            this.mBottom.getResolutionNode().dependsOn(1, constraintWidgetContainer.mBottom.getResolutionNode(), -this.mRelativeEnd);
        } else if (this.mRelativePercent != -1.0f && constraintWidgetContainer.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.FIXED) {
            int position2 = (int) (constraintWidgetContainer.mHeight * this.mRelativePercent);
            this.mTop.getResolutionNode().dependsOn(1, constraintWidgetContainer.mTop.getResolutionNode(), position2);
            this.mBottom.getResolutionNode().dependsOn(1, constraintWidgetContainer.mTop.getResolutionNode(), position2);
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void addToSolver(LinearSystem system) {
        ConstraintWidgetContainer parent = (ConstraintWidgetContainer) getParent();
        if (parent == null) {
            return;
        }
        ConstraintAnchor begin = parent.getAnchor(ConstraintAnchor.Type.LEFT);
        ConstraintAnchor end = parent.getAnchor(ConstraintAnchor.Type.RIGHT);
        boolean z = true;
        boolean parentWrapContent = this.mParent != null && this.mParent.mListDimensionBehaviors[0] == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
        if (this.mOrientation == 0) {
            begin = parent.getAnchor(ConstraintAnchor.Type.TOP);
            end = parent.getAnchor(ConstraintAnchor.Type.BOTTOM);
            if (this.mParent == null || this.mParent.mListDimensionBehaviors[1] != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                z = false;
            }
            parentWrapContent = z;
        }
        if (this.mRelativeBegin != -1) {
            SolverVariable guide = system.createObjectVariable(this.mAnchor);
            SolverVariable parentLeft = system.createObjectVariable(begin);
            system.addEquality(guide, parentLeft, this.mRelativeBegin, 6);
            if (parentWrapContent) {
                system.addGreaterThan(system.createObjectVariable(end), guide, 0, 5);
            }
        } else if (this.mRelativeEnd != -1) {
            SolverVariable guide2 = system.createObjectVariable(this.mAnchor);
            SolverVariable parentRight = system.createObjectVariable(end);
            system.addEquality(guide2, parentRight, -this.mRelativeEnd, 6);
            if (parentWrapContent) {
                system.addGreaterThan(guide2, system.createObjectVariable(begin), 0, 5);
                system.addGreaterThan(parentRight, guide2, 0, 5);
            }
        } else if (this.mRelativePercent != -1.0f) {
            SolverVariable guide3 = system.createObjectVariable(this.mAnchor);
            SolverVariable parentLeft2 = system.createObjectVariable(begin);
            system.addConstraint(LinearSystem.createRowDimensionPercent(system, guide3, parentLeft2, system.createObjectVariable(end), this.mRelativePercent, this.mIsPositionRelaxed));
        }
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void updateFromSolver(LinearSystem system) {
        if (getParent() == null) {
            return;
        }
        int value = system.getObjectVariableValue(this.mAnchor);
        if (this.mOrientation == 1) {
            setX(value);
            setY(0);
            setHeight(getParent().getHeight());
            setWidth(0);
            return;
        }
        setX(0);
        setY(value);
        setWidth(getParent().getWidth());
        setHeight(0);
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void setDrawOrigin(int x, int y) {
        if (this.mOrientation == 1) {
            int position = x - this.mOffsetX;
            if (this.mRelativeBegin != -1) {
                setGuideBegin(position);
                return;
            } else if (this.mRelativeEnd != -1) {
                setGuideEnd(getParent().getWidth() - position);
                return;
            } else if (this.mRelativePercent != -1.0f) {
                float percent = position / getParent().getWidth();
                setGuidePercent(percent);
                return;
            } else {
                return;
            }
        }
        int position2 = y - this.mOffsetY;
        if (this.mRelativeBegin != -1) {
            setGuideBegin(position2);
        } else if (this.mRelativeEnd != -1) {
            setGuideEnd(getParent().getHeight() - position2);
        } else if (this.mRelativePercent != -1.0f) {
            float percent2 = position2 / getParent().getHeight();
            setGuidePercent(percent2);
        }
    }

    public void inferRelativePercentPosition() {
        float percent = getX() / getParent().getWidth();
        if (this.mOrientation == 0) {
            percent = getY() / getParent().getHeight();
        }
        setGuidePercent(percent);
    }

    void inferRelativeBeginPosition() {
        int position = getX();
        if (this.mOrientation == 0) {
            position = getY();
        }
        setGuideBegin(position);
    }

    void inferRelativeEndPosition() {
        int position = getParent().getWidth() - getX();
        if (this.mOrientation == 0) {
            position = getParent().getHeight() - getY();
        }
        setGuideEnd(position);
    }

    public void cyclePosition() {
        if (this.mRelativeBegin != -1) {
            inferRelativePercentPosition();
        } else if (this.mRelativePercent != -1.0f) {
            inferRelativeEndPosition();
        } else if (this.mRelativeEnd != -1) {
            inferRelativeBeginPosition();
        }
    }
}
