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
        Scanner scanner = new Scanner(System.in);

        try (Socket clientSocket = new Socket(serverIpAddress, serverPortNumber);
             DataOutputStream outgoingDataStream = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream incomingDataStream = new DataInputStream(clientSocket.getInputStream())) {

            boolean continueConnection = true;

            while (continueConnection) {
                System.out.println("\nOptions:");
                System.out.println("1. Send a file");
                System.out.println("2. Terminate connection");
                System.out.print("Enter your choice: ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        outgoingDataStream.writeUTF("SEND"); // Notify server about file transfer
                        sendFileToServer(scanner, outgoingDataStream, incomingDataStream);
                        break;
                    case "2":
                        outgoingDataStream.writeUTF("TERMINATE"); // Notify server about termination
                        System.out.println("Terminating connection to the server...");
                        String serverResponse = incomingDataStream.readUTF();
                        System.out.println("Server: " + serverResponse);
                        continueConnection = false;
                        break;
                    default:
                        System.out.println("Invalid input. Please enter 1 or 2.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Client has disconnected.");
    }

    private static void sendFileToServer(Scanner scanner, DataOutputStream outgoingDataStream, DataInputStream incomingDataStream) {
        try {
            File resourcesDirectory = new File("../resources/");
            File[] availableFiles = resourcesDirectory.listFiles();

            if (availableFiles == null || availableFiles.length == 0) {
                System.out.println("No files found in the resources directory.");
                return; // Ensure the return only exits this method, not the program.
            }

            System.out.println("Available files to send to server:\n");
            for (File file : availableFiles) {
                System.out.println("- " + file.getName());
            }

            System.out.println("Enter the name of the file you want to send:");
            String userInputForFileName = scanner.nextLine();
            File nameOfFile = new File(resourcesDirectory, userInputForFileName);

            if (!nameOfFile.exists() || !nameOfFile.isFile()) {
                System.out.println("File not found in directory.");
                return; // Same here: ensure this just exits the method.
            }

            System.out.println("Chosen file to encrypt: " + nameOfFile.getName());
            System.out.println("\nEnter a password for encryption:");
            String userPassword = scanner.nextLine();

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
                    InputStream sourceFileStream = new FileInputStream(encryptedFile)
            ) {
                String checksum = calculateChecksum(nameOfFile);
                System.out.println("Checksum of original file: " + checksum);
                outgoingDataStream.writeUTF(checksum);

                outgoingDataStream.writeUTF(encryptedFile.getName());
                outgoingDataStream.writeInt(salt.length);
                outgoingDataStream.write(salt);
                outgoingDataStream.writeUTF(userPassword);

                long fileSize = encryptedFile.length();
                outgoingDataStream.writeLong(fileSize); // Send file size to server

                long bytesSent = 0;
                byte[] dataBuffer = new byte[4096];
                int bytesRead;

                System.out.println("Uploading file to server...");
                long startTime = System.nanoTime();
                while ((bytesRead = sourceFileStream.read(dataBuffer)) != -1) {
                    outgoingDataStream.write(dataBuffer, 0, bytesRead);
                    bytesSent += bytesRead;

                    int progress = (int) ((bytesSent * 100) / fileSize);
                    long elapsedTime = System.nanoTime() - startTime;
                    double elapsedTimeInSeconds = elapsedTime / 1e9;
                    double speed = (bytesSent / 1024.0) / elapsedTimeInSeconds;

                    int barLength = 50;
                    int filledBars = (progress * barLength) / 100;
                    String progressBar = "=".repeat(filledBars) + " ".repeat(barLength - filledBars);

                    System.out.print(String.format("\r[%s] %d%% | Speed: %.2f KB/s", progressBar, progress, speed));
                }
                System.out.println("\nFile upload complete.");

                String serverResponse = incomingDataStream.readUTF(); // Read server's response
                System.out.println("Server: " + serverResponse);
            } catch (Exception e) {
                System.out.println("Error during file upload: " + e.getMessage());
            }

            System.out.println("Returning to main menu...");
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
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
