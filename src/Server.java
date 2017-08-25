import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Date;
import java.util.Properties;

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
            System.out.println("RMI Binding Error:");
            e.printStackTrace();
        }
    }

    public static void main (String args []){

        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Running...");

//        String url = "https://ckanapi:Quee8eec@ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata";
//        try {
//            System.out.println(getHTML(url));
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }

        new Server();
    }

    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }
}


