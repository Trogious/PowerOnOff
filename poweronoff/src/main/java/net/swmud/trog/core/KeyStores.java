package net.swmud.trog.core;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public class KeyStores {
    private static final String KEY_STORE_CLIENT = "pooffstoreclient.bks";
    private static final String KEY_STORE_TRUST = "pooffstoretrust.bks";
    private final String dataDirectory;
    private final PasswordProvider privateKeyPasswordProvider;

    public KeyStores(final String dataDirectory, final PasswordProvider passwordProvider) {
        this.dataDirectory = dataDirectory;
        this.privateKeyPasswordProvider = passwordProvider;
    }

    @Nullable
    public KeyStore getClientKeyStore() {
        return loadKeyStore(KEY_STORE_CLIENT);
    }

    @Nullable
    public KeyStore getTrustKeyStore() {
        return loadKeyStore(KEY_STORE_TRUST);
    }

    public char[] getPrivateKeyPassword() {
        return privateKeyPasswordProvider.getPassword().toCharArray();
    }

    @Nullable
    private KeyStore loadKeyStore(String keyStoreFileName) {
        File path = new File(dataDirectory, keyStoreFileName);
        try {
            FileInputStream ksInputStream = new FileInputStream(path);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(ksInputStream, getPrivateKeyPassword());
            Log.d("KSs", "loaded key store: " + path.getAbsolutePath());
            Enumeration<String> as = ks.aliases();
            while (as.hasMoreElements()) {
                String a = as.nextElement();
                Log.d("KSs", " " + a);
            }
            return ks;
        } catch (KeyStoreException e) {
            Log.d("KSs", e.getMessage());
        } catch (CertificateException e) {
            Log.d("KSs", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.d("KSs", e.getMessage());
        } catch (FileNotFoundException e) {
            Log.d("KSs", e.getMessage());
        } catch (IOException e) {
            Log.d("KSs", e.getMessage());
        }

        return null;
    }
}
