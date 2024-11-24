package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DistributedFileService extends Remote {
    String sendFile(String fileName, byte[] fileData, String password, byte[] salt, byte[] iv, String originalFileChecksum) throws RemoteException;

    String terminateConnection() throws RemoteException;

    String checkIntegrity(String originalChecksum, byte[] fileData) throws RemoteException;

    byte[] encryptFile(byte[] fileData, String password, byte[] salt) throws RemoteException;

    byte[] decryptFile(byte[] encryptedData, String password, byte[] salt) throws RemoteException;
}
