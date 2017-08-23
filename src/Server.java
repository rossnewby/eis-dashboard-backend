import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * @Author Ross Newby
 */
public class Server {

    public Server(){

        /*Bind the remote object to the RMI registry*/
        try {
            /*Start the RMI registry on port 1099*/
            Runtime.getRuntime().exec("rmiregistry 1099");
            LocateRegistry.createRegistry(1099);

            /*Bind the ServerImpl class to the RMI registry for remote calls by clients*/
            ServerInterface c = new ServerImpl();
            Naming.rebind("rmi://localhost:1099/EISQualityService", c);
        }
        catch (Exception e) {
            System.out.println("RMI Binding Error: " + e);
            // e.printStackTrace();
        }
    }

    public static void main (String args []){

        System.out.println("Running...");

        System.out.println(System.getProperty("java.vendor"));
        System.out.println(System.getProperty("java.vendor.url"));
        System.out.println(System.getProperty("java.version"));

        new Server();
    }
}


