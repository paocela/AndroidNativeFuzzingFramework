package androidx.appcompat.view.menu;
/* loaded from: classes.dex */
class BaseWrapper<T> {
    final T mWrappedObject;

    public BaseWrapper(T object) {
        if (object == null) {
            throw new IllegalArgumentException("Wrapped Object can not be null.");
        }
        this.mWrappedObject = object;
    }

    public T getWrappedObject() {
        return this.mWrappedObject;
    }
}
