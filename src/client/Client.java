package client;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Scanner;
import javax.crypto.SecretKey;

import utilities.FileEncryptor;

public class Client {
    private static final String serverIpAddress = "127.0.0.1";
    private static final int serverPortNumber = 55000;

    public static void main(String[] args) {
        // List down in the terminal the files present in the "resources" directory so the user can choose
        File resourcesDirectory = new File("../resources/");
        File[] availableFiles = resourcesDirectory.listFiles();

        if (availableFiles == null || availableFiles.length == 0) {
            System.out.println("No files found in the resources directory.");
            return;
        }

        // Print available files
        System.out.println("Available files to send to server:\n");
        for (File file : availableFiles) {
            System.out.println("- " + file.getName());
        }

        // Prompt user to enter the file name
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the name of the file you want to send:");
        String userInputForFileName = scanner.nextLine();
        File nameOfFile = new File(resourcesDirectory, userInputForFileName);

        if (!nameOfFile.exists() || !nameOfFile.isFile()) {
            System.out.println("File not found in directory.");
            return;
        }

        System.out.println("Chosen file to encrypt: " + nameOfFile.getName());
        System.out.println("\nEnter a password for encryption:");
        String userPassword = scanner.nextLine();

        // Encrypt the file
        String encryptedFileName = "encrypted_" + nameOfFile.getName();
        File encryptedFolder = new File("../encrypted_data");

        if (!encryptedFolder.exists() && !encryptedFolder.mkdirs()) {
            System.out.println("Failed to create 'encrypted_data' directory.");
            return;
        }

        byte[] salt = FileEncryptor.createSalt();
        SecretKey secretKey;
        try {
            secretKey = FileEncryptor.KeyGenFromPassword(userPassword, salt);
        } catch (Exception e) {
            System.out.println("Error generating secret key: " + e.getMessage());
            return;
        }

        File encryptedFile = new File(encryptedFolder, encryptedFileName);
        try (
                InputStream inputFileStream = new FileInputStream(nameOfFile);
                OutputStream encryptedOutputStream = new FileOutputStream(encryptedFile)
        ) {
            FileEncryptor.encryptFile(inputFileStream, encryptedOutputStream, secretKey);
            System.out.println("File encrypted successfully as: " + encryptedFileName);
        } catch (Exception e) {
            System.out.println("Error encrypting file: " + e.getMessage());
            return;
        }

        try (
                Socket clientSocket = new Socket(serverIpAddress, serverPortNumber);
                InputStream sourceFileStream = new FileInputStream(encryptedFile);
                DataOutputStream outgoingDataStream = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            // Calculate and send checksum
            String checksum = calculateChecksum(nameOfFile);
            System.out.println("Checksum of original file: " + checksum);
            outgoingDataStream.writeUTF(checksum);

            // Send file metadata
            outgoingDataStream.writeUTF(encryptedFile.getName());
            outgoingDataStream.writeInt(salt.length);
            outgoingDataStream.write(salt);
            outgoingDataStream.writeUTF(userPassword);

            // Send file data
            long fileSize = encryptedFile.length();
            long bytesSent = 0;
            byte[] dataBuffer = new byte[4096];
            int bytesRead;

            System.out.println("Uploading file to server...");
            long startTime = System.nanoTime(); // Start timer
            while ((bytesRead = sourceFileStream.read(dataBuffer)) != -1) {
                outgoingDataStream.write(dataBuffer, 0, bytesRead);
                bytesSent += bytesRead;

                // Calculate progress
                int progress = (int) ((bytesSent * 100) / fileSize);

                // Calculate speed in KB/s
                long elapsedTime = System.nanoTime() - startTime; // time in nanoseconds
                double elapsedTimeInSeconds = elapsedTime / 1e9; // convert to seconds
                double speed = (bytesSent / 1024.0) / elapsedTimeInSeconds;

                // Display progress bar with speed
                int barLength = 50;
                int filledBars = (progress * barLength) / 100;
                String progressBar = "=".repeat(filledBars) + " ".repeat(barLength - filledBars);

                System.out.print(String.format("\r[%s] %d%% | Speed: %.2f KB/s", progressBar, progress, speed));
            }
            System.out.println("\nFile upload complete.");
        } catch (Exception e) {
            e.printStackTrace();
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
}
