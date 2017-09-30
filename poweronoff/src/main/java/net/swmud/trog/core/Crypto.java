package net.swmud.trog.core;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    private static final String CIPHER_ALGORITHM = "AES";
    private static final String KEY_ALGORITHM = CIPHER_ALGORITHM;

    public static String encrypt(String key, String clearText) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKey ks = new SecretKeySpec(key.getBytes(), KEY_ALGORITHM);
        Log.d("ENC", "bits:" + ks.getEncoded().length);
        Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, ks);
        byte[] encryptedText = c.doFinal(clearText.getBytes(Constants.ENCODING));

        return Base64.encodeToString(encryptedText, Base64.DEFAULT);
    }

    public static String decrypt(String key, String encryptedText) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        SecretKey ks = new SecretKeySpec(key.getBytes(), KEY_ALGORITHM);
        Log.d("DEC", "bits:" + ks.getEncoded().length);
        Cipher c = Cipher.getInstance(CIPHER_ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, ks);
        byte[] clearText = c.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));

        return new String(clearText, Constants.ENCODING);
    }
}