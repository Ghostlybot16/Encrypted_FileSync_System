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

public class FileEncryptor {

    private static final String algo = "AES"; // AES encryption algorithm
    private static final int saltLength = 16; // Salt length for key generation
    private static final int ITERATIONS = 100000; // Number of iterations for PBKDF2
    private static final int keyLength = 128; // AES-128 key length

    // Method to generate AES key using a user-input password and a random generated salt
    public static SecretKey KeyGenFromPassword(String password, byte[] salt) throws Exception {

        // Key specification/blueprint used to generate the cryptographic key
        // Uses the values from the user-input password, salt, ITERATIONS to generate the AES key
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, keyLength);

        /*  SecretKeyFactory provides the functionality to convert key specifications into actual secret keys
        *   which can be used to encrypt/decrypt.
        *   It takes keySpec and uses it to generate a secretKey
        *
        *   Use PBKDF2 algorithm with HMAC-SHA256 hashing algorithm to generate a unique password */
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        // Generates secretKey using the keySpec values of password, salt, iterations and key length
        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        // Returns AES secret key
        return new SecretKeySpec(secretKey.getEncoded(), algo);
    }

    // Method to generate random salt values
    public static byte[] createSalt() {

        // SecureRandom to generate random byte values
        SecureRandom randomNum = new SecureRandom();
        byte[] salt = new byte[saltLength]; // Byte array creation for salt with defined length values
        randomNum.nextBytes(salt); // Fill the array with randomNum byte values that were generated using SecureRandom
        return salt; // Return the generated salt value
    }

    // Method to encrypt file using AES encryption
    public static void encryptFile(InputStream inputFileStream, OutputStream outputFileStream, SecretKey encryptionKey) throws Exception {

        // Create cipher using AES
        // A factory method that creates a Cipher object for the specified (AES) algorithm
        Cipher encryptionCipher = Cipher.getInstance(algo);

        // Initialize the cipher in encryption mode. It will use the encryptionKey value to perform the encryption
        encryptionCipher.init(Cipher.ENCRYPT_MODE, encryptionKey);

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

    // Method to decrypt file
    // Uses the same logic as encryption but this method runs in decryption mode, therefore it decrypts the data
    public static void decryptFile(InputStream inputFileStream, OutputStream outputFileStream, SecretKey encryptionKey) throws Exception{
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