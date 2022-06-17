package androidx.core.provider;

import android.util.Base64;
import androidx.core.util.Preconditions;
import java.util.List;
/* loaded from: classes.dex */
public final class FontRequest {
    private final List<List<byte[]>> mCertificates;
    private final int mCertificatesArray;
    private final String mIdentifier;
    private final String mProviderAuthority;
    private final String mProviderPackage;
    private final String mQuery;

    public FontRequest(String providerAuthority, String providerPackage, String query, List<List<byte[]>> certificates) {
        String str = (String) Preconditions.checkNotNull(providerAuthority);
        this.mProviderAuthority = str;
        String str2 = (String) Preconditions.checkNotNull(providerPackage);
        this.mProviderPackage = str2;
        String str3 = (String) Preconditions.checkNotNull(query);
        this.mQuery = str3;
        this.mCertificates = (List) Preconditions.checkNotNull(certificates);
        this.mCertificatesArray = 0;
        this.mIdentifier = str + "-" + str2 + "-" + str3;
    }

    public FontRequest(String providerAuthority, String providerPackage, String query, int certificates) {
        String str = (String) Preconditions.checkNotNull(providerAuthority);
        this.mProviderAuthority = str;
        String str2 = (String) Preconditions.checkNotNull(providerPackage);
        this.mProviderPackage = str2;
        String str3 = (String) Preconditions.checkNotNull(query);
        this.mQuery = str3;
        this.mCertificates = null;
        Preconditions.checkArgument(certificates != 0);
        this.mCertificatesArray = certificates;
        this.mIdentifier = str + "-" + str2 + "-" + str3;
    }

    public String getProviderAuthority() {
        return this.mProviderAuthority;
    }

    public String getProviderPackage() {
        return this.mProviderPackage;
    }

    public String getQuery() {
        return this.mQuery;
    }

    public List<List<byte[]>> getCertificates() {
        return this.mCertificates;
    }

    public int getCertificatesArrayResId() {
        return this.mCertificatesArray;
    }

    public String getIdentifier() {
        return this.mIdentifier;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FontRequest {mProviderAuthority: " + this.mProviderAuthority + ", mProviderPackage: " + this.mProviderPackage + ", mQuery: " + this.mQuery + ", mCertificates:");
        for (int i = 0; i < this.mCertificates.size(); i++) {
            builder.append(" [");
            List<byte[]> set = this.mCertificates.get(i);
            for (int j = 0; j < set.size(); j++) {
                builder.append(" \"");
                byte[] array = set.get(j);
                builder.append(Base64.encodeToString(array, 0));
                builder.append("\"");
            }
            builder.append(" ]");
        }
        builder.append("}");
        builder.append("mCertificatesArray: " + this.mCertificatesArray);
        return builder.toString();
    }
}
