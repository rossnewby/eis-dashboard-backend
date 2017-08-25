/**
 * Contains the methods which can be remotely invoked
 *
 * @Author Ross Newby
 */
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.json.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class ServerImpl extends UnicastRemoteObject implements ServerInterface{

    private String cookie = null;
    private String cookiename = null;
    private String apikey = null;
    private String apiuser = null;
    private String apipass = null;

    private String meterMetadata = null;
    private String loggerMetadata = null;

    public ServerImpl() throws RemoteException{

        /*Read configuration file; populate variables*/
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

        /*Get CKAN metadata and process*/
        try {
            String ckanPackages = ckanRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            JSONObject obj = new JSONObject(ckanPackages);
            JSONArray arr = obj.getJSONObject("result").getJSONArray("resources"); // Array of resource names available in CKAN

            String lookingFor = "Planon metadata - Meters Sensors";
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("name").equals(lookingFor)){
                    String name = arr.getJSONObject(i).getString("name");
                    String id = arr.getJSONObject(i).getString("id");
                    System.out.println(name + " = " + id);
                    (new Thread() {
                        public void run() {
                            try {
                                //Very large
                                //meterMetadata = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
            lookingFor = "Planon metadata - Loggers Controllers"; // Search for logger / controller metadata
            for (int i = 0; i < arr.length(); i++) { // for every package name in CKAN
                if (arr.getJSONObject(i).getString("name").equals(lookingFor)){ // if package is loggers / controllers

                    // Get package ID number and use this in another CKAN request for logger / controller metadata
                    String id = arr.getJSONObject(i).getString("id");
                    (new Thread() {
                        public void run() {
                            try {
                                loggerMetadata = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        }
        catch (NullPointerException e){
            System.out.println("Error Processing JSON String: CKAN Response 'null'");
            e.printStackTrace();
        }
        catch (Exception e){
            System.out.println("Error Processing JSON String:");
            e.printStackTrace();
        }
    }

    /**
     * Submit CKAN HTTP request with automatic basic authentication
    * @param url Desired ckan url, excluding 'https://'
    * @return This returns the CKAN response as String
    * */
    public String ckanRequest(String url) throws RemoteException{
        StringBuffer response = null;

        try {
            // http://www.baeldung.com/java-http-request
            URL newURL = new URL("https://"+apiuser+":"+apipass+"@"+url); // append basic authentication credentials and URL
            System.out.println("CKAN Requesting: " + newURL.toString());
            HttpsURLConnection con = (HttpsURLConnection) newURL.openConnection();

            String userCredentials = apiuser+":"+apipass;
            String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
            con.setRequestProperty ("Authorization", basicAuth);
            con.setRequestProperty("X-CKAN-API-Key", apikey); //API key as request header
            con.setRequestMethod("GET");
            //con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //con.setUseCaches(false);
            //con.setDoInput(true);
            //con.setDoOutput(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            System.out.println("CKAN Response: " + response.toString()); //print result
            in.close();

            // con.disconnect();
        }
        catch (Exception e){
            System.out.println("CKAN Connection Error:");
            e.printStackTrace();
        }

        if (response == null){
            throw new RemoteException("CKAN Response 'null'");
        }
        return response.toString();
    }

    //
    public int method2 () throws RemoteException{
        return 1;
    }

    //
    public int method3 () throws RemoteException{
        return 1;
    }

    // Simple file to text for testing ckan responses
    // https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
