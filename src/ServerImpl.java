/**
 * Contains the methods which can be remotely invoked
 *
 * @Author Ross Newby
 */
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class ServerImpl extends UnicastRemoteObject implements ServerInterface{

    private String cookie = null;
    private String cookiename = null;
    private String apikey = null;
    private String apiuser = null;
    private String apipass = null;

    public ServerImpl() throws RemoteException{

        /*Read config file*/
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("'" + propFileName + "' not found in classpath");
            }

            cookie = prop.getProperty("cookie");
            cookiename = prop.getProperty("cookiename");
            apikey = prop.getProperty("apikey");
            apiuser = prop.getProperty("apiuser");
            apipass = prop.getProperty("apipass");
        }
        catch (Exception e){
            System.out.println("Error Reading Configuration File:");
            e.printStackTrace();
        }

        try {
            // http://www.baeldung.com/java-http-request
            URL url = new URL("https://"+apiuser+":"+apipass+"@ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            //con.setRequestProperty("Cookie", cookiename+"="+cookie);
            con.setRequestProperty("X-CKAN-API-Key", apikey);

            // TODO: Problem with security certificate needs fixing!
//            BufferedReader in = new BufferedReader(
//                    new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            StringBuffer response = new StringBuffer();
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            System.out.println(response.toString()); //print result
//            in.close();

            con.disconnect();
        }
        catch (Exception e){
            System.out.println("CKAN Connection Error:");
            e.printStackTrace();
        }
    }

    //
    public int method () throws RemoteException{
        return 1;
    }

    //
    public int method2 () throws RemoteException{
        return 1;
    }

    //
    public int method3 () throws RemoteException{
        return 1;
    }
}
