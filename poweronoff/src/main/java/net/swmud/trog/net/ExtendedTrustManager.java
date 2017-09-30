package net.swmud.trog.net;

import android.util.Log;

import net.swmud.trog.core.Global;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ExtendedTrustManager implements X509TrustManager {
    protected X509Certificate acceptedIssuer;
    protected List<X509TrustManager> trustManagers = new LinkedList<>();

    protected ExtendedTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        final List<TrustManagerFactory> factories = new LinkedList<>();

        // The default TrustManager with default keystore
        final TrustManagerFactory original = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        original.init((KeyStore) null);

        final TrustManagerFactory additionalCerts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        additionalCerts.init(Global.keyStores.getTrustKeyStore());

//        factories.add(additionalCerts); //add custom certs first so that these override the system ones
        factories.add(original);

        for (TrustManagerFactory trustManagerFactory : factories) {
            for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    trustManagers.add((X509TrustManager) tm);
                }
            }
        }

        if (trustManagers.size() < 1)
            throw new KeyStoreException("No X509TrustManagers available.");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        Log.d("AUTH", "checkClientTrusted");
        throw new CertificateException("This trust manager validates only server certificates");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length < 1) {
            throw new CertificateException("x509 cert chain empty");
        }
        Log.d("AUTH", "_authType: " + authType);

        for (X509TrustManager tm : trustManagers) {
            try {
                tm.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException e) {
            }
        }

        Log.e("AUTH", "Server certificate not trusted: " + chain[0].getIssuerDN().getName());
//        throw new CertificateException("Server certificate not trusted.");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        Log.d("AUTH", "getAcceptedIssuers");
        for (X509TrustManager tm : trustManagers) {
            X509Certificate[] issuers = tm.getAcceptedIssuers();
            if (null != issuers && issuers.length > 0) {
                Log.d("AUTH", "returning issuers: " + issuers.length);
                return issuers;
            }

        }
        return new X509Certificate[0];
    }
}
