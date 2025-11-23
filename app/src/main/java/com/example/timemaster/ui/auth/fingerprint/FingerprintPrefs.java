package com.example.timemaster.ui.auth.fingerprint;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.google.firebase.auth.FirebaseUser;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import android.util.Base64;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Lưu cấu hình đăng nhập bằng vân tay cho 1 tài khoản Firebase.
 *
 * - Hỗ trợ 2 loại account:
 *   + provider "password": lưu email + password (đã mã hoá).
 *   + provider "google"  : lưu uid + email (dùng silentSignIn để login lại).
 *
 * - Mã hoá dùng Android Keystore (AES/CBC/PKCS7).
 */

public class FingerprintPrefs {
    private static final String PREFS_NAME = "fingerprint_prefs";

    private static final String KEY_ENABLED = "fingerprint_enabled";
    private static final String KEY_UID = "fingerprint_uid";
    private static final String KEY_PROVIDER = "fingerprint_provider"; // password hoặc google
    private static final String KEY_EMAIL_ENC = "fingerprint_email_enc";

    private static final String KEY_PASSWORD_ENC = "fingerprint_password_enc";

    private final SharedPreferences prefs;

    private final CryptoHelper cryptoHelper;

    public FingerprintPrefs(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE);
        this.cryptoHelper = new CryptoHelper();
    }

    // LƯU CẤU HÌNH FINGERPRINT CHO ACCOUNT EMAIL/PASSWORD
    public void savePasswordUser(FirebaseUser user, String emailPlain, String passwordPlain) {
        if (user == null || emailPlain == null || passwordPlain == null) {
            return;
        }

        try {
            String emailEnc = cryptoHelper.encrypt(emailPlain);
            String passEnc = cryptoHelper.encrypt(passwordPlain);

            prefs.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putString(KEY_UID, user.getUid())
                    .putString(KEY_PROVIDER, "password")
                    .putString(KEY_EMAIL_ENC, emailEnc)
                    .putString(KEY_PASSWORD_ENC, passEnc)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // LƯU CẤU HÌNH FINGERPRINT CHO ACCOUNT GOOGLE
    public void saveGoogleUser(FirebaseUser user) {
        if (user == null) {
            return;
        }

        prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_UID, user.getUid())
                .putString(KEY_PROVIDER, "google")
                .putString(KEY_EMAIL_ENC, user.getEmail())
                .remove(KEY_PASSWORD_ENC)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public String getProvider() {
        return prefs.getString(KEY_PROVIDER, null);
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getDecryptedEmail() {
        String enc = prefs.getString(KEY_EMAIL_ENC, null);
        if (enc == null) {
            return null;
        }

        // Với provider=google: KEY_EMAIL_ENC là email dạng plain, không mã hoá.
        String provider = getProvider();
        if ("google".equals(provider)) {
            return enc;
        }

        try {
            return cryptoHelper.decrypt(enc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getDecryptedPassword() {
        String enc = prefs.getString(KEY_PASSWORD_ENC, null);
        if (enc == null) {
            return null;
        }

        try {
            return cryptoHelper.decrypt(enc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ======================= CRYPTO HELPER NỘI BỘ=======================
    /**
     * Mã hoá/giải mã string bằng Android Keystore.
     * Dùng 1 key duy nhất cho app này để bảo vệ email/password.
     * Muốn dùng key phải qua xác thực (vân tay/PIN) tuỳ theo cấu hình BiometricPrompt phía ngoài.
     */

    private static class CryptoHelper {
        private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
        private static final String KEY_ALIAS = "tm_fingerprint_login_key";
        private static final String TRANSFORMATION = "AES/CBC/PKCS7Padding";

        CryptoHelper() {
            try {
                generateKeyIfNeeded();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void generateKeyIfNeeded() throws Exception {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                return;
            }

            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(false)
                    .build();

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            keyGenerator.init(keySpec);
            keyGenerator.generateKey();
        }

        private SecretKey getSecretKey() throws Exception {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        // MÃ HÓA PLAINTEXT, TRẢ VỀ CHUỖI BASE64
        String encrypt(String plainText) throws Exception {
            if (plainText == null) {
                return null;
            }

            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            byte[] enc = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String ivBase = Base64.encodeToString(iv, Base64.NO_WRAP);
            String encBase = Base64.encodeToString(enc, Base64.NO_WRAP);

            return ivBase + ":" + encBase;
        }

        // GIẢI MÃ CHUỖI BASE64 VỀ PLAINTEXT
        String decrypt(String data) throws Exception {
            if (data == null) {
                return null;
            }

            String[] parts = data.split(":");
            if (parts.length != 2) {
                return null;
            }

            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] enc = Base64.decode(parts[1], Base64.NO_WRAP);

            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] decoded = cipher.doFinal(enc);
            return new String(decoded, StandardCharsets.UTF_8);
        }
    }


}
