package server;

import rmi.DistributedFileService;
import utilities.FileEncryptor;

import javax.crypto.SecretKey;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.Arrays;

public class DistributedFileServiceImpl extends UnicastRemoteObject implements DistributedFileService {
    private static final String receivedFilesDir = "../received_files/";
    private static final String decryptedFolder = "../decrypted_data/";

    // Constructor
    protected DistributedFileServiceImpl() throws RemoteException {
        super();
        createDirectory(receivedFilesDir);
        createDirectory(decryptedFolder);
    }

    @Override
    public String sendFile(String fileName, byte[] fileData, String password, byte[] salt, byte[] iv, String originalFileChecksum) throws RemoteException {
        try {
            System.out.println("***Client Connected***");

            File destinationFile = new File(receivedFilesDir, fileName);

            // Log the password, salt, and IV received from the client
            System.out.println("Password used for encryption: " + password);
            System.out.println("Salt (hex): " + bytesToHex(salt));
            System.out.println("IV (hex): " + bytesToHex(iv));

            // Save the encrypted file to disk
            try (FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
                fileOutputStream.write(fileData);
            }
            System.out.println("Encrypted file saved at: " + destinationFile.getAbsolutePath());

            // Decrypt the file using the IV
            SecretKey secretKey = FileEncryptor.KeyGenFromPassword(password, salt);
            File decryptedFile = new File(decryptedFolder, "decrypted_" + fileName);
            try (
                    InputStream encryptedInputStream = new FileInputStream(destinationFile);
                    OutputStream decryptedOutputStream = new FileOutputStream(decryptedFile)
            ) {
                FileEncryptor.decryptFile(encryptedInputStream, decryptedOutputStream, secretKey, iv);
            }
            System.out.println("Decrypted file saved at: " + decryptedFile.getAbsolutePath());

            // Calculate the checksum of the decrypted file
            String decryptedFileChecksum = calculateFileChecksum(decryptedFile);
            System.out.println("Checksum of decrypted file: " + decryptedFileChecksum);

            // Compare the checksum of the decrypted file with the original file checksum
            if (originalFileChecksum.equals(decryptedFileChecksum)) {
                System.out.println("*** Checksum Match: File integrity verified. ***");
            } else {
                System.out.println("*** Checksum Mismatch: File integrity verification failed. ***");
            }
            return "File received and decrypted successfully at: " + decryptedFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RemoteException("Error in sendFile: " + e.getMessage(), e);
        }
    }

    @Override
    public String terminateConnection() throws RemoteException {
        System.out.println("***Client Disconnected***");
        return "Connection terminated.";
    }

    @Override
    public String checkIntegrity(String originalChecksum, byte[] fileData) throws RemoteException {
        return "Integrity check not implemented yet.";
    }

    @Override
    public byte[] encryptFile(byte[] fileData, String password, byte[] salt) throws RemoteException {
        return new byte[0];
    }

    @Override
    public byte[] decryptFile(byte[] encryptedData, String password, byte[] salt) throws RemoteException {
        return new byte[0];
    }

    private static void createDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() && directory.mkdirs()) {
            System.out.println("Directory created at: " + directoryPath);
        }
    }

    // Utility method to convert bytes to hex
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    // Utility method to calculate checksum of a file
    private String calculateFileChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] checksumBytes = digest.digest();
        return bytesToHex(checksumBytes);
    }

    // Utility method to read the first few bytes of a file for debugging
    private byte[] readFileFirstBytes(File file, int byteCount) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[byteCount];
            int bytesRead = fis.read(buffer);
            if (bytesRead < byteCount) {
                return Arrays.copyOf(buffer, bytesRead);
            }
            return buffer;
        }
    }
}
