package net.swmud.trog.core;

import android.os.Environment;

import java.io.File;

public class Global {
    private static String keyStoresLocation;
    public static KeyStores keyStores;

    public static String getKeyStoresLocation() {
        if (null == keyStoresLocation) {
            keyStoresLocation = new File(Environment.getExternalStorageDirectory().getPath(), Constants.DATA_DIRECTORY).getPath();
        }

        return keyStoresLocation;
    }
}
