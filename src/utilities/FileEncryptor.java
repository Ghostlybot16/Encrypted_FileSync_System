package utilities;

import java.io.*; // Input/Output classes for file handling
import javax.crypto.Cipher; // Provides cryptographic encryption and decryption functions
import javax.crypto.SecretKey; // Secret key used in encryption and decryption
import javax.crypto.spec.SecretKeySpec; // Converts byte data into secret key
import javax.crypto.CipherOutputStream; // Class that encrypts data as it's written
import javax.crypto.CipherInputStream; // Class that decrypts data as it's read
import javax.crypto.SecretKeyFactory; // Used to generate a key from the password using PBKDF2
import javax.crypto.spec.PBEKeySpec; // Specification for password based encryption
import java.security.SecureRandom; // Generates random salt (random value) for key generation

// Salt = a random binary value

/**
 * Utility class responsible for:
 * - Generating AES encryptions keys from passwords.
 * - Creating secure random salts.
 * - Encrypting files using AES encryption.
 * - Decrypting encrypted files
 *
 * This class uses:
 * - AES for en/de(cryption)
 * - PBKDF2-with-HMAC-SHA256 for password-based key generation
 */
public class FileEncryptor {

    // Encryption algorithm used for file encryption and decryption
    // AES encryption algorithm
    private static final String algo = "AES";
    private static final int saltLength = 16; // Salt length in bytes for key generation

    /**
     * Number of PBKDF2 iterations used during key generation.
     * Higher values increase security but require more processing time.
     */
    private static final int ITERATIONS = 100000; // Number of iterations for PBKDF2
    private static final int keyLength = 128; // AES-128 key length in bits

    /**
     * Generates a secret AES encryption key from a password and salt.
     *
     * This method uses PBKDF2 with HMAC-SHA256 to derive
     * a secure cryptographic key from the user's password and
     * a randomly generated salt value.
     *
     * @param password user-provided password.
     * @param salt randomly generated salt value.
     * @return generated AES SecretKey
     * @throws Exception if key generation fails
     */
    public static SecretKey KeyGenFromPassword(
            String password,
            byte[] salt
    ) throws Exception {

        // Key specification/blueprint used to generate the cryptographic key
        // Uses the values from the user-input password, salt and ITERATIONS to generate the AES key
        PBEKeySpec keySpec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                keyLength
        );

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        return new SecretKeySpec(
                secretKey.getEncoded(),
                algo
        );
    }

    /**
     * Generates a secure random salt value.
     *
     * Salt values are used during password-based key generation.
     * to ensure stronger security and prevent rainbow table attacks.
     *
     * @return randomly generated salt byte array
     */
    public static byte[] createSalt() {

        SecureRandom randomNum = new SecureRandom();

        byte[] salt = new byte[saltLength];

        randomNum.nextBytes(salt);

        return salt; // Return the generated salt value
    }

    /**
     * Encrypts file data using AES encryption.
     *
     * The method reads bytes from the input stream,
     * encrypts them using the provided key,
     * and writes the encrypted data to the output stream.
     *
     * @param inputFileStream stream containing original file data.
     * @param outputFileStream stream where encrypted data will be written.
     * @param encryptionKey AES encryption key
     * @throws Exception if encryption fails
     */
    public static void encryptFile(
            InputStream inputFileStream,
            OutputStream outputFileStream,
            SecretKey encryptionKey
    ) throws Exception {

        // Create cipher using AES
        // A factory method that creates a Cipher object for the specified (AES) algorithm
        Cipher encryptionCipher = Cipher.getInstance(algo);

        // Initialize the cipher in encryption mode. It will use the encryptionKey value to perform the encryption
        encryptionCipher.init(
                Cipher.ENCRYPT_MODE,
                encryptionKey
        );

        // Wrapping the output stream for encryption
        CipherOutputStream encryptedOutputStream = new CipherOutputStream(outputFileStream, encryptionCipher);

        /* dataBuffer is a temporary storage array that can hold up to 1024 bytes = 1KB of data at a time while
         * reading and writing a file. This value can be changed. */
        byte[] dataBuffer = new byte[1024];

        // Variable to hold the number of bytes read
        int bytesRead;

        /*  inputFileStream.read(dataBuffer) reads up to 1024 bytes of data from the input stream into the buffer array
         *   If there's data available then the buffer is filled up to 1024 bytes
         *   If it reaches EOF then it returns -1, meaning there's no more data to read hence breaking the while loop */
        while ((bytesRead = inputFileStream.read(dataBuffer)) != -1){
            encryptedOutputStream.write(dataBuffer, 0, bytesRead); // Writes the contents of the buffer to encryptedOutputStream (The encrypted output stream)
        }

        encryptedOutputStream.close(); // Close the stream
    }

    /**
     * Decrypts encrypted file data using AES decryption.
     *
     * This method reads encrypted bytes from the input stream,
     * decrypts them using the provided key,
     * and writes the decrypted data to the output stream.
     *
     * @param inputFileStream stream containing encrypted file data.
     * @param outputFileStream stream where decrypted data will be written.
     * @param encryptionKey AES decryption key.
     * @throws Exception if decryption fails.
     */
    public static void decryptFile(
            InputStream inputFileStream,
            OutputStream outputFileStream,
            SecretKey encryptionKey
    ) throws Exception{

        Cipher encryptionCipher = Cipher.getInstance(algo);

        // Initialize the cipher in decryption mode. It will use the encryptionKey value to perform the decryption
        encryptionCipher.init(Cipher.DECRYPT_MODE, encryptionKey);

        // Creation of a CipherInputStream to decrypt the data as it reads
        // It wraps the input stream for decryption
        CipherInputStream decryptedInputStream = new CipherInputStream(inputFileStream, encryptionCipher);

        byte[] dataBuffer = new byte[1024];
        int bytesRead;

        /*  Same logic as before,
         *   A buffer array of 1024bytes is created to temporarily store data as it's being read from the input stream
         *   decryptedInputStream.read(dataBuffer) reads encrypted data from the input stream, decrypts it and places the
         *   decrypted data into the buffer.
         *   If data is available, the decrypted data is filled up to 1024bytes
         *   If it reaches EOF, then it returns -1, thus breaking the while loop*/
        while((bytesRead = decryptedInputStream.read(dataBuffer)) != -1){
            outputFileStream.write(dataBuffer, 0, bytesRead);
        }

        decryptedInputStream.close(); //Close the stream
    }
}