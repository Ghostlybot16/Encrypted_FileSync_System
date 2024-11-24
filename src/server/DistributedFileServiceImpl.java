package server;

import rmi.DistributedFileService;
import utilities.FileEncryptor;

import javax.crypto.SecretKey;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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
    public String sendFile(String fileName, byte[] fileData, String password, byte[] salt, byte[] iv) throws RemoteException {
        try {
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

            return "File received and decrypted successfully at: " + decryptedFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RemoteException("Error in sendFile: " + e.getMessage(), e);
        }
    }

    @Override
    public String terminateConnection() throws RemoteException {
        return "Connection terminated.";
    }

    @Override
    public String checkIntegrity(String originalChecksum, byte[] fileData) throws RemoteException {
        // Optional: Implement checksum verification
        return "Integrity check not implemented yet.";
    }

    @Override
    public byte[] encryptFile(byte[] fileData, String password, byte[] salt) throws RemoteException {
        // Optional: Implement encryption logic
        return new byte[0];
    }

    @Override
    public byte[] decryptFile(byte[] encryptedData, String password, byte[] salt) throws RemoteException {
        // Optional: Implement decryption logic
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

    public static void main(String[] args) {
        try {
            // Create the service implementation
            DistributedFileService service = new DistributedFileServiceImpl();

            // Start RMI Registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Bind the service to the RMI registry
            registry.rebind("DistributedFileService", service);
            System.out.println("RMI Server is running and waiting for client requests...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
