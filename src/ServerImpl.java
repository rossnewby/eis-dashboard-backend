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

    private JSONObject packageJSON = null;
    private JSONObject meterJSON = null;
    private JSONObject loggerJSON = null;
    private Thread meterThread, loggerThread;

    /**
     * Initialises a server by reading basic authentication details and API data from config file.
     * Queries CKAN datastore for all metadata and returns once JSON objects have been read completely.
     * @throws RemoteException
     */
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

        /*Get CKAN metadata*/
        try {
            packageJSON = ckanRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            JSONArray packageList = packageJSON.getJSONObject("result").getJSONArray("resources"); // Array of resource names available in CKAN

            String lookingFor = "Planon metadata - Meters Sensors";
            for (int i = 0; i < packageList.length(); i++) {
                if (packageList.getJSONObject(i).getString("name").equals(lookingFor)){

                    String id = packageList.getJSONObject(i).getString("id");
                    meterThread = new Thread() {
                        public void run() {
                            try {
                                //Very large
                                meterJSON = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    meterThread.start();
                }
            }
            lookingFor = "Planon metadata - Loggers Controllers"; // Search for logger / controller metadata
            for (int i = 0; i < packageList.length(); i++) { // for every package name in CKAN
                if (packageList.getJSONObject(i).getString("name").equals(lookingFor)){ // if package is loggers / controllers

                    // Get package ID number and use this in another CKAN request for logger / controller metadata
                    String id = packageList.getJSONObject(i).getString("id");
                    loggerThread = new Thread() {
                        public void run() {
                            try {
                                loggerJSON = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    loggerThread.start();
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

        /*Join threads; threads must be complete when constructor ends*/
        try {
            meterThread.join();
            loggerThread.join();
            System.out.println("Server Initialisation Successful");
        }
        catch (Exception e){
            System.out.println("Metadata Threads Interrupted:");
            e.printStackTrace();
        }
    }

    /**
     * Submit CKAN HTTP request with automatic basic authentication and API header; specified by config file
    * @param url Desired ckan url, excluding or including 'https://'
    * @return This returns the CKAN response as a JSONObject
    * */
    public JSONObject ckanRequest(String url) throws RemoteException{
        StringBuffer response = null;
        JSONObject ret = null;

        try {
            // Make new HTTP connection
            URL newURL;
            if (url.contains("https://")){ //appends 'https://' to URL if needed
                newURL = new URL(url);
            }
            else {
                newURL = new URL("https://"+url);
            }
            System.out.println("CKAN Requesting: " + newURL.toString());
            HttpsURLConnection con = (HttpsURLConnection) newURL.openConnection();

            // Append headers to HTTP request
            String userCredentials = apiuser+":"+apipass; // use basic authentication credentials from config file
            String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
            con.setRequestProperty ("Authorization", basicAuth);
            con.setRequestProperty("X-CKAN-API-Key", apikey); // personal API key (config file)
            con.setRequestMethod("GET");
            //con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //con.setUseCaches(false);
            //con.setDoInput(true);
            //con.setDoOutput(true);

            // Read reply from HTTP request as String
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            //parse return string to a JSON object
            try {
                ret = new JSONObject(response.toString());
                System.out.println("CKAN Request Successful");
            }
            catch (Exception e){
                System.out.println("CKAN Request Error:");
                e.printStackTrace();
            }

            in.close(); //close connections
            con.disconnect();
        }
        catch (Exception e){
            System.out.println("CKAN Connection Error:");
            e.printStackTrace();
        }

        if (response == null){
            throw new RemoteException("CKAN Response 'null'");
        }
        return ret;
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
