package androidx.lifecycle;

import androidx.lifecycle.Lifecycle;
/* loaded from: classes.dex */
public class CompositeGeneratedAdaptersObserver implements GenericLifecycleObserver {
    private final GeneratedAdapter[] mGeneratedAdapters;

    public CompositeGeneratedAdaptersObserver(GeneratedAdapter[] generatedAdapters) {
        this.mGeneratedAdapters = generatedAdapters;
    }

    @Override // androidx.lifecycle.GenericLifecycleObserver
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        GeneratedAdapter[] generatedAdapterArr;
        GeneratedAdapter[] generatedAdapterArr2;
        MethodCallsLogger logger = new MethodCallsLogger();
        for (GeneratedAdapter mGenerated : this.mGeneratedAdapters) {
            mGenerated.callMethods(source, event, false, logger);
        }
        for (GeneratedAdapter mGenerated2 : this.mGeneratedAdapters) {
            mGenerated2.callMethods(source, event, true, logger);
        }
    }
}
