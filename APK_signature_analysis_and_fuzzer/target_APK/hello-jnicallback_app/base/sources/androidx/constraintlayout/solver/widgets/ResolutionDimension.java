package androidx.constraintlayout.solver.widgets;
/* loaded from: classes.dex */
public class ResolutionDimension extends ResolutionNode {
    float value = 0.0f;

    @Override // androidx.constraintlayout.solver.widgets.ResolutionNode
    public void reset() {
        super.reset();
        this.value = 0.0f;
    }

    public void resolve(int value) {
        if (this.state == 0 || this.value != value) {
            this.value = value;
            if (this.state == 1) {
                invalidate();
            }
            didResolve();
        }
    }

    public void remove() {
        this.state = 2;
    }
}
