package androidx.collection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
/* loaded from: classes.dex */
public class LruCache<K, V> {
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private final LinkedHashMap<K, V> map;
    private int maxSize;
    private int missCount;
    private int putCount;
    private int size;

    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }

    public final V get(K key) {
        Throwable th;
        V mapValue;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            try {
                try {
                    V mapValue2 = this.map.get(key);
                    if (mapValue2 != null) {
                        this.hitCount++;
                        return mapValue2;
                    }
                    this.missCount++;
                    V createdValue = create(key);
                    if (createdValue == null) {
                        return null;
                    }
                    synchronized (this) {
                        this.createCount++;
                        mapValue = this.map.put(key, createdValue);
                        if (mapValue != null) {
                            this.map.put(key, mapValue);
                        } else {
                            this.size += safeSizeOf(key, createdValue);
                        }
                    }
                    if (mapValue != null) {
                        entryRemoved(false, key, createdValue, mapValue);
                        return mapValue;
                    }
                    trimToSize(this.maxSize);
                    return createdValue;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }
        synchronized (this) {
            try {
                try {
                    this.putCount++;
                    this.size += safeSizeOf(key, value);
                    V previous = this.map.put(key, value);
                    if (previous != null) {
                        this.size -= safeSizeOf(key, previous);
                    }
                    if (previous != null) {
                        entryRemoved(false, key, previous, value);
                    }
                    trimToSize(this.maxSize);
                    return previous;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:24:0x0076, code lost:
        throw new java.lang.IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
     */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:28:0x007a -> B:26:0x0078). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void trimToSize(int r7) {
        /*
            r6 = this;
            r0 = 0
            r1 = r0
            r2 = r1
        L3:
            monitor-enter(r6)
            int r3 = r6.size     // Catch: java.lang.Throwable -> L77
            if (r3 < 0) goto L56
            java.util.LinkedHashMap<K, V> r3 = r6.map     // Catch: java.lang.Throwable -> L77
            boolean r3 = r3.isEmpty()     // Catch: java.lang.Throwable -> L77
            if (r3 == 0) goto L14
            int r3 = r6.size     // Catch: java.lang.Throwable -> L77
            if (r3 != 0) goto L56
        L14:
            int r3 = r6.size     // Catch: java.lang.Throwable -> L77
            if (r3 <= r7) goto L54
            java.util.LinkedHashMap<K, V> r3 = r6.map     // Catch: java.lang.Throwable -> L77
            boolean r3 = r3.isEmpty()     // Catch: java.lang.Throwable -> L77
            if (r3 == 0) goto L21
            goto L54
        L21:
            java.util.LinkedHashMap<K, V> r3 = r6.map     // Catch: java.lang.Throwable -> L77
            java.util.Set r3 = r3.entrySet()     // Catch: java.lang.Throwable -> L77
            java.util.Iterator r3 = r3.iterator()     // Catch: java.lang.Throwable -> L77
            java.lang.Object r3 = r3.next()     // Catch: java.lang.Throwable -> L77
            java.util.Map$Entry r3 = (java.util.Map.Entry) r3     // Catch: java.lang.Throwable -> L77
            java.lang.Object r1 = r3.getKey()     // Catch: java.lang.Throwable -> L77
            java.lang.Object r2 = r3.getValue()     // Catch: java.lang.Throwable -> L52
            java.util.LinkedHashMap<K, V> r4 = r6.map     // Catch: java.lang.Throwable -> L7a
            r4.remove(r1)     // Catch: java.lang.Throwable -> L7a
            int r4 = r6.size     // Catch: java.lang.Throwable -> L7a
            int r5 = r6.safeSizeOf(r1, r2)     // Catch: java.lang.Throwable -> L7a
            int r4 = r4 - r5
            r6.size = r4     // Catch: java.lang.Throwable -> L7a
            int r4 = r6.evictionCount     // Catch: java.lang.Throwable -> L7a
            r5 = 1
            int r4 = r4 + r5
            r6.evictionCount = r4     // Catch: java.lang.Throwable -> L7a
            monitor-exit(r6)     // Catch: java.lang.Throwable -> L7a
            r6.entryRemoved(r5, r1, r2, r0)
            goto L3
        L52:
            r0 = move-exception
            goto L78
        L54:
            monitor-exit(r6)     // Catch: java.lang.Throwable -> L77
            return
        L56:
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException     // Catch: java.lang.Throwable -> L77
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L77
            r3.<init>()     // Catch: java.lang.Throwable -> L77
            java.lang.Class r4 = r6.getClass()     // Catch: java.lang.Throwable -> L77
            java.lang.String r4 = r4.getName()     // Catch: java.lang.Throwable -> L77
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L77
            java.lang.String r4 = ".sizeOf() is reporting inconsistent results!"
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L77
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L77
            r0.<init>(r3)     // Catch: java.lang.Throwable -> L77
            throw r0     // Catch: java.lang.Throwable -> L77
        L77:
            r0 = move-exception
        L78:
            monitor-exit(r6)     // Catch: java.lang.Throwable -> L7a
            throw r0
        L7a:
            r0 = move-exception
            goto L78
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.collection.LruCache.trimToSize(int):void");
    }

    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            try {
                try {
                    V previous = this.map.remove(key);
                    if (previous != null) {
                        this.size -= safeSizeOf(key, previous);
                    }
                    if (previous != null) {
                        entryRemoved(false, key, previous, null);
                    }
                    return previous;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    protected void entryRemoved(boolean evicted, K key, V oldValue, V newValue) {
    }

    protected V create(K key) {
        return null;
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    protected int sizeOf(K key, V value) {
        return 1;
    }

    public final void evictAll() {
        trimToSize(-1);
    }

    public final synchronized int size() {
        return this.size;
    }

    public final synchronized int maxSize() {
        return this.maxSize;
    }

    public final synchronized int hitCount() {
        return this.hitCount;
    }

    public final synchronized int missCount() {
        return this.missCount;
    }

    public final synchronized int createCount() {
        return this.createCount;
    }

    public final synchronized int putCount() {
        return this.putCount;
    }

    public final synchronized int evictionCount() {
        return this.evictionCount;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized String toString() {
        int hitPercent;
        int i = this.hitCount;
        int accesses = this.missCount + i;
        hitPercent = accesses != 0 ? (i * 100) / accesses : 0;
        return String.format(Locale.US, "LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]", Integer.valueOf(this.maxSize), Integer.valueOf(this.hitCount), Integer.valueOf(this.missCount), Integer.valueOf(hitPercent));
    }
}
