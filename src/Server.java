import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Ross Newby
 */
public class Server {

    public Server(){

        try {
            /*Start the RMI registry on port 1099*/
            //Runtime.getRuntime().exec("rmiregistry 1099");
            //LocateRegistry.createRegistry(1099);

            /*Bind the ServerImpl class to the RMI registry for remote calls by clients*/
            ServerInterface c = new ServerImpl();
            //Naming.rebind("rmi://localhost:1099/EISQualityService", c);
        }
        catch (Exception e) {
            System.out.println("RMI Binding Error:");
            e.printStackTrace();
        }
    }

    public static void main (String args []){

        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Running...");

        Server server = new Server();
    }
}


