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
import org.json.*;
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
            //String jsonString = ckanRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            String jsonString = "{\"help\": \"https://ckan.lancaster.ac.uk/api/3/action/help_show?name=package_show\", \"success\": true, \"result\": {\"license_title\": \"Creative Commons Attribution\", \"maintainer\": \"\", \"relationships_as_object\": [], \"private\": false, \"maintainer_email\": \"\", \"num_tags\": 1, \"id\": \"8f150a81-22ad-4e93-b232-83b595b3e9df\", \"metadata_created\": \"2016-12-23T10:50:47.174237\", \"metadata_modified\": \"2017-08-24T01:00:08.185653\", \"author\": \"\", \"author_email\": \"\", \"state\": \"active\", \"version\": \"\", \"creator_user_id\": \"a69d04b7-c249-4c4a-9f0a-bf9f8d2b36ac\", \"type\": \"dataset\", \"resources\": [{\"cache_last_updated\": null, \"package_id\": \"8f150a81-22ad-4e93-b232-83b595b3e9df\", \"webstore_last_updated\": null, \"datastore_active\": false, \"id\": \"dcdb9e82-f89e-4a1d-8c58-a7197da2d648\", \"size\": null, \"state\": \"active\", \"hash\": \"\", \"description\": \"\", \"format\": \"CSV\", \"last_modified\": \"2017-08-24T01:00:07.481598\", \"url_type\": \"upload\", \"mimetype\": null, \"cache_url\": null, \"name\": \"planonassets\", \"created\": \"2016-12-23T10:50:48.554551\", \"url\": \"https://ckan.lancaster.ac.uk/dataset/8f150a81-22ad-4e93-b232-83b595b3e9df/resource/dcdb9e82-f89e-4a1d-8c58-a7197da2d648/download/planonassets.csv\", \"webstore_url\": null, \"mimetype_inner\": null, \"position\": 0, \"revision_id\": \"05e2c013-57cd-4db8-b822-16ed91162e57\", \"resource_type\": null}, {\"cache_last_updated\": null, \"package_id\": \"8f150a81-22ad-4e93-b232-83b595b3e9df\", \"webstore_last_updated\": null, \"datastore_active\": false, \"id\": \"767f14d6-0849-4349-806f-e48c4b563a6d\", \"size\": null, \"state\": \"active\", \"hash\": \"\", \"description\": \"\", \"format\": \"CSV\", \"last_modified\": \"2017-08-24T01:00:07.753165\", \"url_type\": \"upload\", \"mimetype\": null, \"cache_url\": null, \"name\": \"planonbuildings\", \"created\": \"2016-12-23T10:50:48.859290\", \"url\": \"https://ckan.lancaster.ac.uk/dataset/8f150a81-22ad-4e93-b232-83b595b3e9df/resource/767f14d6-0849-4349-806f-e48c4b563a6d/download/planonbuildings.csv\", \"webstore_url\": null, \"mimetype_inner\": null, \"position\": 1, \"revision_id\": \"6f3d9ea6-71d8-4563-999b-bf7d57757846\", \"resource_type\": null}, {\"cache_last_updated\": null, \"package_id\": \"8f150a81-22ad-4e93-b232-83b595b3e9df\", \"webstore_last_updated\": null, \"datastore_active\": false, \"id\": \"d970fb38-8566-4ed5-987e-9bad585f406d\", \"size\": null, \"state\": \"active\", \"hash\": \"\", \"description\": \"Lists all the different spaces that are available in PlanOn. This includes all bookable spaces on campus.\", \"format\": \"CSV\", \"last_modified\": \"2017-08-24T01:00:08.164403\", \"url_type\": \"upload\", \"mimetype\": null, \"cache_url\": null, \"name\": \"planonspaces\", \"created\": \"2016-12-23T10:50:49.364651\", \"url\": \"https://ckan.lancaster.ac.uk/dataset/8f150a81-22ad-4e93-b232-83b595b3e9df/resource/d970fb38-8566-4ed5-987e-9bad585f406d/download/planonspaces.csv\", \"webstore_url\": null, \"mimetype_inner\": null, \"position\": 2, \"revision_id\": \"6f3d9ea6-71d8-4563-999b-bf7d57757846\", \"resource_type\": null}, {\"cache_last_updated\": null, \"package_id\": \"8f150a81-22ad-4e93-b232-83b595b3e9df\", \"webstore_last_updated\": null, \"datastore_active\": true, \"id\": \"2b347b1c-8843-4846-be31-ae8cf4609c28\", \"size\": null, \"state\": \"active\", \"hash\": \"\", \"description\": \"\", \"format\": \"CSV\", \"last_modified\": \"2017-08-18T10:02:48.508189\", \"url_type\": \"upload\", \"mimetype\": null, \"cache_url\": null, \"name\": \"Planon metadata - Meters Sensors\", \"created\": \"2017-08-18T11:02:48.538621\", \"url\": \"https://ckan.lancaster.ac.uk/dataset/8f150a81-22ad-4e93-b232-83b595b3e9df/resource/2b347b1c-8843-4846-be31-ae8cf4609c28/download/lancshomes17bastiaanmy-documentsenergy-monitoringenergy-information-systemplanon-meta-dataplanon.csv\", \"webstore_url\": null, \"mimetype_inner\": null, \"position\": 3, \"revision_id\": \"7ba3353e-0055-4752-aefb-b2a4f2b541c2\", \"resource_type\": null}, {\"cache_last_updated\": null, \"package_id\": \"8f150a81-22ad-4e93-b232-83b595b3e9df\", \"webstore_last_updated\": null, \"datastore_active\": true, \"id\": \"8ac304bb-680f-4ef9-8d0a-68f07f60a62e\", \"size\": null, \"state\": \"active\", \"hash\": \"\", \"description\": \"\", \"format\": \"CSV\", \"last_modified\": \"2017-08-18T10:03:29.243241\", \"url_type\": \"upload\", \"mimetype\": null, \"cache_url\": null, \"name\": \"Planon metadata - Loggers Controllers\", \"created\": \"2017-08-18T11:03:29.271551\", \"url\": \"https://ckan.lancaster.ac.uk/dataset/8f150a81-22ad-4e93-b232-83b595b3e9df/resource/8ac304bb-680f-4ef9-8d0a-68f07f60a62e/download/lancshomes17bastiaanmy-documentsenergy-monitoringenergy-information-systemplanon-meta-dataplanon.csv\", \"webstore_url\": null, \"mimetype_inner\": null, \"position\": 4, \"revision_id\": \"beb899d5-f77b-4478-979d-2ea0ab420c8b\", \"resource_type\": null}], \"building_code\": [\"AP000\"], \"num_resources\": 5, \"groups\": [], \"license_id\": \"cc-by\", \"relationships_as_subject\": [], \"organization\": {\"description\": \"\", \"created\": \"2016-10-25T13:51:25.564544\", \"title\": \"Lancaster University\", \"name\": \"lancaster-university\", \"is_organization\": true, \"state\": \"active\", \"image_url\": \"\", \"revision_id\": \"a3b50a6a-f481-4152-9cb4-2c7981e1d3e6\", \"type\": \"organization\", \"id\": \"2ecc6566-7bca-42cc-827a-b043a600ee85\", \"approval_status\": \"approved\"}, \"name\": \"planonmetadata\", \"isopen\": true, \"url\": \"\", \"notes\": \"Planon is the system that is used to map all buildings and spaces on campus.\", \"owner_org\": \"2ecc6566-7bca-42cc-827a-b043a600ee85\", \"extras\": [], \"license_url\": \"http://www.opendefinition.org/licenses/cc-by\", \"title\": \"PlanonMetaData\", \"revision_id\": \"ce10384c-215f-46f8-9acd-2a6b868e359c\"}}";

            JSONObject obj = new JSONObject(jsonString);
            JSONArray arr = obj.getJSONObject("result").getJSONArray("resources");

            String lookingFor = "Planon metadata - Meters Sensors";
            String meterMetadata = null;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("name").equals(lookingFor)){
                    String name = arr.getJSONObject(i).getString("name");
                    String id = arr.getJSONObject(i).getString("id");
                    System.out.println(name + " = " + id);
                    // meterMetadata = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT * FROM \""+id+"\"");
                }
            }
            lookingFor = "Planon metadata - Loggers Controllers";
            String loggerMetadata = null;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("name").equals(lookingFor)){
                    String name = arr.getJSONObject(i).getString("name");
                    String id = arr.getJSONObject(i).getString("id");
                    System.out.println(name + " = " + id);
                    // loggerMetadata = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT * FROM \""+id+"\"");
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
        System.out.println("Requesting CKAN URL: " + url);

        try {
            // http://www.baeldung.com/java-http-request
            URL newURL = new URL("https://"+apiuser+":"+apipass+"@"+url); // append basic authentication credentials and URL
            HttpsURLConnection con = (HttpsURLConnection) newURL.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("X-CKAN-API-Key", apikey); //API key as request header
            //con.setRequestProperty("Cookie", cookiename+"="+cookie);

            // TODO: Problem with security certificate needs fixing!
//            BufferedReader in = new BufferedReader(
//                    new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            response = new StringBuffer();
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
}
