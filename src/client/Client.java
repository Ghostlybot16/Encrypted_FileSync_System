package client;

import java.io.*;
import java.net.Socket; // Using the socket class for connecting to the server

public class Client {

    // serverIpAddress holds the IP address of the server which the client is going to connect to
    private static final String serverIpAddress = "127.0.0.1";

    // The port number where the server is listening
    private static final int serverPortNumber = 55000;

    public static void main(String[] args){

        // Holds the relative path of the file which the client will be sending to the server
        String fileToUploadPath = "../../resources/testfile.txt";

        // File object which the client will send to server
        File uploadFile = new File(fileToUploadPath);

        //Check to see if file which is going to be sent to the server is valid and does exist
        if (!uploadFile.exists() || !uploadFile.isFile()){
            System.out.println("File does not exist or is not a valid file");
            return;
        }

        try(
            // Connection to the server

            // Connects to the server using the specified IP address and port number
            Socket clientSocket = new Socket(serverIpAddress, serverPortNumber);

            // Reads data from the file that will be sent to the server
            InputStream sourceFileStream = new FileInputStream(uploadFile);

            // Sends data from the client to the server through the socket
            // This stream is sending data from the client to the server
            OutputStream clientToServerStream = clientSocket.getOutputStream();

            // Stream used to send structured data to the server
            // Wraps the socket output stream to send structured data like file name and file data
            DataOutputStream outgoingDataStream = new DataOutputStream(clientToServerStream)
        ){

            // Send file name to the server
            outgoingDataStream.writeUTF(uploadFile.getName());

            // Send file data in chunks of 4KB
            byte[] dataBuffer = new byte[4096]; // Buffer to store file data
            int bytesRead; // Variable to store number of bytes read in each loop iteration

            while((bytesRead = sourceFileStream.read(dataBuffer)) != -1){
                outgoingDataStream.write(dataBuffer, 0, bytesRead); // Write buffer content into the server
            }

            // Confirmation message sent to the terminal to communicate file transfer
            System.out.println("File send: " + uploadFile.getName());
        } catch (IOException e){
            e.printStackTrace();
        }

    }

}
