package server;

import rmi.DistributedFileService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) {
        try {
            DistributedFileService service = new DistributedFileServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099); // Default RMI port
            registry.rebind("DistributedFileService", service);
            System.out.println("RMI Server is running and ready for client requests...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
