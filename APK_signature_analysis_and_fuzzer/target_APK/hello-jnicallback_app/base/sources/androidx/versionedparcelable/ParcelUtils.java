package androidx.versionedparcelable;

import android.os.Parcelable;
import java.io.InputStream;
import java.io.OutputStream;
/* loaded from: classes.dex */
public class ParcelUtils {
    private ParcelUtils() {
    }

    public static Parcelable toParcelable(VersionedParcelable obj) {
        return new ParcelImpl(obj);
    }

    public static <T extends VersionedParcelable> T fromParcelable(Parcelable p) {
        if (!(p instanceof ParcelImpl)) {
            throw new IllegalArgumentException("Invalid parcel");
        }
        return (T) ((ParcelImpl) p).getVersionedParcel();
    }

    public static void toOutputStream(VersionedParcelable obj, OutputStream output) {
        VersionedParcelStream stream = new VersionedParcelStream(null, output);
        stream.writeVersionedParcelable(obj);
        stream.closeField();
    }

    public static <T extends VersionedParcelable> T fromInputStream(InputStream input) {
        VersionedParcelStream stream = new VersionedParcelStream(input, null);
        return (T) stream.readVersionedParcelable();
    }
}
