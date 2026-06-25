package com.danil.app.common;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String ALGO = "AES";
    private static final byte[] KEY = "1234567890123456".getBytes(); // рівно 16 символів

    public static byte[] encrypt(byte[] data) throws Exception {
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALGO));
        return c.doFinal(data);
    }

    public static byte[] decrypt(byte[] data) throws Exception {
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALGO));
        return c.doFinal(data);
    }
}