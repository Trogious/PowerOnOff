package net.swmud.trog.net;

import android.util.Log;

import net.swmud.trog.core.Global;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

public class ExtendedKeyManager implements X509KeyManager {
    protected List<X509KeyManager> keyManagers = new LinkedList<>();
    protected String preferredCertAlias;

    protected ExtendedKeyManager(final String preferredCertAlias) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        this.preferredCertAlias = preferredCertAlias;
        final List<KeyManagerFactory> factories = new LinkedList<>();

        // The default KeyManager with default keystore
        final KeyManagerFactory original = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        original.init(null, "".toCharArray());

        final KeyManagerFactory additionalCerts = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        additionalCerts.init(Global.keyStores.getClientKeyStore(), Global.keyStores.getPrivateKeyPassword());

        factories.add(additionalCerts); //add custom certs first so that these override the system ones
        factories.add(original);

        for (KeyManagerFactory trustManagerFactory : factories) {
            for (KeyManager keyManager : trustManagerFactory.getKeyManagers()) {
                if (keyManager instanceof X509KeyManager) {
                    keyManagers.add((X509KeyManager) keyManager);
                }
            }
        }

        if (keyManagers.size() < 1)
            throw new KeyStoreException("No X509KeyManagers available.");
    }

    protected boolean aliasMatches(String hostName, String alias) {
        if (null != preferredCertAlias && preferredCertAlias.length() > 0) {
            return preferredCertAlias.equals(alias);
        }
        return hostName.equalsIgnoreCase(alias);
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        Log.d("KM", "chooseClientAlias");
        SocketAddress socketAddress = socket.getRemoteSocketAddress();
        String hostName = ((InetSocketAddress) socketAddress).getHostName();
        for (X509KeyManager km : keyManagers) {
            for (String keyType : keyTypes) {
                Log.d("KM", "keyType: " + keyType);
                String[] aliases = km.getClientAliases(keyType, issuers);
                if (null != aliases) {
                    for (String alias : aliases) {
                        Log.d("KM", "alias: " + alias);
                        if (aliasMatches(hostName, alias)) {
                            Log.d("KM", "alias chosen: " + alias);
                            return alias;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        Log.d("KM", "chooseServerAlias");
        return null;
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        Log.d("KM", "getCertificateChain");
        for (X509KeyManager km : keyManagers) {
            X509Certificate[] chain = km.getCertificateChain(alias);
            if (null != chain && chain.length > 0) {
                return chain;
            }
        }

        return new X509Certificate[0];
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        Log.d("KM", "getClientAliases");
        List<String> aliases = new LinkedList<>();
        for (X509KeyManager km : keyManagers) {
            String[] clientAliases = km.getClientAliases(keyType, issuers);
            if (null != clientAliases) {
                Log.d("KM", "client aliases found");
                aliases.addAll(Arrays.asList(clientAliases));
            }
        }

        return aliases.toArray(new String[aliases.size()]);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        Log.d("KM", "getServerAliases");
        return new String[0];
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        Log.d("KM", "getPrivateKey for: " + alias);
        PrivateKey privateKey = null;
        for (X509KeyManager km : keyManagers) {
            privateKey = km.getPrivateKey(alias);
            if (null != privateKey) {
                Log.d("KM", "key found");
                break;
            }
        }

        return privateKey;
    }
}
