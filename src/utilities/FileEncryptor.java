package utilities;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;

public class FileEncryptor {

    private static final String algo = "AES/CBC/PKCS5Padding"; // AES with CBC mode and PKCS5 padding
    private static final int saltLength = 16; // Salt length in bytes
    private static final int ITERATIONS = 100000; // Number of iterations for PBKDF2
    private static final int keyLength = 128; // AES-128 key length
    private static final int ivLength = 16; // IV length in bytes for AES

    // Method to generate AES key using a user-input password and a random generated salt
    public static SecretKey KeyGenFromPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey secretKey = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    // Method to generate random salt values
    public static byte[] createSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[saltLength];
        random.nextBytes(salt);
        return salt;
    }

    // Method to generate random IV
    public static byte[] createIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[ivLength];
        random.nextBytes(iv);
        return iv;
    }

    // Encrypt the file and return the IV used for encryption
    public static byte[] encryptFile(InputStream inputFileStream, OutputStream outputFileStream, SecretKey encryptionKey) throws Exception {
        // Generate a random IV for encryption
        byte[] iv = createIv();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Create cipher for encryption
        Cipher encryptionCipher = Cipher.getInstance(algo);
        encryptionCipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec);

        // Wrap output stream for encryption
        CipherOutputStream encryptedOutputStream = new CipherOutputStream(outputFileStream, encryptionCipher);

        // Buffer for file data
        byte[] buffer = new byte[1024];
        int bytesRead;

        // Read and encrypt the file
        while ((bytesRead = inputFileStream.read(buffer)) != -1) {
            encryptedOutputStream.write(buffer, 0, bytesRead);
        }
        encryptedOutputStream.close();

        // Return the IV
        return iv;
    }

    // Decrypt the file using the provided IV
    public static void decryptFile(InputStream inputFileStream, OutputStream outputFileStream, SecretKey decryptionKey, byte[] iv) throws Exception {
        // Use the provided IV for decryption
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Create cipher for decryption
        Cipher decryptionCipher = Cipher.getInstance(algo);
        decryptionCipher.init(Cipher.DECRYPT_MODE, decryptionKey, ivSpec);

        // Wrap input stream for decryption
        CipherInputStream decryptedInputStream = new CipherInputStream(inputFileStream, decryptionCipher);

        // Buffer for file data
        byte[] buffer = new byte[1024];
        int bytesRead;

        // Read and decrypt the file
        while ((bytesRead = decryptedInputStream.read(buffer)) != -1) {
            outputFileStream.write(buffer, 0, bytesRead);
        }
        decryptedInputStream.close();
    }
}
