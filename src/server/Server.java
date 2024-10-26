package server; // Declares that this file is part of the 'server' package

import java.io.*;
import java.net.ServerSocket; // Import used to create a server socket
import java.net.Socket; // Import used to create client-server communication

import javax.crypto.SecretKey;
import utilities.FileEncryptor;

public class Server {

    // Defines the port number that the server will listen on
    // This is the port used by clients to connect
    private static final int serverListenPort = 55000;

    // The directory where the received files sent by clients will be saved
    private static final String receivedFilesDir = "../received_files/";

    // The directory where the decrypted files will be saved
    private static final String decryptedFolder = "../decrypted_data";

    public static void main(String[] args) {

        // Terminal output to communicate that the server is active
        System.out.println("Server is active\n");

        try{
            Thread.sleep(2000); // Delay for 2 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Calling a method to check if the directory for saving files from the client exists
        directoryStatus(receivedFilesDir);

        // Check to see if the directory that is storing the decrypted data exists
        directoryStatus(decryptedFolder);

        try{
            Thread.sleep(2000); // Delay for 2 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Terminal output to communicate that the server socket is being created
        System.out.println("Creating server socket...");

        // Create a server socket that listens for connections in the mentioned port (55000)
        try(ServerSocket serverSocket = new ServerSocket(serverListenPort)) {

            // Terminal output to communicate that the server is active and waiting for a client to join
            System.out.println("Server is listening on port " + serverListenPort);

            // Infinite loop to continuously listen for a client to join
            while(true){
                Socket clientSocket = serverSocket.accept(); // Accept the client
                System.out.println("Client connected.");

                // Method called to handle the file received from the client
                receiveFileFromClientAndDecrypt(clientSocket); //THIS WILL NEED TO BE ALTERED

                clientSocket.close(); // Close client socket after file has been transferred
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    private static void directoryStatus(String directoryPath) {

        // Create a file object that represents the path to the directory
        File directory = new File(directoryPath);

        // Checking to see if the directory to store data from the client
        if(!directory.exists()){
            if(directory.mkdirs()) {    // Attempts to create the directory

                // Outputs path if directory gets created
                System.out.println("Directory created successfully at: " + directory.getAbsolutePath());

            }
            else{
                // Error message if directory fails to create
                System.out.println("Failed to create directory at: " + directory.getAbsolutePath());
            }
        } else {
            System.out.println("Directory already exists at: " + directory.getAbsolutePath());
        }
    }

    private static void receiveFileFromClientAndDecrypt(Socket clientSocket) {

        // try with resources block to automatically close the input stream once it's done being used
        try(
                // Retrieves the input stream from client socket in order to allow the server to read data sent by client
                InputStream clientSocketInputStream = clientSocket.getInputStream();

                // Wrap input stream so it's easier to read files
                // dataStreamFromClient reads data from client
                DataInputStream dataStreamFromClient = new DataInputStream(clientSocketInputStream)
                ){

            // Read the file name sent from the client
            // clientFileName stores the name of the file being transferred from the client
            String encryptedClientFileName = dataStreamFromClient.readUTF();

            //  Receive salt length and salt value from client
            int saltLength = dataStreamFromClient.readInt();
            byte[] salt = new byte[saltLength]; // Byte array to store the salt value
            dataStreamFromClient.readFully(salt);

            // Read the password that the client used for encryption
            String password = dataStreamFromClient.readUTF();

            // Output to the terminal to communicate that the FileName and Password sent by the client has been received by the server
            System.out.println("Received password from client: " + password);
            System.out.println("Received encrypted file name: " + encryptedClientFileName);
            System.out.println("Received salt value: " + saltToHex(salt));

            // Creates a file object that shows where the data from client will be saved within the server
            // The file will be stored in receivedFilesDir location with its filename
            File destinationFileInServer = new File(receivedFilesDir + encryptedClientFileName);


            // fileOutputStream handles writing the data to the file in the server
            try(FileOutputStream fileOutputStream = new FileOutputStream(destinationFileInServer)) {

                // Buffer of 4KB size to read from client file
                byte[] dataBuffer = new byte[4096];
                int bytesRead;

                while((bytesRead = dataStreamFromClient.read(dataBuffer)) != -1){

                    // Write the content inside the buffer to the server storage
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                // Outputs a confirmation message to the terminal that the process has been executed successfully
                System.out.println("Encrypted file received and saved at: " + destinationFileInServer.getAbsolutePath());
            }

            // Creation of secretKey using the received password and salt from client
            // Essentially the secretkey generated in the server should match the secretkey generated in the client
            SecretKey secretKey = FileEncryptor.KeyGenFromPassword(password,salt);

            // Creating a decrypted file directory
            String decryptedFolderName = "decrypted_" + encryptedClientFileName;

            // Decrypt the file and save it to the 'decrypted_data' directory
            File decryptedClientFile = new File(decryptedFolder, decryptedFolderName);
            try(
                    InputStream encryptedInputStream = new FileInputStream(destinationFileInServer);
                    OutputStream decryptedOutputStream = new FileOutputStream(decryptedClientFile);
                    ){

                // Decrypting the file by calling FileEncryptor
                FileEncryptor.decryptFile(encryptedInputStream, decryptedOutputStream, secretKey);
                System.out.println("File decrypted successfully at: " + decryptedClientFile.getAbsolutePath());
            } catch (Exception e){
                System.out.println("Error during file decryption: " + e.getMessage());
            }
        } catch (IOException e){
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    // Method to convert salt values into hex decimal strings
    // This is called to output salt values to the terminal
    private static String saltToHex(byte[] data) {

        // Each byte is 2 hexadecimal characters
        // StringBuilder holds the final hexadecimal string
        StringBuilder hexString = new StringBuilder();

        for(int i = 0; i < data.length; i++) {
             hexString.append(String.format("%02x", data[i])); // Format each byte as 2 character hex
        }
        return hexString.toString(); // Convert StringBuilder to string type and return it
    }


}
