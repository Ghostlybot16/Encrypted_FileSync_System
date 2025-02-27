package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import javax.crypto.SecretKey;
import utilities.FileEncryptor;

public class Server {
    private static final int serverListenPort = 55000;
    private static final String receivedFilesDir = "../received_files/";
    private static final String decryptedFolder = "../decrypted_data/";

    public static void main(String[] args) {
        System.out.println("Server is active");

        createDirectory(receivedFilesDir);
        createDirectory(decryptedFolder);

        try (ServerSocket serverSocket = new ServerSocket(serverListenPort)) {
            System.out.println("Server is listening on port " + serverListenPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                DataInputStream dataStreamFromClient = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dataStreamToClient = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            boolean keepRunning = true;

            while (keepRunning) {
                String command = dataStreamFromClient.readUTF();

                if ("TERMINATE".equalsIgnoreCase(command)) {
                    System.out.println("Client requested termination.");
                    keepRunning = false;
                    dataStreamToClient.writeUTF("Connection terminated by server.");
                } else if ("SEND".equalsIgnoreCase(command)) {
                    System.out.println("Processing file transfer...");
                    handleFileTransfer(dataStreamFromClient, dataStreamToClient);
                } else {
                    dataStreamToClient.writeUTF("Unknown command.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleFileTransfer(DataInputStream dataStreamFromClient, DataOutputStream dataStreamToClient) throws Exception {
        String clientChecksum = dataStreamFromClient.readUTF();
        System.out.println("Checksum received from client: " + clientChecksum);

        String encryptedClientFileName = dataStreamFromClient.readUTF();
        int saltLength = dataStreamFromClient.readInt();
        byte[] salt = new byte[saltLength];
        dataStreamFromClient.readFully(salt);
        String password = dataStreamFromClient.readUTF();

        System.out.println("Encrypted file name: " + encryptedClientFileName);
        System.out.println("Salt value (hex): " + bytesToHex(salt));
        System.out.println("Password: " + password);

        File destinationFile = new File(receivedFilesDir + encryptedClientFileName);

        // Read file size from the client
        long fileSize = dataStreamFromClient.readLong(); // Client sends the file size
        long bytesReceived = 0; // Track how many bytes have been received

        System.out.println("Expected file size: " + fileSize + " bytes.");

        try (FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
            byte[] dataBuffer = new byte[4096];
            int bytesRead;

            // Continue reading data until the expected file size is reached
            while (bytesReceived < fileSize) {
                bytesRead = dataStreamFromClient.read(dataBuffer);
                if (bytesRead == -1) break; // End of stream
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                bytesReceived += bytesRead;

                // Optional: Display progress on the server side
                int progress = (int) ((bytesReceived * 100) / fileSize);
                System.out.print("\rReceiving file... " + progress + "% complete");
            }
            System.out.println("\nFile transfer complete. Total bytes received: " + bytesReceived + " bytes.");
        }

        // Ensure the file size matches
        if (bytesReceived != fileSize) {
            System.out.println("Warning: Mismatch in file size. Expected " + fileSize + ", but received " + bytesReceived);
        } else {
            System.out.println("File received successfully.");
        }

        // Decrypt the file
        SecretKey secretKey = FileEncryptor.KeyGenFromPassword(password, salt);
        File decryptedFile = new File(decryptedFolder, "decrypted_" + encryptedClientFileName);

        try (
                InputStream encryptedInputStream = new FileInputStream(destinationFile);
                OutputStream decryptedOutputStream = new FileOutputStream(decryptedFile)
        ) {
            FileEncryptor.decryptFile(encryptedInputStream, decryptedOutputStream, secretKey);
            System.out.println("File decrypted successfully at: " + decryptedFile.getAbsolutePath());
        }

        String decryptedChecksum = calculateChecksum(decryptedFile);
        System.out.println("Checksum of decrypted file: " + decryptedChecksum);

        if (clientChecksum.equals(decryptedChecksum)) {
            System.out.println("File integrity verified: Checksums match!------------------------------------------------------------------------------");
            dataStreamToClient.writeUTF("Checksum verification success: Files match!");
        } else {
            System.out.println("File integrity check failed: Checksums do not match!");
            dataStreamToClient.writeUTF("Checksum verification failed: Files do not match!");
        }
    }

    private static String calculateChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] checksumBytes = digest.digest();
        StringBuilder checksum = new StringBuilder();
        for (byte b : checksumBytes) {
            checksum.append(String.format("%02x", b));
        }
        return checksum.toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private static void createDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() && directory.mkdirs()) {
            System.out.println("Directory created at: " + directoryPath);
        }
    }
}
