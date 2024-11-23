import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import utilities.FileEncryptor;

public class EncryptionTest {
    public static void main(String[] args) {
        try {
            // Test Case 1: General file with strings, numbers, and symbols
            runTest("C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\resources\\testfile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\encrypted_data\\encrypted_testfile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\decrypted_data\\decrypted_testfile.txt");

            // Test Case 2: Large file
            runTest("C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\resources\\largefile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\encrypted_data\\encrypted_largefile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\decrypted_data\\decrypted_largefile.txt");

            // Test Case 3: Empty file
            runTest("C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\resources\\emptyfile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\encrypted_data\\encrypted_emptyfile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\decrypted_data\\decrypted_emptyfile.txt");

            // Test Case 4: Binary file
            runTest("C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\resources\\binaryfile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\encrypted_data\\encrypted_binaryfile.txt", "C:\\Distributed_Sys_Assignments\\encrypted_filesync_proj\\encrypted_filesync\\decrypted_data\\decrypted_binaryfile.txt");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Utility method to run encryption and decryption for each test case
    public static void runTest(String originalFilePath, String encryptedFilePath, String decryptedFilePath) {
        try {
            // Generate a secret key
            String password = "myStrongPassword";  // Use a strong password for the key
            byte[] salt = FileEncryptor.createSalt();  // Generate a random salt
            SecretKey secretKey = FileEncryptor.KeyGenFromPassword(password, salt);

            // Step 1: Encrypt the original file
            try (FileInputStream inputFile = new FileInputStream(originalFilePath);
                 FileOutputStream encryptedOutput = new FileOutputStream(encryptedFilePath)) {
                FileEncryptor.encryptFile(inputFile, encryptedOutput, secretKey);
            }

            // Step 2: Decrypt the encrypted file
            try (FileInputStream encryptedInput = new FileInputStream(encryptedFilePath);
                 FileOutputStream decryptedOutput = new FileOutputStream(decryptedFilePath)) {
                FileEncryptor.decryptFile(encryptedInput, decryptedOutput, secretKey);
            }

            // Output the result
            System.out.println("Encryption and decryption completed for: " + originalFilePath);
            System.out.println("Check the files '" + encryptedFilePath + "' and '" + decryptedFilePath + "' to verify the results.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}