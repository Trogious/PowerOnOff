package net.swmud.trog.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

import net.swmud.trog.core.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class TcpClient implements Runnable {
    private static final String TAG = TcpClient.class.getSimpleName();
    private static final int READ_SIZE = 4096;
    private static final int CONNECT_TIMEOUT = 10000;
    private String host;
    private int port;
    private boolean useClientCertLogin;
    private static String filesDir;
    private Socket socket = new Socket();
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;
    private boolean running = false;
    private Listener<String> msgListener;
    private Listener<String> errListener;
    private ConnectedListener connListener;

    public TcpClient(final String host, final int port, final boolean useClientCertLogin, final String filesDir, final Listener<String> msgListener, final Listener<String> errListener, final ConnectedListener connListener) {
        this.host = host;
        this.port = port;
        this.useClientCertLogin = useClientCertLogin;
        TcpClient.filesDir = filesDir;
        this.msgListener = msgListener;
        this.errListener = errListener;
        this.connListener = connListener;
    }

    @Override
    public void run() {
        running = true;
        Log.d("Tcp", "TcpClient.run()");

        SocketAddress sockAddr = new InetSocketAddress(host, port);
        try {
            Log.d("Tcp", "try");
            socket.connect(sockAddr, CONNECT_TIMEOUT);
            Log.d("Tcp", "socket created");

            SSLSocket sock = createSslSocket(socket);
            sock.startHandshake();

            synchronized (socket) {
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(sock.getOutputStream())), true);
                mBufferIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                sock.setSoTimeout(0);
            }
        } catch (UnknownHostException e) {
            handleException(e);
            return;
        } catch (IOException e) {
            handleException(e);
            return;
        }
        connListener.onConnected();

        ResponseParser responseParser = new ResponseParser();
        while (running) {
            Log.d("Tcp", "while");
            char buf[] = new char[READ_SIZE];
            int bytesRead;
            try {
                bytesRead = mBufferIn.read(buf, 0, READ_SIZE);
                Log.d("bytesRead", "" + bytesRead);
                responseParser.parse(buf, bytesRead);
            } catch (SocketTimeoutException e) {
                //errListener.onMessage("socket timed out ok");
                continue;
            } catch (IOException e) {
                //errListener.onMessage(e.getMessage());
                break;
            }

            if (bytesRead < 1) {
                Log.d("Tcp", "connection closed by peer");
                //errListener.onMessage("connection closed by peer");
                break;
            } else {
                if (responseParser.isRequestComplete()) {
                    String msg = responseParser.getBody();
//                    Log.d("message", msg);
//                    Log.d("messageLen", "" + msg.length());
                    msgListener.onMessage(msg);
                    responseParser = new ResponseParser();
                }
            }
        }

        Log.d("Tcp", "finished");
        finish();
    }

    private void handleException(Exception e) {
        Log.d("E", e.getMessage());
        //errListener.onMessage(e.getMessage());
        finish();
    }

    private void closeSocket() {
        if (socket != null) {
            synchronized (socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void finish() {
        running = false;
        closeSocket();
        Log.d("E", "finished");
    }

    public boolean isRunning() {
        return running;
    }

    public void sendMessage(String msg) {
        if (mBufferOut != null) {
            synchronized (mBufferOut) {
                mBufferOut.print(msg);
                mBufferOut.flush();
            }
        }
    }

    public interface Listener<T> {
        void onMessage(T msg);
    }

    public interface ConnectedListener {
        void onConnected();
    }

    private static SSLContext getInitializedSslContext() {
        SSLContext sslContext = null;
        TrustManager[] trustManagers;
        KeyManager[] keyManagers;
        if (false) {
            trustManagers = getTrustAllTrustManagers();
            keyManagers = null;
        } else {
            try {
                Pair<TrustManager[], KeyManager[]> managers = getCustomManagers();
                trustManagers = managers.first;
                keyManagers = managers.second;
            } catch (IOException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | CertificateException e) {
                Log.d(TAG, Log.getStackTraceString(e));
                trustManagers = null;
                keyManagers = null;
            }
        }
        try {
            sslContext = SSLContext.getInstance(Constants.SECURE_PROTOCOL);
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "algo error", e);
        } catch (KeyManagementException e) {
            Log.e(TAG, "key mgmt error", e);
        }
        return sslContext;
    }

    @SuppressLint("TrustAllX509TrustManager")
    private static X509TrustManager[] getTrustAllTrustManagers() {
        return new X509TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                    }
                }
        };
    }

    private static Pair<TrustManager[], KeyManager[]> getCustomManagers() throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, CertificateException {
        final String password = "test12";

        KeyStore ks = KeyStore.getInstance(Constants.KEYSTORE_TYPE);
        File kf = new File(filesDir, Constants.KEYSTORE_FILE_NAME);
        ks.load(new FileInputStream(kf), password.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(Constants.X509_FACTORY_TYPE);
        kmf.init(ks, password.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(Constants.X509_FACTORY_TYPE);
        tmf.init(ks);

        return new Pair<>(tmf.getTrustManagers(), kmf.getKeyManagers());
    }

    private SSLSocket createSslSocket(Socket socket) throws IOException {
        SSLContext sslContext = getInitializedSslContext();
        SSLSocketFactory sf = sslContext.getSocketFactory();
        SSLSocket sock = (SSLSocket) sf.createSocket(socket, host, port, true);
        sock.setEnabledProtocols(new String[]{Constants.SECURE_PROTOCOL});

        return sock;
    }
}
