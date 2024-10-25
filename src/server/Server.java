package server; // Declares that this file is part of the 'server' package

import java.io.*;
import java.net.ServerSocket; // Import used to create a server socket
import java.net.Socket; // Import used to create client-server communication

public class Server {

    // Defines the port number that the server will listen on
    // This is the port used by clients to connect
    private static final int serverListenPort = 55000;

    // The directory where the received files sent by clients will be saved
    private static final String receivedFilesDir = "../received_files/";

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
                receiveFileFromClient(clientSocket);

                clientSocket.close(); // Close client socket after file has been transferred
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }


    private static void directoryStatus(String directoryPath) {

        // Create a file object that represents the path to the directory
        File directory = new File(directoryPath);

        // Checking to see if the directory to store data from the client already exists
        if(!directory.exists()){
            if(directory.mkdirs()) {    // Attempts to create the directory

                // Outputs path if directory gets created
                System.out.println("Directory created at: " + directoryPath);
            }
            else{

                // Error message if directory fails to create
                System.out.println("Failed to create directory at: " + directoryPath);
            }
        }
    }

    private static void receiveFileFromClient(Socket clientSocket) {

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
            String clientFileName = dataStreamFromClient.readUTF();

            // Creates a file object that shows where the data from client will be saved within the server
            // The file will be stored in receivedFilesDir location with its filename
            File destinationFileInServer = new File(receivedFilesDir + clientFileName);


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
                System.out.println("File received and saved as: " + destinationFileInServer.getAbsolutePath());
            }

        } catch (IOException e){
            e.printStackTrace();
        }

    }

}
