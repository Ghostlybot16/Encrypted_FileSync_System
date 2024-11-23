package client;

import java.io.*;
import java.net.Socket; // Using the socket class for connecting to the server
import java.util.Scanner;

import utilities.FileEncryptor;
import javax.crypto.SecretKey;

public class Client {

    // serverIpAddress holds the IP address of the server which the client is going to connect to
    private static final String serverIpAddress = "127.0.0.1"; //IP address of the server

    // The port number where the server is listening
    private static final int serverPortNumber = 55000;

    public static void main(String[] args){

        // List down in the terminal the files present in the "resources" directory so the user can choose
        File resourcesDirectory = new File("../resources/"); // Path to resources directory

        File[] availableFiles = resourcesDirectory.listFiles(); // Create list of files in the directory

        if(availableFiles == null || availableFiles.length == 0){
            System.out.println("No files found in the resources directory."); // Output to the terminal if no files in directory
            return;
        }

        // Print to the terminal the list of available files
        System.out.println("Available files to send to server:\n");
        for (File file : availableFiles) {
            System.out.println("- " + file.getName()); // hyphen in front of each list item
        }

        // Prompt user to enter the name of the file the needs to be sent to the server
        Scanner scanner = new Scanner(System.in); // Scanner obj to read user input
        System.out.println("Enter the name of the file you want to send:");
        String userInputForFileName = scanner.nextLine();

        // Check to see if user-input file name exists in the resources directory
        File nameOfFile = new File(resourcesDirectory, userInputForFileName); // File obj for user chosen file

        // if the user input file does not exist or is not valid
        if(!nameOfFile.exists() || !nameOfFile.isFile()) {
            System.out.println("File not found in directory.");
            return; // Exits if file not found
        }

        System.out.println("Chosen file to encrypt: " + nameOfFile.getName());
        System.out.println("\n");

        // User inputs the password which gets used to encrypt
        System.out.println("Enter a password for encryption: ");
        String userPassword = scanner.nextLine();

        // Automatically retrieve the file name using getName() to create the encrypted file name
        // primaryNameOfFile = raw file within the resources directory
        String primaryNameOfFile = nameOfFile.getName();
        String encryptedFileName = "encrypted_" + primaryNameOfFile; // example: "encrypted_testfile.txt"

        // Checking to see if the directory where the encrypted data will be stored already exists
        File encryptedFolder = new File("../encrypted_data");

        //  If the encryptedFolder does not exist then it creates the directory
        if(!encryptedFolder.exists()){
            if(encryptedFolder.mkdirs()){
                System.out.println("Directory 'encrypted_data' created successfully at: " + encryptedFolder.getAbsolutePath());
            } else {
                System.out.println("Failed to create 'encrypted_data' directory at: " + encryptedFolder.getAbsolutePath());
                return;
            }
        } else {
            System.out.println("'encrypted_data' directory already exists at: " + encryptedFolder.getAbsolutePath());
        }

        // Generate salt and create secret key from password and salt
        byte[] salt = FileEncryptor.createSalt();
        SecretKey secretKey;
        try{
            secretKey = FileEncryptor.KeyGenFromPassword(userPassword, salt);
        } catch(Exception e){
            System.out.println("Error generating secret key: " + e.getMessage());
            return;
        }

        // Encrypt the file and save it in the encrypted_data directory
        File encryptedFile = new File(encryptedFolder, encryptedFileName);
        try(
                InputStream inputFileStream = new FileInputStream(nameOfFile);
                OutputStream encryptedOutputStream = new FileOutputStream(encryptedFile);
        ) {
            // Encrypt the raw file by calling FileEncryptor
            FileEncryptor.encryptFile(inputFileStream, encryptedOutputStream, secretKey);
            System.out.println("File encrypted successfully as: " + encryptedFileName);
        } catch(Exception e){
            System.out.println("Error encrypting file: " + e.getMessage());
            return; // Exit if encryption fails
        }

        try(
                // Connection to the server
                Socket clientSocket = new Socket(serverIpAddress, serverPortNumber);

                InputStream sourceFileStream = new FileInputStream(encryptedFile);

                DataInputStream serverDataInput = new DataInputStream(clientSocket.getInputStream());

                OutputStream clientToServerStream = clientSocket.getOutputStream();
                DataOutputStream outgoingDataStream = new DataOutputStream(clientToServerStream)
        )
        {
            // Start a new thread to listen for updates from the server
            new Thread(() -> {
                try {
                    while (true) {
                        // Read updates from the server and print them to the console
                        String update = serverDataInput.readUTF();
                        System.out.println("Update from server: " + update);
                    }
                } catch (IOException e) {
                    System.out.println("Connection to server disconnected.");
                }
            }).start();

            // Send file metadata
            outgoingDataStream.writeUTF(encryptedFile.getName());
            outgoingDataStream.writeInt(salt.length);
            outgoingDataStream.write(salt);
            outgoingDataStream.writeUTF(userPassword);

            // File size to calculate progress
            long fileSize = encryptedFile.length();
            long bytesSent = 0;

            byte[] dataBuffer = new byte[4096]; // Buffer to store file data
            int bytesRead;

            System.out.println("Uploading file to server...");

            while((bytesRead = sourceFileStream.read(dataBuffer)) != -1){
                outgoingDataStream.write(dataBuffer, 0, bytesRead);
                bytesSent += bytesRead;

                // Calculate progress percentage
                int progress = (int)((bytesSent * 100) / fileSize);

                // Display progress bar
                int barLength = 50; // Length of the progress bar
                int filledBars = (progress * barLength) / 100; // Number of '=' characters
                String progressBar = "=".repeat(filledBars) + " ".repeat(barLength - filledBars);

                System.out.print("\r[" + progressBar + "] " + progress + "%");
            }


            System.out.println("\nFile upload complete.");
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
