# Encrypted File Transfer Program

Student Name: Kramptj KC\
Student ID: 100787909 

This application is a Java-based encrypted file transfer system that allows clients to safely upload files to a server. 
After the server receives the file, it decrypts the file and stores it. This project supoorts real time notifications for transfer
completion and uses multithreaded handling for simultaneous client connections. This project showcases a secure client-server 
architecture system that emphasizes encryption, password based secret key generation, real-time notifications and a multi-threaded server.

# Features 
- **File Encryption**: Encrypt files in client side using a password-based key created from a uniquely generate salt. (16 byte value)
- **File Transfer**: Send encrypted files fomr the client to the server over a socket.
- **File Decryption**: Decrypt files in the server after receving them from the client.
- **Real-time notifications**: Notify client after required directories have been created, files has been encrypted/decrypted a long with the path to the file, and after connection termination
-  **Multithreaded Server**: Handle multiple client connections using a thread pool

# Installation
- Clone this project
- Set up the project structure
  - _resources_ (client side): directory to store files which the client will send to the server.
  - _encrypted_data_ (client side): this directory will be created once the client file is running.
  - _received_files_ (server side): directory that will store received encrypted files from the client.
  - _decrypted_data_ (server side): this directory will be created once the server file is running.
-  Dependencies
    - Java 8 or higher
    - Make sure to import _utilities.FileEncryptor_ class. This class contians the methods required for encrypted, decryption, key generation and salt value generation.

# Steps to run file transfer program
1. Complile and run the server (Need to be in the _src_ directory level to run server):
    - _javac server/Server.java_
    - _java server.Server.java_
2. Compile and run the client (Need to be in the _src_ directory level to run client):
    - _javac client/Client.java_
    - _java client.Client.java_
3. Follow the prompts in the terminal to:
    - Select a file from the _resources_ directory to encrypt (files will be listed in the terminal, the file must exist within the directory or it will not show up)
    - Enter a password within the terminal for encryption
