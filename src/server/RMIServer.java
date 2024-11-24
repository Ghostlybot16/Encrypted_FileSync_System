package server;

import rmi.DistributedFileService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) {
        try {
            DistributedFileService service = new DistributedFileServiceImpl();

            // Create an RMI registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Bind the service to the registry
            registry.rebind("DistributedFileService", service);

            System.out.println("RMI Server is running and ready for client requests...");

            // Notify connection logs
            System.out.println("Waiting for clients to connect");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
