import java.io.*; // Input/Output classes for file handling
import javax.crypto.Cipher; // Provides cryptographic encryption and decryption functions
import javax.crypto.SecretKey; // Secret key used in encryption and decryption
import javax.crypto.spec.SecretKeySpec; // Converts byte data into secret key
import javax.crypto.CipherOutputStream; // Class that encrypts data as it's written
import javax.crypto.CipherInputStream; // Class that decrypts data as it's read
import javax.crypto.SecretKeyFactory; // Used to generate a key from the password using PBKDF2
import javax.crypto.spec.PBEKeySpec; // Specification for password based encryption
import java.security.SecureRandom; // Generates random salt (random value) for key generation

// Salt = a random value

public class FileEncryptor {

    private static final String algo = "AES"; // AES encryption algorithm
    private static final int saltLength = 16; // salt length for key generation
    private static final int ITERATIONS = 100000; // Number of iterations for PBKDF2
    private static final int keyLength = 128; // AES-128 key length

    // Method to generate AES key from a user-input password and a random salt
    public static SecretKey KeyGenFromPassword(String password, byte[] salt) throws Exception {

        // Key specification/Blueprint used to generate the cryptographic key
        // Uses the values of user-inputted password, salt, ITERATIONS to generate the AES key
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, keyLength);

        /*  SecretKeyFactory provides the functionality to convert key specifications into actual secret keys
        *   which can be used to encrypt/decrypt.
        *   It takes keySpec and uses it to generate a secretKey
        *
        *   This line says to use PBKDF2 algorithm with HMAC-SHA256 hashing algorithm to generate a unique password */
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        // Generates secretKey using the keySpec values of password, salt, iterations and key length
        SecretKey secretKey = keyFactory.generateSecret(keySpec);

        // returns AES-compatible secret key
        return new SecretKeySpec(secretKey.getEncoded(), algo);
    }

}