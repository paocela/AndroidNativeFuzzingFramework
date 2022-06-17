package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class Snapshot {
    private ArrayList<Connection> mConnections = new ArrayList<>();
    private int mHeight;
    private int mWidth;
    private int mX;
    private int mY;

    /* loaded from: classes.dex */
    public static class Connection {
        private ConstraintAnchor mAnchor;
        private int mCreator;
        private int mMargin;
        private ConstraintAnchor.Strength mStrengh;
        private ConstraintAnchor mTarget;

        public Connection(ConstraintAnchor anchor) {
            this.mAnchor = anchor;
            this.mTarget = anchor.getTarget();
            this.mMargin = anchor.getMargin();
            this.mStrengh = anchor.getStrength();
            this.mCreator = anchor.getConnectionCreator();
        }

        public void updateFrom(ConstraintWidget widget) {
            ConstraintAnchor anchor = widget.getAnchor(this.mAnchor.getType());
            this.mAnchor = anchor;
            if (anchor != null) {
                this.mTarget = anchor.getTarget();
                this.mMargin = this.mAnchor.getMargin();
                this.mStrengh = this.mAnchor.getStrength();
                this.mCreator = this.mAnchor.getConnectionCreator();
                return;
            }
            this.mTarget = null;
            this.mMargin = 0;
            this.mStrengh = ConstraintAnchor.Strength.STRONG;
            this.mCreator = 0;
        }

        public void applyTo(ConstraintWidget widget) {
            ConstraintAnchor anchor = widget.getAnchor(this.mAnchor.getType());
            anchor.connect(this.mTarget, this.mMargin, this.mStrengh, this.mCreator);
        }
    }

    public Snapshot(ConstraintWidget widget) {
        this.mX = widget.getX();
        this.mY = widget.getY();
        this.mWidth = widget.getWidth();
        this.mHeight = widget.getHeight();
        ArrayList<ConstraintAnchor> anchors = widget.getAnchors();
        int anchorsSize = anchors.size();
        for (int i = 0; i < anchorsSize; i++) {
            ConstraintAnchor a = anchors.get(i);
            this.mConnections.add(new Connection(a));
        }
    }

    public void updateFrom(ConstraintWidget widget) {
        this.mX = widget.getX();
        this.mY = widget.getY();
        this.mWidth = widget.getWidth();
        this.mHeight = widget.getHeight();
        int connections = this.mConnections.size();
        for (int i = 0; i < connections; i++) {
            Connection connection = this.mConnections.get(i);
            connection.updateFrom(widget);
        }
    }

    public void applyTo(ConstraintWidget widget) {
        widget.setX(this.mX);
        widget.setY(this.mY);
        widget.setWidth(this.mWidth);
        widget.setHeight(this.mHeight);
        int mConnectionsSize = this.mConnections.size();
        for (int i = 0; i < mConnectionsSize; i++) {
            Connection connection = this.mConnections.get(i);
            connection.applyTo(widget);
        }
    }
}
