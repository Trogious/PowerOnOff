package net.swmud.trog.core;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Settings {
    private static final String SETTINGS_FILE_NAME = "pooffsettings";
    private static final int SETTINGS_READ_SIZE = 1024;
    private static Settings instance;

    private SettingsJson settings = new SettingsJson();

    public Settings() {
    }

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

    public static Settings set(String mac, String host, int port, String keystorePassword, boolean acceptUntrustedServerCert, String preferredCertificateAlias, int time) {
        Settings instance1 = getInstance();
        instance1.settings.mac = mac;
        instance1.settings.host = host;
        instance1.settings.port = port;
        instance1.settings.keystorePassword = keystorePassword;
        instance1.settings.acceptUntrustedServerCert = acceptUntrustedServerCert;
        instance1.settings.preferredClientCertAlias = preferredCertificateAlias;
        instance1.settings.time = time;
        return instance1;
    }

    public void load(Context context) {
        FileInputStream inputStream = null;
        try {
            inputStream = context.openFileInput(SETTINGS_FILE_NAME);
            byte buf[] = new byte[SETTINGS_READ_SIZE];
            int bytesRead = inputStream.read(buf);
            if (bytesRead > 0) {
                String jsonStr = new String(buf, 0, bytesRead, Constants.ENCODING);
                try {
                    settings = new Gson().fromJson(jsonStr, SettingsJson.class);
                    settings.keystorePassword = Crypto.decrypt(getAndroidId(context), settings.keystorePassword);
                    Log.d("LOADED", bytesRead + "    " + jsonStr);
                } catch (JsonSyntaxException e) {
                    Log.d("SETT", e.getMessage());
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
        FileOutputStream outputStream = null;
        String unencryptedPassword = new String(settings.keystorePassword);
        try {
            outputStream = context.openFileOutput(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
            settings.keystorePassword = Crypto.encrypt(getAndroidId(context), settings.keystorePassword);
            String jsonStr = new Gson().toJson(settings);
            outputStream.write(jsonStr.getBytes(Constants.ENCODING));
            outputStream.flush();
            Log.e("SAVE", jsonStr + " flushed");
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
            settings.keystorePassword = unencryptedPassword;
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public String getMac() {
        return settings.mac;
    }

    public String getHost() {
        return settings.host;
    }

    public int getPort() {
        return settings.port;
    }

    public String getKeystorePassword() {
        return settings.keystorePassword;
    }

    public boolean acceptUntrustedServerCert() {
        return settings.acceptUntrustedServerCert;
    }

    public String getPreferredCertificateAlias() {
        return settings.preferredClientCertAlias;
    }

    public int getTime() {
        return settings.time;
    }

    private class SettingsJson implements Serializable {
        @SerializedName("keystore_password")
        private String keystorePassword;
        @SerializedName("mac")
        private String mac;
        @SerializedName("host")
        private String host;
        @SerializedName("port")
        private int port;
        @SerializedName("accept_untrusted_cert")
        private boolean acceptUntrustedServerCert;
        @SerializedName("preferred_alias")
        private String preferredClientCertAlias;
        @SerializedName("time")
        private int time;
    }
}
