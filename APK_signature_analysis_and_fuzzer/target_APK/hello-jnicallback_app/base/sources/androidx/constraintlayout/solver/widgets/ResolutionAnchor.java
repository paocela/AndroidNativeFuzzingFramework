package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;
import androidx.constraintlayout.solver.SolverVariable;
import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
/* loaded from: classes.dex */
public class ResolutionAnchor extends ResolutionNode {
    public static final int BARRIER_CONNECTION = 5;
    public static final int CENTER_CONNECTION = 2;
    public static final int CHAIN_CONNECTION = 4;
    public static final int DIRECT_CONNECTION = 1;
    public static final int MATCH_CONNECTION = 3;
    public static final int UNCONNECTED = 0;
    float computedValue;
    ConstraintAnchor myAnchor;
    float offset;
    private ResolutionAnchor opposite;
    private float oppositeOffset;
    float resolvedOffset;
    ResolutionAnchor resolvedTarget;
    ResolutionAnchor target;
    int type = 0;
    private ResolutionDimension dimension = null;
    private int dimensionMultiplier = 1;
    private ResolutionDimension oppositeDimension = null;
    private int oppositeDimensionMultiplier = 1;

    public ResolutionAnchor(ConstraintAnchor anchor) {
        this.myAnchor = anchor;
    }

    @Override // androidx.constraintlayout.solver.widgets.ResolutionNode
    public void remove(ResolutionDimension resolutionDimension) {
        ResolutionDimension resolutionDimension2 = this.dimension;
        if (resolutionDimension2 == resolutionDimension) {
            this.dimension = null;
            this.offset = this.dimensionMultiplier;
        } else if (resolutionDimension2 == this.oppositeDimension) {
            this.oppositeDimension = null;
            this.oppositeOffset = this.oppositeDimensionMultiplier;
        }
        resolve();
    }

    public String toString() {
        if (this.state == 1) {
            if (this.resolvedTarget == this) {
                return "[" + this.myAnchor + ", RESOLVED: " + this.resolvedOffset + "]  type: " + sType(this.type);
            }
            return "[" + this.myAnchor + ", RESOLVED: " + this.resolvedTarget + ":" + this.resolvedOffset + "] type: " + sType(this.type);
        }
        return "{ " + this.myAnchor + " UNRESOLVED} type: " + sType(this.type);
    }

    public void resolve(ResolutionAnchor target, float offset) {
        if (this.state == 0 || (this.resolvedTarget != target && this.resolvedOffset != offset)) {
            this.resolvedTarget = target;
            this.resolvedOffset = offset;
            if (this.state == 1) {
                invalidate();
            }
            didResolve();
        }
    }

    String sType(int type) {
        if (type == 1) {
            return "DIRECT";
        }
        if (type == 2) {
            return "CENTER";
        }
        if (type == 3) {
            return "MATCH";
        }
        if (type == 4) {
            return "CHAIN";
        }
        if (type == 5) {
            return "BARRIER";
        }
        return "UNCONNECTED";
    }

    @Override // androidx.constraintlayout.solver.widgets.ResolutionNode
    public void resolve() {
        ResolutionAnchor resolutionAnchor;
        ResolutionAnchor resolutionAnchor2;
        ResolutionAnchor resolutionAnchor3;
        ResolutionAnchor resolutionAnchor4;
        ResolutionAnchor resolutionAnchor5;
        ResolutionAnchor resolutionAnchor6;
        float distance;
        float distance2;
        float percent;
        ResolutionAnchor resolutionAnchor7;
        boolean isEndAnchor = true;
        if (this.state == 1 || this.type == 4) {
            return;
        }
        ResolutionDimension resolutionDimension = this.dimension;
        if (resolutionDimension != null) {
            if (resolutionDimension.state != 1) {
                return;
            }
            this.offset = this.dimensionMultiplier * this.dimension.value;
        }
        ResolutionDimension resolutionDimension2 = this.oppositeDimension;
        if (resolutionDimension2 != null) {
            if (resolutionDimension2.state != 1) {
                return;
            }
            this.oppositeOffset = this.oppositeDimensionMultiplier * this.oppositeDimension.value;
        }
        if (this.type == 1 && ((resolutionAnchor7 = this.target) == null || resolutionAnchor7.state == 1)) {
            ResolutionAnchor resolutionAnchor8 = this.target;
            if (resolutionAnchor8 == null) {
                this.resolvedTarget = this;
                this.resolvedOffset = this.offset;
            } else {
                this.resolvedTarget = resolutionAnchor8.resolvedTarget;
                this.resolvedOffset = resolutionAnchor8.resolvedOffset + this.offset;
            }
            didResolve();
        } else if (this.type != 2 || (resolutionAnchor4 = this.target) == null || resolutionAnchor4.state != 1 || (resolutionAnchor5 = this.opposite) == null || (resolutionAnchor6 = resolutionAnchor5.target) == null || resolutionAnchor6.state != 1) {
            if (this.type == 3 && (resolutionAnchor = this.target) != null && resolutionAnchor.state == 1 && (resolutionAnchor2 = this.opposite) != null && (resolutionAnchor3 = resolutionAnchor2.target) != null && resolutionAnchor3.state == 1) {
                if (LinearSystem.getMetrics() != null) {
                    LinearSystem.getMetrics().matchConnectionResolved++;
                }
                ResolutionAnchor resolutionAnchor9 = this.target;
                this.resolvedTarget = resolutionAnchor9.resolvedTarget;
                ResolutionAnchor resolutionAnchor10 = this.opposite;
                ResolutionAnchor resolutionAnchor11 = resolutionAnchor10.target;
                resolutionAnchor10.resolvedTarget = resolutionAnchor11.resolvedTarget;
                this.resolvedOffset = resolutionAnchor9.resolvedOffset + this.offset;
                resolutionAnchor10.resolvedOffset = resolutionAnchor11.resolvedOffset + resolutionAnchor10.offset;
                didResolve();
                this.opposite.didResolve();
            } else if (this.type == 5) {
                this.myAnchor.mOwner.resolve();
            }
        } else {
            if (LinearSystem.getMetrics() != null) {
                LinearSystem.getMetrics().centerConnectionResolved++;
            }
            this.resolvedTarget = this.target.resolvedTarget;
            ResolutionAnchor resolutionAnchor12 = this.opposite;
            resolutionAnchor12.resolvedTarget = resolutionAnchor12.target.resolvedTarget;
            if (this.myAnchor.mType != ConstraintAnchor.Type.RIGHT && this.myAnchor.mType != ConstraintAnchor.Type.BOTTOM) {
                isEndAnchor = false;
            }
            if (isEndAnchor) {
                distance = this.target.resolvedOffset - this.opposite.target.resolvedOffset;
            } else {
                distance = this.opposite.target.resolvedOffset - this.target.resolvedOffset;
            }
            if (this.myAnchor.mType == ConstraintAnchor.Type.LEFT || this.myAnchor.mType == ConstraintAnchor.Type.RIGHT) {
                distance2 = distance - this.myAnchor.mOwner.getWidth();
                percent = this.myAnchor.mOwner.mHorizontalBiasPercent;
            } else {
                distance2 = distance - this.myAnchor.mOwner.getHeight();
                percent = this.myAnchor.mOwner.mVerticalBiasPercent;
            }
            int margin = this.myAnchor.getMargin();
            int oppositeMargin = this.opposite.myAnchor.getMargin();
            if (this.myAnchor.getTarget() == this.opposite.myAnchor.getTarget()) {
                percent = 0.5f;
                margin = 0;
                oppositeMargin = 0;
            }
            float distance3 = (distance2 - margin) - oppositeMargin;
            if (isEndAnchor) {
                ResolutionAnchor resolutionAnchor13 = this.opposite;
                resolutionAnchor13.resolvedOffset = resolutionAnchor13.target.resolvedOffset + oppositeMargin + (distance3 * percent);
                this.resolvedOffset = (this.target.resolvedOffset - margin) - ((1.0f - percent) * distance3);
            } else {
                this.resolvedOffset = this.target.resolvedOffset + margin + (distance3 * percent);
                ResolutionAnchor resolutionAnchor14 = this.opposite;
                resolutionAnchor14.resolvedOffset = (resolutionAnchor14.target.resolvedOffset - oppositeMargin) - ((1.0f - percent) * distance3);
            }
            didResolve();
            this.opposite.didResolve();
        }
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override // androidx.constraintlayout.solver.widgets.ResolutionNode
    public void reset() {
        super.reset();
        this.target = null;
        this.offset = 0.0f;
        this.dimension = null;
        this.dimensionMultiplier = 1;
        this.oppositeDimension = null;
        this.oppositeDimensionMultiplier = 1;
        this.resolvedTarget = null;
        this.resolvedOffset = 0.0f;
        this.computedValue = 0.0f;
        this.opposite = null;
        this.oppositeOffset = 0.0f;
        this.type = 0;
    }

    public void update() {
        ConstraintAnchor targetAnchor = this.myAnchor.getTarget();
        if (targetAnchor == null) {
            return;
        }
        if (targetAnchor.getTarget() == this.myAnchor) {
            this.type = 4;
            targetAnchor.getResolutionNode().type = 4;
        }
        int margin = this.myAnchor.getMargin();
        if (this.myAnchor.mType == ConstraintAnchor.Type.RIGHT || this.myAnchor.mType == ConstraintAnchor.Type.BOTTOM) {
            margin = -margin;
        }
        dependsOn(targetAnchor.getResolutionNode(), margin);
    }

    public void dependsOn(int type, ResolutionAnchor node, int offset) {
        this.type = type;
        this.target = node;
        this.offset = offset;
        node.addDependent(this);
    }

    public void dependsOn(ResolutionAnchor node, int offset) {
        this.target = node;
        this.offset = offset;
        node.addDependent(this);
    }

    public void dependsOn(ResolutionAnchor node, int multiplier, ResolutionDimension dimension) {
        this.target = node;
        node.addDependent(this);
        this.dimension = dimension;
        this.dimensionMultiplier = multiplier;
        dimension.addDependent(this);
    }

    public void setOpposite(ResolutionAnchor opposite, float oppositeOffset) {
        this.opposite = opposite;
        this.oppositeOffset = oppositeOffset;
    }

    public void setOpposite(ResolutionAnchor opposite, int multiplier, ResolutionDimension dimension) {
        this.opposite = opposite;
        this.oppositeDimension = dimension;
        this.oppositeDimensionMultiplier = multiplier;
    }

    public void addResolvedValue(LinearSystem system) {
        SolverVariable sv = this.myAnchor.getSolverVariable();
        ResolutionAnchor resolutionAnchor = this.resolvedTarget;
        if (resolutionAnchor == null) {
            system.addEquality(sv, (int) (this.resolvedOffset + 0.5f));
            return;
        }
        SolverVariable v = system.createObjectVariable(resolutionAnchor.myAnchor);
        system.addEquality(sv, v, (int) (this.resolvedOffset + 0.5f), 6);
    }

    public float getResolvedValue() {
        return this.resolvedOffset;
    }
}
