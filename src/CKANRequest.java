import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * Class for submitting and receiving CKAN Requests
 *
 * @Author Ross Newby
 */
public class CKANRequest {

    private URL url = null;
    private String propertiesFileName = "config.properties";
    private String apikey = null;
    private String apiuser = null;
    private String apipass = null;

    private static final int CHUNK_SIZE = 50000;
    private int counter = 0;

    /**
     *
     * @param url
     */
    public CKANRequest(String url) throws MalformedURLException{

        /*Read configuration file; populate variables*/
        try {
            Properties prop = new Properties();
            InputStream in = getClass().getClassLoader().getResourceAsStream(propertiesFileName);

            if (in != null) {
                prop.load(in);
            } else {
                throw new FileNotFoundException("'" + propertiesFileName + "' not found in classpath");
            }

            apikey = prop.getProperty("apikey");
            apiuser = prop.getProperty("apiuser");
            apipass = prop.getProperty("apipass");

            in.close();
        }
        catch (Exception e){
            System.out.println("Error Reading Configuration File:");
            e.printStackTrace();
        }

        /*appends 'https://' to URL if needed*/
        try {
            this.url = url.contains("https://") ? new URL(url) : new URL("https://" + url);
        }
        catch (Exception e){
            throw new MalformedURLException("Error Reading input URL");
        }
    }

    /**
     * Submit CKAN HTTP request with automatic basic authentication and API header; specified by config file
     * @return Returns the CKAN response as a JSONObject
     * @throws RemoteException Problem with remote procedure call
     */
    public JSONObject requestJSON() throws RemoteException{

        JSONObject ret = null;
        String response = requestString(url.toString());

        /*parse return string to a JSON object*/
        try {
            ret = new JSONObject(response);
            //System.out.println("CKAN Successful: " + url); // debug
        }
        catch (Exception e){
            System.out.println("CKAN Request Error for " + this.url);
            //e.printStackTrace();
        }

        return ret;
    }

    /**
     *
     * @param url
     * @return
     * @throws RemoteException
     */
    public JSONObject ckanRequestLong(String url) throws RemoteException {

        JSONObject ret;
        int counter = 0;

        String newURLString = url + "&offset=" + counter + "&limit=" + CHUNK_SIZE;
        String current = requestString(newURLString);

        ret = new JSONObject(current); // parse return string to a JSON object
        int total = ret.getJSONObject("result").getInt("total");
        counter =+ CHUNK_SIZE;

        do {
            newURLString = url + "&offset=" + counter + "&limit=" + CHUNK_SIZE;
            current = requestString(newURLString);

            JSONObject currentJSON = new JSONObject(current);
            JSONArray resultList = currentJSON.getJSONObject("result").getJSONArray("records");
            for (int i = 0; i < resultList.length(); i++) {
                ret.accumulate("records", resultList.getJSONObject(i));
            }
            counter =+ CHUNK_SIZE;
        }
        while (counter <= total);

        return ret;
    }

    /**
     * Submit CKAN HTTP request with automatic basic authentication and API header; specified by config file
     * @return Returns the CKAN response as String
     * @throws RemoteException Problem with remote procedure call
     */
    public String requestString(String url) throws RemoteException {

        StringBuffer response = null;
        //System.out.println("CKAN Requesting: " + url.toString()); // debug
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
        catch (Exception e) {
            System.out.print("CKAN Connection Error: ");
            if (response == null){ // no response from CKAN with specified URL
                //throw new RemoteException("CKAN Response 'null'");
                System.out.println("CKAN Response 'null'");
                return "";
            }
            //e.printStackTrace();
        }

        return response.toString(); // returns StringBuffer as String
    }
}
