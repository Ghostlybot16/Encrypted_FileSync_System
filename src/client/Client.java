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
            System.out.println("- " + file.getName()); // hyphen infront of each list item
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

        System.out.println("You have selected: " + nameOfFile.getName());
        System.out.println("\n");

        // User inputs the password which gets used to encrypt
        System.out.println("Enter your password for encryption:");
        String userPassword = scanner.nextLine();

        // Automatically retrieve the file name using getName() to create the encrypted file name
        // primaryNameOfFile = raw file within the resources directory
        String primaryNameOfFile = nameOfFile.getName();
        String encryptedFileName = "encrypted_" + primaryNameOfFile; // example: "encrypted_testfile.txt"

        // Checking to see if the directory where the encrypted data will be stored already exists
        File encryptedFolder = new File("../../encrypted_data");

        //  If the encryptedFolder does not exist then it creates the directory
        if(!encryptedFolder.exists()){
            if(encryptedFolder.mkdirs()){
                System.out.println("Directory 'encrypted_data' created successfully.");
            } else {
                System.out.println("Failed to create 'encrypted_data' directory.");
                return;
            }
        }

        // Generate salt and create secretkey from password and salt
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
            // Encrypt the raw file and save the encrypted version
            FileEncryptor.encryptFile(inputFileStream, encryptedOutputStream, secretKey);
            System.out.println("File encrypted successfully as: " + encryptedFileName);
        } catch(Exception e){
            System.out.println("Error encrypting file: " + e.getMessage());
            return; // Exit if encryption fails
        }

        try(
            // Connection to the server

            // Connects to the server using the specified IP address and port number
            Socket clientSocket = new Socket(serverIpAddress, serverPortNumber);

            // Reads data from the file that will be sent to the server
            InputStream sourceFileStream = new FileInputStream(encryptedFile);

            // Sends data from the client to the server through the socket
            // This stream is sending data from the client to the server
            OutputStream clientToServerStream = clientSocket.getOutputStream();

            // Stream used to send structured data to the server
            // Wraps the socket output stream to send structured data like file name and file data
            DataOutputStream outgoingDataStream = new DataOutputStream(clientToServerStream)
        ){

            // Send file name to the server
            outgoingDataStream.writeUTF(encryptedFile.getName());

            // Send file data in chunks of 4KB
            byte[] dataBuffer = new byte[4096]; // Buffer to store file data
            int bytesRead; // Variable to store number of bytes read in each loop iteration

            while((bytesRead = sourceFileStream.read(dataBuffer)) != -1){
                outgoingDataStream.write(dataBuffer, 0, bytesRead); // Write buffer content into the server
            }

            // Confirmation message sent to the terminal to communicate file transfer
            System.out.println("Encrypted file sent: " + encryptedFile.getName());
        } catch (IOException e){
            e.printStackTrace();
        }

    }

}
