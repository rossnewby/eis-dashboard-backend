/**
 * Contains the methods which can be remotely invoked
 *
 * @Author Ross Newby
 */
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class ServerImpl extends UnicastRemoteObject implements ServerInterface{

    private final String USER_AGENT = "Mozilla/5.0";

    private String cookie = "cosign-https-ckan.lancaster.ac.uk";
    private String cookiename = "VdV48bc+gJh8i4NRtvZx95OTVkyGCtoaF-TSaIOm7lz-9v3kxvvlqodDKiUKwvaNpMiuLWkgerUGySpMETKd6SqtyU9bKpcZ6pS5bydnVKAcgdUsHWlDA1dWNJ2l/1503307766";

    public ServerImpl() throws RemoteException{

        try {
            // http://www.baeldung.com/java-http-request
            URL url = new URL("https://ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Cookie", cookiename+"="+cookie);

            // Problem with security Certificate needs fixing
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
            System.out.println("CKAN Connection Error: " + e);
            // e.printStackTrace();
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
