package androidx.constraintlayout.solver.widgets;

import androidx.constraintlayout.solver.LinearSystem;
/* loaded from: classes.dex */
public class Chain {
    private static final boolean DEBUG = false;

    Chain() {
    }

    public static void applyChainConstraints(ConstraintWidgetContainer constraintWidgetContainer, LinearSystem system, int orientation) {
        ChainHead[] chainsArray;
        int chainsSize;
        int offset;
        if (orientation == 0) {
            offset = 0;
            chainsSize = constraintWidgetContainer.mHorizontalChainsSize;
            chainsArray = constraintWidgetContainer.mHorizontalChainsArray;
        } else {
            offset = 2;
            chainsSize = constraintWidgetContainer.mVerticalChainsSize;
            chainsArray = constraintWidgetContainer.mVerticalChainsArray;
        }
        for (int i = 0; i < chainsSize; i++) {
            ChainHead first = chainsArray[i];
            first.define();
            if (constraintWidgetContainer.optimizeFor(4)) {
                if (!Optimizer.applyChainOptimized(constraintWidgetContainer, system, orientation, offset, first)) {
                    applyChainConstraints(constraintWidgetContainer, system, orientation, offset, first);
                }
            } else {
                applyChainConstraints(constraintWidgetContainer, system, orientation, offset, first);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:292:0x0646  */
    /* JADX WARN: Removed duplicated region for block: B:293:0x064b  */
    /* JADX WARN: Removed duplicated region for block: B:296:0x0652  */
    /* JADX WARN: Removed duplicated region for block: B:297:0x0657  */
    /* JADX WARN: Removed duplicated region for block: B:299:0x065a  */
    /* JADX WARN: Removed duplicated region for block: B:304:0x066e  */
    /* JADX WARN: Removed duplicated region for block: B:306:0x0672  */
    /* JADX WARN: Removed duplicated region for block: B:307:0x067e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static void applyChainConstraints(androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer r44, androidx.constraintlayout.solver.LinearSystem r45, int r46, int r47, androidx.constraintlayout.solver.widgets.ChainHead r48) {
        /*
            Method dump skipped, instructions count: 1716
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.constraintlayout.solver.widgets.Chain.applyChainConstraints(androidx.constraintlayout.solver.widgets.ConstraintWidgetContainer, androidx.constraintlayout.solver.LinearSystem, int, int, androidx.constraintlayout.solver.widgets.ChainHead):void");
    }
}
