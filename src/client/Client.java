package client;

import rmi.DistributedFileService;
import utilities.FileEncryptor;

import javax.crypto.SecretKey;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            // Connect to the RMI Registry on localhost and port 1099
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // Lookup the DistributedFileService in the registry
            DistributedFileService service = (DistributedFileService) registry.lookup("DistributedFileService");

            Scanner scanner = new Scanner(System.in);

            boolean keepRunning = true;

            while (keepRunning) {
                System.out.println("\nOptions:");
                System.out.println("1. Send a file");
                System.out.println("2. Terminate connection");
                System.out.print("Enter your choice: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        sendFile(scanner, service);
                        break;

                    case "2":
                        // Terminate connection
                        System.out.println("Terminating connection...");
                        String terminationResponse = service.terminateConnection();
                        System.out.println("Server response: " + terminationResponse);
                        keepRunning = false;
                        break;

                    default:
                        System.out.println("Invalid input. Please try again.");
                }
            }

            System.out.println("Client has disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendFile(Scanner scanner, DistributedFileService service) {
        try {
            // List files in the resources directory
            File resourcesDirectory = new File("../resources/");
            File[] availableFiles = resourcesDirectory.listFiles();

            if (availableFiles == null || availableFiles.length == 0) {
                System.out.println("No files found in the resources directory.");
                return;
            }

            System.out.println("Available files to send to server:");
            for (File file : availableFiles) {
                System.out.println("- " + file.getName());
            }

            System.out.print("Enter the name of the file to send: ");
            String fileName = scanner.nextLine();
            File fileToSend = new File(resourcesDirectory, fileName);

            if (!fileToSend.exists() || !fileToSend.isFile()) {
                System.out.println("File not found.");
                return;
            }

            System.out.print("Enter a password for encryption: ");
            String password = scanner.nextLine();

            // Read the file content
            byte[] fileData = readFileAsBytes(fileToSend);
            byte[] salt = FileEncryptor.createSalt();
            SecretKey secretKey = FileEncryptor.KeyGenFromPassword(password, salt);

            // Create the encrypted_data directory if it doesn't exist
            File encryptedDataDir = new File("../encrypted_data/");
            if (!encryptedDataDir.exists() && encryptedDataDir.mkdirs()) {
                System.out.println("Created directory: " + encryptedDataDir.getAbsolutePath());
            }

            // Save the encrypted file to the encrypted_data folder
            File encryptedFile = new File(encryptedDataDir, "encrypted_" + fileName);
            byte[] iv;
            try (FileOutputStream encryptedFileOutput = new FileOutputStream(encryptedFile)) {
                iv = FileEncryptor.encryptFile(new ByteArrayInputStream(fileData), encryptedFileOutput, secretKey);
            }
            System.out.println("Encrypted file saved at: " + encryptedFile.getAbsolutePath());

            // Send the encrypted file to the server with progress bar and speed
            long fileSize = encryptedFile.length();
            try (FileInputStream fileInputStream = new FileInputStream(encryptedFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long bytesSent = 0;
                long startTime = System.nanoTime();

                System.out.println("Sending file to the server...");
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    // Simulate sending chunks (you could use a chunked transfer if needed)
                    bytesSent += bytesRead;

                    // Calculate progress
                    int progress = (int) ((bytesSent * 100) / fileSize);

                    // Calculate speed
                    long elapsedTime = System.nanoTime() - startTime;
                    double elapsedSeconds = elapsedTime / 1e9;
                    double speed = (bytesSent / 1024.0) / elapsedSeconds; // Speed in KB/s

                    // Display progress bar
                    int barLength = 50;
                    int filledBars = (progress * barLength) / 100;
                    String progressBar = "=".repeat(filledBars) + " ".repeat(barLength - filledBars);

                    System.out.print(String.format("\r[%s] %d%% | Speed: %.2f KB/s", progressBar, progress, speed));
                }
                System.out.println("\nFile transfer complete.");
            }

            // Send metadata and encrypted data to the server
            byte[] encryptedData = readFileAsBytes(encryptedFile);
            String response = service.sendFile(fileName, encryptedData, password, salt, iv);
            System.out.println("Server Response: " + response);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static byte[] readFileAsBytes(File file) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }
}
