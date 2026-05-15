package postanogov.dev.mynotesnew.service;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * Превращает твой существующий ключ из базы в формат, пригодный для AES.
     */
    private SecretKeySpec getSecretKeySpec(String existingKeyFromDb) {
        // Берем байты существующего ключа
        byte[] keyBytes = existingKeyFromDb.getBytes(StandardCharsets.UTF_8);
        // AES-128 требует ровно 16 байт. Обрезаем или дополняем ключ из базы.
        byte[] finalKey = Arrays.copyOf(keyBytes, 16);
        return new SecretKeySpec(finalKey, "AES");
    }

    public String encrypt(String plainText, String existingKeyFromDb) {
        try {
            if (plainText == null || plainText.isEmpty()) return plainText;

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(existingKeyFromDb));

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            return plainText;
        }
    }

    public String decrypt(String cipherText, String existingKeyFromDb) {
        try {
            if (cipherText == null || cipherText.isEmpty()) return cipherText;

            // Если это старая заметка от CryptoJS (начинается с Salted__)
            if (cipherText.startsWith("U2FsdGVkX1")) {
                // ВАЖНО: CryptoJS использовал твой key как пароль для генерации своего внутреннего ключа.
                // Поэтому старый метод дешифровки все еще нужен для обратной совместимости.
                return decryptLegacyCryptoJS(cipherText, existingKeyFromDb);
            }

            // Новая логика (чистый AES):
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(existingKeyFromDb));

            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cipherText;
        }
    }

    // Тот же метод для совместимости со старыми записями
    private String decryptLegacyCryptoJS(String cipherText, String keyAsPassword) throws Exception {
        byte[] ctBytes = Base64.getDecoder().decode(cipherText);
        byte[] salt = Arrays.copyOfRange(ctBytes, 8, 16);
        byte[] ciphertext = Arrays.copyOfRange(ctBytes, 16, ctBytes.length);

        java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
        byte[] keyIv = new byte[48];
        byte[] lastHash = new byte[0];
        for (int i = 0; i < 3; i++) {
            md5.reset();
            md5.update(lastHash);
            md5.update(keyAsPassword.getBytes(StandardCharsets.UTF_8));
            md5.update(salt);
            lastHash = md5.digest();
            System.arraycopy(lastHash, 0, keyIv, i * 16, 16);
        }

        SecretKeySpec key = new SecretKeySpec(Arrays.copyOfRange(keyIv, 0, 32), "AES");
        javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec(Arrays.copyOfRange(keyIv, 32, 48));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
}