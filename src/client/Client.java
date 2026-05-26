package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.crypto.SecretKey;

import utilities.FileEncryptor;
import utilities.ChecksumUtil;

/**
 * Client-side program for connecting to a file transfer server
 *
 * This client allows the user to:
 * - Connect to a server using a socket.
 * - Choose a file from the resources directory.
 * - Encrypt the selected file using a password.
 * - Send the encrypted file to the server.
 * - Send checksum data so the server can verify file integrity.
 * - Terminate the connection safely
 */

public class Client {
    /**
     * IP address of the server.
     * 127.0.0.1 means the server is running on the same machine.
     */
    private static final String serverIpAddress = "127.0.0.1";

    /**
     * Port number used to connect to the server.
     * The server must be listening on this same port.
     */
    private static final int serverPortNumber = 55000;


    /**
     * Main entry point of the client application.
     *
     * This method:
     * - Connects to the server.
     * - Displays a menu to the user.
     * - Allows the user to send files or terminate the connection.
     *
     * @param args command-line arguments, not used in this program.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (
                Socket clientSocket = new Socket(serverIpAddress, serverPortNumber);
                DataOutputStream outgoingDataStream = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream incomingDataStream = new DataInputStream(clientSocket.getInputStream())
        ) {

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

    /**
     * Handles the full process of selecting, encrypting, and sending a file to the server.
     *
     * This method:
     * - Lists available files from the 'resources' directory.
     * - Asks the user to choose a file.
     * - Asks the user for an encryption password.
     * - Generates a salt and secret key.
     * - Encrypts the selected file.
     * - Calculates the checksum of the original file.
     * - Sends the checksum, encrypted file name, salt, password, file size,
     *   and encrypted file content to the server.
     * - Displays upload progress and speed.
     *
     * @param scanner user input scanner used to read file name and password.
     * @param outgoingDataStream stream used to send data to the server.
     * @param incomingDataStream stream used to receive data from the server.
     */
    private static void sendFileToServer(
            Scanner scanner,
            DataOutputStream outgoingDataStream,
            DataInputStream incomingDataStream
    ) {
        try {
            // Helper method is called
            File nameOfFile = selectFileFromResources(scanner);

            if (nameOfFile == null) {
                return;
            }

            System.out.println("Chosen file to encrypt: " + nameOfFile.getName());
            System.out.println("\nEnter a password for encryption:");

            String userPassword = scanner.nextLine();

            String encryptedFileName = "encrypted_" + nameOfFile.getName();
            File encryptedFolder = new File("../encrypted_data");

            // Create encrypted_data folder if it does not already exist.
            if (!encryptedFolder.exists() && !encryptedFolder.mkdirs()) {
                System.out.println("Failed to create 'encrypted_data' directory.");
                return;
            }

            // Generate salt and encryption key from the user's password.
            byte[] salt = FileEncryptor.createSalt();
            SecretKey secretKey;

            try {
                secretKey = FileEncryptor.KeyGenFromPassword(userPassword, salt);
            } catch (Exception e) {
                System.out.println("Error generating secret key: " + e.getMessage());
                return;
            }

            File encryptedFile = new File(encryptedFolder, encryptedFileName);

            // Encrypt the selected file and save it into encrypted_data directory.
            boolean encryptionSuccessful = encryptSelectedFile(
                    nameOfFile,
                    encryptedFile,
                    secretKey
            );

            if (!encryptionSuccessful) {
                return;
            }

            // Send encrypted file data to the server.
            try (InputStream sourceFileStream = new FileInputStream(encryptedFile)) {

                // Calculate checksum of the original file before encryption.
                String checksum = ChecksumUtil.calculateChecksum(nameOfFile);
                System.out.println("Checksum of original file: " + checksum);

                // Send metadata required by the server.
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

                    String progressBar =
                            "=".repeat(filledBars) + " ".repeat(barLength - filledBars);

                    System.out.print(
                            String.format(
                                    "\r[%s] %d%% | Speed: %.2f KB/s",
                                    progressBar,
                                    progress,
                                    speed
                            )
                    );
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

    /**
     * Displays available files from the resources directory and allows the user
     * to select one file for transfer.
     *
     * This method:
     * - Lists all files available in the resources directory.
     * - Prompts the user to choose a file.
     * - Validates the selected file exists.
     * - Returns the selected file object if valid.
     *
     * @param scanner user input scanner used to read the selected file name.
     * @return selected File object if valid, otherwise null
     */
    private static File selectFileFromResources(Scanner scanner) {

        try {
            File resourcesDirectory = new File("../resources/");

            // Check if the resources directory exists for the user to use
            if (!resourcesDirectory.exists()) {
                System.out.println("Resources directory does not exist.\nIt needs to be created to allow file transfer.");
                return null;
            }

            File[] availableFiles = resourcesDirectory.listFiles();

            // Check if the resources directory is empty
            if (availableFiles == null || availableFiles.length == 0) {
                System.out.println("No files found in the resources directory.");
                return null;
            }

            System.out.println("Available files to send to server:\n");

            for (File file: availableFiles) {
                System.out.println("- " + file.getName());
            }

            System.out.println("Enter the name of the file you want to send:");

            String userInputForFileName = scanner.nextLine();

            File selectedFile = new File(resourcesDirectory, userInputForFileName);

            // Validate if user selected file exists and is a valid file to send.
            if (!selectedFile.exists() || !selectedFile.isFile()) {
                System.out.println("File not found in directory.");
                return null;
            }

            return selectedFile;

        } catch (Exception e) {
            System.out.println("An error occured while selecting the file: " + e.getMessage());

            return null;
        }
    }

    /**
     * Encrypts the selected file and saves it inside the "encrypted_data" directory.
     *
     * @param originalFile original file selected by the user to become encrypted.
     * @param encryptedFile file location where encrypted data will be saved.
     * @param secretKey encryption key used to encrypt the file.
     * @return true if encryption succeeds, otherwise false.
     */
    private static boolean encryptSelectedFile(
            File originalFile,
            File encryptedFile,
            SecretKey secretKey
    ) {
        try (
                InputStream inputFileStream = new FileInputStream(originalFile);
                OutputStream encryptedOutputStream = new FileOutputStream(encryptedFile)
        ) {
            FileEncryptor.encryptFile(inputFileStream, encryptedOutputStream, secretKey);
            System.out.println("File encrypted successfully as: " + encryptedFile.getName());
            return true;
        } catch (Exception e) {
            System.out.println("Error encrypting file: " + e.getMessage());
            return false;
        }
    }
}