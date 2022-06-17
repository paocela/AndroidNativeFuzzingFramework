package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;
import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
/* loaded from: classes.dex */
public class ConstraintHorizontalLayout extends ConstraintWidgetContainer {
    private ContentAlignment mAlignment = ContentAlignment.MIDDLE;

    /* loaded from: classes.dex */
    public enum ContentAlignment {
        BEGIN,
        MIDDLE,
        END,
        TOP,
        VERTICAL_MIDDLE,
        BOTTOM,
        LEFT,
        RIGHT
    }

    public ConstraintHorizontalLayout() {
    }

    public ConstraintHorizontalLayout(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ConstraintHorizontalLayout(int width, int height) {
        super(width, height);
    }

    @Override // androidx.constraintlayout.solver.widgets.ConstraintWidget
    public void addToSolver(LinearSystem system) {
        ConstraintAnchor.Strength strength;
        if (this.mChildren.size() != 0) {
            ConstraintWidget previous = this;
            int mChildrenSize = this.mChildren.size();
            for (int i = 0; i < mChildrenSize; i++) {
                ConstraintWidget widget = this.mChildren.get(i);
                if (previous != this) {
                    widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT);
                    previous.connect(ConstraintAnchor.Type.RIGHT, widget, ConstraintAnchor.Type.LEFT);
                } else {
                    ConstraintAnchor.Strength strength2 = ConstraintAnchor.Strength.STRONG;
                    if (this.mAlignment != ContentAlignment.END) {
                        strength = strength2;
                    } else {
                        ConstraintAnchor.Strength strength3 = ConstraintAnchor.Strength.WEAK;
                        strength = strength3;
                    }
                    widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.LEFT, 0, strength);
                }
                widget.connect(ConstraintAnchor.Type.TOP, this, ConstraintAnchor.Type.TOP);
                widget.connect(ConstraintAnchor.Type.BOTTOM, this, ConstraintAnchor.Type.BOTTOM);
                previous = widget;
            }
            if (previous != this) {
                ConstraintAnchor.Strength strength4 = ConstraintAnchor.Strength.STRONG;
                if (this.mAlignment == ContentAlignment.BEGIN) {
                    strength4 = ConstraintAnchor.Strength.WEAK;
                }
                previous.connect(ConstraintAnchor.Type.RIGHT, this, ConstraintAnchor.Type.RIGHT, 0, strength4);
            }
        }
        super.addToSolver(system);
    }
}
