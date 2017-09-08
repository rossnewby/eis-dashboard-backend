import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * A Class for submitting CKAN HTTP Requests; A config.properties file must be specified, containing a CKAN API key ('apikey'), as well as
 * a username ('apiuser') and password ('apipass') for basic authentication.
 * @Author Ross Newby
 */
public class CKANRequest {

    private URL url = null;
    private static final String PROPERTIES_FILENAME = "config.properties";
    private String apikey = null;
    private String apiuser = null;
    private String apipass = null;

    /**
     * Initialise a CKAN request for a specified URL
     * @param url The URL address of the CKAN request; works with or without 'https://'
     * @throws MalformedURLException The specified CKAN URL was not valid
     * @throws FileNotFoundException A file names config.properties was not found in the class path
     */
    public CKANRequest(String url) throws MalformedURLException, FileNotFoundException{

        /*Read configuration file; populate variables*/
        try {
            Properties prop = new Properties();
            InputStream in = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILENAME);

            if (in != null) {
                prop.load(in);
            } else {
                throw new FileNotFoundException("'" + PROPERTIES_FILENAME + "' not found in classpath");
            }

            apikey = prop.getProperty("apikey");
            apiuser = prop.getProperty("apiuser");
            apipass = prop.getProperty("apipass");

            in.close();
        }
        catch (IOException e){
            // System.out.println("Error Reading Configuration File: "+ PROPERTIES_FILENAME);
        }

        /*appends 'https://' to URL if needed*/
        try {
            this.url = url.contains("https://") ? new URL(url) : new URL("https://" + url);
        }
        catch (Exception e){
            throw new MalformedURLException(url +" is not a valid URL string");
        }
    }

    /**
     * Submit the CKAN HTTP request and automatically parse the return statement to JSON format
     * @return The CKAN response as a JSONObject
     * @throws IOException When CKAN connection could not be established
     */
    public JSONObject requestJSON() throws IOException{

        String response = requestString();
        JSONObject ret = new JSONObject(response);
        return ret;
    }

    /**
     * Submit the CKAN HTTP request
     * @return The CKAN response as  String
     * @throws IOException When CKAN connection could not be established
     */
    public String requestString() throws IOException{

        return requestString(this.url.toString());
    }

    /**
     * Facilitates public methods by submitting a CKAN HTTP request with automatic basic authentication
     * and API header specified by config file
     * @return Returns the CKAN response as String
     * @throws IOException When CKAN connection could not be established
     */
    private String requestString(String url) throws IOException{

        StringBuffer response;
        try {
            URL newURL = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) newURL.openConnection();

            /*Append headers to HTTP request*/
            String userCredentials = apiuser + ":" + apipass; // use basic authentication credentials from config file
            String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
            con.setRequestProperty("Authorization", basicAuth);
            con.setRequestProperty("X-CKAN-API-Key", apikey); // personal API key (config file)
            con.setRequestMethod("GET");

            /*Read reply from HTTP request as String*/
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close(); //close connections
            con.disconnect();
        }
        catch (MalformedURLException e){
            System.out.print("Could not read: "+ this.url);
            return "";
        }
        return response.toString(); // returns StringBuffer as String
    }
}
