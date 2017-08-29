import java.io.*;
import org.json.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Contains the methods for quality assurance processing
 *
 * @Author Ross Newby
 */
public class ServerImpl extends UnicastRemoteObject implements ServerInterface{

    private JSONObject packageJSON = null; // JSON objects for all metadata
    private JSONObject meterJSON = null;
    private JSONObject loggerJSON = null;
    private Thread meterThread, loggerThread; // threads for initial metadata reading
    private JSONObject readJSON;

    private Database database = null;
    private static final String METER_SENSOR_METADATA_NAME = "Planon metadata - Meters Sensors";
    private static final String LOGGER_CONTROLLER_METADATA_NAME = "Planon metadata - Loggers Controllers";
    private static final String BMS_CLASSIFICATION_GROUP = "Energy meter";
    private static final String EMS_CLASSIFICATION_GROUP = "Energy sensor";


    private Scanner scanner = new Scanner(System.in);
    private String input = null;

    /**
     * Initialises a server by reading basic authentication details and API data from config file.
     * Queries CKAN datastore for all metadata and returns once JSON objects have been read completely.
     * @throws RemoteException Problem during remote procedure call
     * @throws FileNotFoundException If config.properties file not found in root directory
     */
    public ServerImpl() throws RemoteException{

        System.out.println("Initialising Server...");

        /*Get CKAN metadata*/
        try {
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            packageJSON = ckanReq.ckanRequest();
            JSONArray packageList = packageJSON.getJSONObject("result").getJSONArray("resources"); // Array of resource names available in CKAN

            String lookingFor = METER_SENSOR_METADATA_NAME; // search for meter / sensor metadata
            for (int i = 0; i < packageList.length(); i++) { // for every package name in CKAN
                if (packageList.getJSONObject(i).getString("name").equals(lookingFor)){ // if package is meter / sensor data
                    // Get package ID number and use this in another CKAN request for meter / sensor metadata
                    String id = packageList.getJSONObject(i).getString("id");
                    meterThread = new Thread() {
                        public void run() {
                            try {
                                CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                                meterJSON = ckanReq.ckanRequest();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    meterThread.start();
                }
            }
            lookingFor = LOGGER_CONTROLLER_METADATA_NAME; // Search for logger / controller metadata
            for (int i = 0; i < packageList.length(); i++) { // for every package name in CKAN
                if (packageList.getJSONObject(i).getString("name").equals(lookingFor)){ // if package is loggers / controllers
                    // Get package ID number and use this in another CKAN request for logger / controller metadata
                    String id = packageList.getJSONObject(i).getString("id");
                    loggerThread = new Thread() {
                        public void run() {
                            try {
                                CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                                loggerJSON = ckanReq.ckanRequest();
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
        }
        catch (Exception e){
            System.out.println("Metadata Threads Interrupted:");
            e.printStackTrace();
        }

        /*Connect to Database*/
        database = new Database();
        database.printTable("errors"); //debug

        System.out.println("Server Initialised"); // confirmation message
        serverMenu();
    }

    /**
     *
     */
    private void serverMenu(){

        while (!"4".equals(input)) {
            INVALID:
            {
                System.out.println("-- Actions --");
                System.out.println("Select an option:");
                System.out.println("  1) Print Database");
                System.out.println("  2) Read & Process BMS");
                System.out.println("  3) Process Meter");
                System.out.println("  9) Disable Actions");

                input = scanner.nextLine();

                // Print Database
                if ("1".equals(input)){
                    database.printTable("errors");
                }
                // Read CKAN Files
                else if ("2".equals(input)){
                    // TESTING ONLY
                    processFile();
                }
                // <>
                else if ("3".equals(input)){
                    // TESTING ONLY
                    processMeter("{05937EE0-58E6-42F3-B6BD-A180D9634B6C}", "D1");
                }
                else if ("9".equals(input)){
                    System.out.println("Actions no longer available");
                }
                // Exit
                else if ("e".equals(input)) {
                    exit();
                }
                // Invalid Input
                else {
                    System.out.println("Invalid Option.");
                }
            }
        }
    }

    public void processFile(){

        try {
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=bms");
            JSONObject bmsJSON = ckanReq.ckanRequest();
            JSONArray bmsList = bmsJSON.getJSONObject("result").getJSONArray("resources"); // Array of bms fines in CKAN

            String lookingFor = "bms-jun-2017";
            for (int i = 0; i < bmsList.length(); i++) {

                if (bmsList.getJSONObject(i).getString("name").equals(lookingFor)){

                    String id = bmsList.getJSONObject(i).getString("id");
                    new Thread() {
                        public void run() {
                            try {
                                //CKANRequest ckanReq = new CKANRequest("https://ckan.lancaster.ac.uk/api/action/datastore_search?resource_id="+id+"&offset="+0+"&limit="+50000);
                                //readJSON = ckanRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                                //readJSON = ckanReq.ckanRequestLong("https://ckan.lancaster.ac.uk/api/action/datastore_search?resource_id="+id);
                                CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"%20WHERE%20device_id='{05937EE0-58E6-42F3-B6BD-A180D9634B6C}'%20AND%20module_key='D1'");
                                readJSON = ckanReq.ckanRequest();
                                System.out.println("Back to Server Class");
                                writeFile(readJSON, lookingFor);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        }
        catch (Exception e){
            System.out.println("Error Processing BMS:");
            e.printStackTrace();
        }
    }

    public void processMeter(String loggerCode, String moduleKey){

        ArrayList<String> outputList = new ArrayList<String>();

        JSONArray loggerList = meterJSON.getJSONObject("result").getJSONArray("records");
        for (int i = 0; i < loggerList.length(); i++) { // loop through every meter
            String code = loggerList.getJSONObject(i).getString("Logger Asset Code");
            String channel = loggerList.getJSONObject(i).getString("Logger Channel");
            if (loggerCode.equals(code) && moduleKey.equals(channel)){
                System.out.println(code + " - " + channel);
            }
        }

        /*Convert ArrayList to String array*/
        String[] outputArray = new String[outputList.size()];
        outputList.toArray(outputArray);

    }

    /**
     * A list of building names available in the CKAN metadata
     * @return Array of building names
     * @throws RemoteException Problem with remote procedure call
     */
    public String[] getBuildingList() throws RemoteException{
        ArrayList<String> outputList = new ArrayList<String>();

        JSONArray loggerList = loggerJSON.getJSONObject("result").getJSONArray("records"); //list of loggers / controllers
        for (int i = 0; i < loggerList.length(); i++) { // loop through every logger
            String name = loggerList.getJSONObject(i).getString("Building Name");
            if (!outputList.contains(name)){
                outputList.add(name); // add building name of logger to return list, if it hasnt been seen already
            }
        }

        /*Convert ArrayList to String array*/
        String[] outputArray = new String[outputList.size()];
        outputList.toArray(outputArray);
        return outputArray;
    }

    /**
     * <></>
     * @param JSONObj
     * @param toFind
     * @return
     * @throws RemoteException
     */
    public String[] getSpecified(JSONObject JSONObj, String toFind) throws RemoteException{
        ArrayList<String> outputList = new ArrayList<String>();

        JSONArray loggerList = JSONObj.getJSONObject("result").getJSONArray("records"); //list of loggers / controllers
        for (int i = 0; i < loggerList.length(); i++) { // loop through every logger
            String name = loggerList.getJSONObject(i).getString(toFind);
            if (!outputList.contains(name)){
                outputList.add(name); // add building name of logger to return list, if it hasnt been seen already
            }
        }

        /*Convert ArrayList to String array*/
        String[] outputArray = new String[outputList.size()];
        outputArray = outputList.toArray(outputArray);
        return outputArray;
    }

    // Simple file to text method for testing ckan responses
    // https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    void writeFile(JSONObject obj, String name) throws IOException{
        String fileName = name+".txt";
        try (FileWriter file = new FileWriter(fileName)) {
            file.write(obj.toString());
            System.out.println("Successfully Copied JSON Object to File: " + fileName);
        }
    }

    /**
     * Terminates application
     */
    private void exit(){
        System.out.println("Exiting...");
        System.exit(1);
    }
}
