import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class GroupServer  {
    public static void main(String[] args) throws RemoteException, MalformedURLException {
        LocateRegistry.createRegistry(1099);
        Communicate comm = new CommunicateImpl();
        Naming.rebind("server.comm", comm);
        System.out.println("You have been served");
    }
}