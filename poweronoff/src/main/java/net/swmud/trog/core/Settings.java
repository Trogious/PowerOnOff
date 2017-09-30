package net.swmud.trog.core;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Settings {
    private static final String SETTINGS_FILE_NAME = "pooffsettings";
    private static final int SETTINGS_READ_SIZE = 1024;
    private static final String SETTING_SEPARATOR = "\n";
    private static final String PREFERRED_CERT_ALIAS_PLACEHOLDER = " ";
    private static Settings instance;

    private String host = "";
    private int port = 0;
    private String password = "";
    private boolean loginWithCertificate = false;

    private  String preferredCertificateAlias = "";

    public Settings() {}

    public static String getAndroidId(Context context) {
        return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    }

    public static Settings loadSettings(Context context) {
        Settings settings = getInstance();
        settings.load(context);
        return settings;
    }

    public static Settings getInstance() {
        if (null == instance) {
            instance = new Settings();
        }
        return instance;
    }

    public static Settings set(String host, int port, String password, boolean loginWithCertificate, String preferredCertificateAlias) {
        Settings settings = getInstance();
        settings.host = host;
        settings.port = port;
        settings.password = password;
        settings.loginWithCertificate = loginWithCertificate;
        settings.preferredCertificateAlias = preferredCertificateAlias;
        return settings;
    }

    public void load(Context context) {
        FileInputStream inputStream = null;
        try {
            inputStream = context.openFileInput(SETTINGS_FILE_NAME);
            byte buf[] = new byte[SETTINGS_READ_SIZE];
            int bytesRead = inputStream.read(buf);
            if (bytesRead > 0) {
                String settingsStr = new String(buf, 0, bytesRead, Constants.ENCODING);
                String setStr[] = settingsStr.split(SETTING_SEPARATOR);
                Log.d("LOAD", settingsStr + " | " + setStr.length);
                if (setStr != null && setStr.length > 4) {
                    host = setStr[0];
                    port = Integer.parseInt(setStr[1]);
                    password = Crypto.decrypt(getAndroidId(context), setStr[2]);
                    loginWithCertificate = (1 == Integer.parseInt(setStr[3]));
                    preferredCertificateAlias = setStr[4];
                    if (PREFERRED_CERT_ALIAS_PLACEHOLDER.equals(preferredCertificateAlias)) {
                        preferredCertificateAlias = "";
                    }
                    Log.d("LOADED", "");
                }
            }
        } catch (FileNotFoundException e) {
            Log.d("SETT", e.getMessage());
        } catch (IOException e) {
            Log.d("SETT", e.getMessage());
        } catch (NumberFormatException e) {
            Log.d("SETT", e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.d("SETT", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.d("SETT", e.getMessage());
        } catch (InvalidKeyException e) {
            Log.d("SETT", e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.d("SETT", e.getMessage());
        } catch (BadPaddingException e) {
            Log.d("SETT", e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void save(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(host);
        sb.append(SETTING_SEPARATOR);
        sb.append(port);
        sb.append(SETTING_SEPARATOR);
        FileOutputStream outputStream = null;
        try {
            outputStream = context.openFileOutput(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
            sb.append(Crypto.encrypt(getAndroidId(context), password));
//            sb.append(SETTING_SEPARATOR); //TODO: fix Crypto.encrypt to return password without trailing new line
            sb.append(loginWithCertificate ? "1" : "0");
            sb.append(SETTING_SEPARATOR);
            sb.append(preferredCertificateAlias.length() < 1 ? PREFERRED_CERT_ALIAS_PLACEHOLDER : preferredCertificateAlias);
            outputStream.write(sb.toString().getBytes(Constants.ENCODING));
            outputStream.flush();
            Log.e("SAVE", sb.toString() + " flushed");
        } catch (FileNotFoundException e) {
            Log.d("SETT", e.getMessage());
        } catch (IOException e) {
            Log.d("SETT", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.d("SETT", e.getMessage());
        } catch (InvalidKeyException e) {
            Log.d("SETT", e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.d("SETT", e.getMessage());
        } catch (BadPaddingException e) {
            Log.d("SETT", e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.d("SETT", e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLoginWithCertificate() { return loginWithCertificate; }

    public String getPreferredCertificateAlias() { return preferredCertificateAlias; }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
