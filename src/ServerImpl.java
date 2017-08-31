import java.io.*;
import org.json.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Contains the methods for quality assurance processing
 *
 * @Author Ross Newby
 */
public class ServerImpl extends UnicastRemoteObject implements ServerInterface {

    private JSONObject packageJSON = null; // JSON objects for all metadata
    private JSONObject meterJSON = null;
    private JSONObject loggerJSON = null;
    private Thread meterThread, loggerThread; // threads for initial metadata reading
    private JSONObject readingJSON = null; // JSON for current CKAN request

    private static final String METER_SENSOR_METADATA_NAME = "Planon metadata - Meters Sensors"; // names of metadata files in CKAN
    private static final String LOGGER_CONTROLLER_METADATA_NAME = "Planon metadata - Loggers Controllers";
    private static final String BMS_CLASSIFICATION_GROUP = "Energy meter"; // identifier for EMS records
    private static final String EMS_CLASSIFICATION_GROUP = "Energy sensor"; // identifier for BMS records

    private Database database = null;
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
            packageJSON = ckanReq.requestJSON();
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
                                meterJSON = ckanReq.requestJSON();
                                //meterCodes = getSpecified(meterJSON, "Logger Asset Code");
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
                                loggerJSON = ckanReq.requestJSON();
                                //loggerCodes = getSpecified(loggerJSON, "Logger Serial Number");
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
        try {
            database.executeSQLScript("C:/Users/Ross/Documents/University/Intern - EIS Dashboard/SQL Scripts/eisqualityinit.sql");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        database.printDatabase(); //debug

        System.out.println("Server Initialised"); // confirmation message
        serverMenu();
    }

    /**
     *
     */
    private void serverMenu(){

        while (!"9".equals(input)) {
            INVALID:
            {
                System.out.println("-- Actions --");
                System.out.println("Select an Action:\n" +
                                    "  1) Print Database\n" +
                                    "  2) Test Metadata Quality\n" +
                                    "  3) Test BMS Data Quality* (print loggerCodes)\n" +
                                    "  4) Test EMS Data Quality* (print meterCodes)\n" +
                                    "  5) Test Specific Meter\n" +
                                    "  9) Disable Actions");
                input = scanner.nextLine();

                if ("1".equals(input)){ // Print Database
                    database.printDatabase();
                }
                else if ("2".equals(input)){ // Test Metadata for errors
                    testMetadata();
                }
                else if ("3".equals(input)){
//                    for (int i = 0; i < loggerCodes.length; i++)
//                        System.out.println(loggerCodes[i]);
                }
                else if ("4".equals(input)){
                    // TESTING ONLY
                    // processFile();
//                    for (int i = 0; i < meterCodes.length; i++)
//                        System.out.println(meterCodes[i]);
                }
                else if ("5".equals(input)){
                    //processFile();
                    processMeter("{05937EE0-58E6-42F3-B6BD-A180D9634B6C}", "D1"); //testing only
                }
                else if ("9".equals(input)){
                    System.out.println("Actions no longer available");
                }
                else if ("e".equals(input)) { // Close application
                    exit();
                }
                else { // Invalid Input
                    System.out.println("Invalid Option.");
                }
            }
        }
    }

    public void testMetadata() {
        System.out.println("Processing Metadata..."); //debug
        String[] loggerCodes, meterCodes;
        String[] loggerDescs, meterDescs;

        /*All logger codes in logger metadata*/
        ArrayList<String> loggerCodeArray = new ArrayList<>();
        ArrayList<String> loggerDescArray = new ArrayList<>();
        JSONArray loggerList = loggerJSON.getJSONObject("result").getJSONArray("records"); //list of loggers
        for (int i = 0; i < loggerList.length(); i++) { // every logger
            String name = loggerList.getJSONObject(i).getString("Logger Serial Number"); //store logger code
            loggerCodeArray.add(name);
            String desc = loggerList.getJSONObject(i).getString("Description"); //store description
            loggerDescArray.add(desc);
        }
        loggerCodes = convertToArray(loggerCodeArray); // convert to regular String arrays
        loggerDescs = convertToArray(loggerDescArray);

        /*All logger codes in meter metadata*/
        ArrayList<String> meterArray = new ArrayList<>();
        ArrayList<String> meterDescArray = new ArrayList<>();
        JSONArray meterList = meterJSON.getJSONObject("result").getJSONArray("records"); //list of meters
        for (int i = 0; i < meterList.length(); i++) { // every meter
            String name = meterList.getJSONObject(i).getString("Logger Asset Code"); // store logger code
            meterArray.add(name);
            String desc = meterList.getJSONObject(i).getString("Description"); //store description
            meterDescArray.add(desc);
        }
        meterCodes = convertToArray(meterArray); // convert to regular String arrays
        meterDescs = convertToArray(meterDescArray);

        System.out.println("Loggers Codes: " + loggerCodes.length + ", Meter Codes: " + meterCodes.length); //debug
        int errors = 0;

        /*Test for loggers without meters*/
        for (int i = 0; i < loggerCodes.length; i++){ // for every logger
            boolean found = false;
            for (int j = 0; j < meterCodes.length; j++){ // look at every meter
                if (loggerCodes[i].equals(meterCodes[j])) {
                    found = true;
                    break; // break if meter matching logger is found
                }
            }
            if (!found){
                errors++;
                Map<String, String> vals = new HashMap<>();
                vals.put("error_type", "\"logger no meter\"");
                vals.put("asset_code", "\""+loggerCodes[i]+"\"");
                vals.put("description", "\""+loggerDescs[i]+"\"");
                database.addRecord("metadataerrors", vals); // Add error to database
            }
        }
        System.out.println("Loggers without Meters: " + errors);

        errors = 0;
        /*Test for meters without loggers*/
        for (int i = 0; i < meterCodes.length; i++){ // for every meter
            boolean found = false;
            for (int j = 0; j < loggerCodes.length; j++){ // look at every logger
                if (meterCodes[i].equals(loggerCodes[j])) {
                    found = true;
                    break; // break if logger matching meter is found
                }
            }
            if (!found){
                errors++;
                Map<String, String> vals = new HashMap<>();
                vals.put("error_type", "\"meter no loggers\"");
                vals.put("asset_code", "\""+meterCodes[i]+"\"");
                vals.put("description", "\""+meterDescs[i]+"\""); //TODO Improve descriptions (account for blanks and expand desc)
                database.addRecord("metadataerrors", vals); // Add error to database
            }
        }
        System.out.println("Meters without Loggers: " + errors);
        System.out.println("Metadata Processing Successful");
    }

    public void processFile(){

        try {
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=bms");
            JSONObject bmsJSON = ckanReq.requestJSON();
            JSONArray bmsList = bmsJSON.getJSONObject("result").getJSONArray("resources"); // Array of bms fines in CKAN

            String lookingFor = "bms-jun-2017";
            for (int i = 0; i < bmsList.length(); i++) {

                if (bmsList.getJSONObject(i).getString("name").equals(lookingFor)){

                    String id = bmsList.getJSONObject(i).getString("id");
                    new Thread() {
                        public void run() {
                            try {
                                //CKANRequest ckanReq = new CKANRequest("https://ckan.lancaster.ac.uk/api/action/datastore_search?resource_id="+id+"&offset="+0+"&limit="+50000);
                                //readJSON = requestJSON("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                                //readJSON = ckanReq.ckanRequestLong("https://ckan.lancaster.ac.uk/api/action/datastore_search?resource_id="+id);
                                CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"%20WHERE%20device_id='{05937EE0-58E6-42F3-B6BD-A180D9634B6C}'%20AND%20module_key='D1'");
                                readingJSON = ckanReq.requestJSON();
                                System.out.println("Back to Server Class");
                                writeFile(readingJSON, lookingFor);
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

    /**
     * Convert all data for a single meter / sensor to a single JSON object.
     * Both the logger code and module key make a unique identifier for the Meter
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key
     */
    public void processMeter(String loggerCode, String moduleKey){
        try {
            /*List of BMS files in CKAN*/
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=bms");
            JSONObject bmsJSON = ckanReq.requestJSON();
            JSONArray bmsList = bmsJSON.getJSONObject("result").getJSONArray("resources"); // Array of bms files in CKAN (JSON Objects)
            ArrayList<String> fileList = new ArrayList<>(); // list of BMS filenames in CKAN
            for (int i = 0; i < bmsList.length(); i++){ // each BMS file
                String fileName = bmsList.getJSONObject(i).getString("name"); // next BMS filename
                if (!fileName.equals("bmsdevicemeta")) {
                    if (!fileName.equals("bmsmodulemeta")) {// don't include bms metadata in list
                        fileList.add(bmsList.getJSONObject(i).getString("id"));
                        System.out.println("Added: " + fileName + ", " + bmsList.getJSONObject(i).getString("id")); //debug
                    }
                }
            }

            /*Read JSON object for first file and remove reference from list*/
            ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" +
                                        fileList.get(0) + "\"%20WHERE%20device_id='"+loggerCode+"'%20AND%20module_key='"+moduleKey+"'");
            readingJSON = ckanReq.requestJSON();
            fileList.remove(0);

            String[] bmsFileIDs = new String[fileList.size()]; // convert list of filenames to regular String[] array
            fileList.toArray(bmsFileIDs);

            /*Get data for meter from every bms file*/
            ExecutorService es = Executors.newCachedThreadPool();
            for (String id: bmsFileIDs) { // for every bms file
                es.execute(new Thread() { // execute code on new thread
                    public void run() {
                        try {
                            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\""
                                                                    + id + "\"%20WHERE%20device_id='"+loggerCode+"'%20AND%20module_key='"+moduleKey+"'");
                            JSONObject newJSON = ckanReq.requestJSON(); // JSON object of meter data from CKAN
                            JSONArray toAccumulate = newJSON.getJSONObject("result").getJSONArray("records");
                            readingJSON.accumulate("records", toAccumulate); // append meter data to current JSONObject
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            /*Wait for all thread to end*/
            System.out.println("Waiting on Threads for " + loggerCode + "...");
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            System.out.println("All Threads Complete for " + loggerCode + "-" + moduleKey);

            //For now; output result to file for debug
            writeFile(readingJSON, loggerCode+"-"+moduleKey);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * A list of building names available in the CKAN metadata
     * @return Array of building names
     */
    public String[] getBuildingList() {
        ArrayList<String> outputList = new ArrayList<>();

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
     * A set of elements from a specified JSON attribute; duplicate elements are ignores
     * @param JSONObj JSON object to search records
     * @param toFind Element name to find
     * @return List of unique element names from JSONObject
     */
    public String[] getSpecified(JSONObject JSONObj, String toFind) {
        ArrayList<String> outputList = new ArrayList<>();
        JSONArray objList = JSONObj.getJSONObject("result").getJSONArray("records"); // list of JSONObjects
        for (int i = 0; i < objList.length(); i++) { // loop through every object
            String name = objList.getJSONObject(i).getString(toFind);
            if (!outputList.contains(name)){
                outputList.add(name); // add element name to output
            }
        }
        if (outputList.size() == 0) {
            System.out.println("WARNING: No elements found for " + toFind + "in JSONObject");
        }
        /*Convert ArrayList to String array*/
        String[] outputArray = new String[outputList.size()];
        outputArray = outputList.toArray(outputArray);
        return outputArray;
    }

    /**
     * Converts an ArrayList<String> to regular String[] array
     * @param in ArrayList to convert
     * @return Regular String[] array of input
     */
    private String[] convertToArray(ArrayList<String> in){
        String[] ret = new String[in.size()]; // convert list of filenames to regular String[] array
        in.toArray(ret);
        return ret;
    }

    /**
     * Converts text file to String; used for testing. Code from:
     * https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
     * @param path Path to file to be converted
     * @param encoding Charset encoding typically StandardCharsets.UTF_8
     * @return String of converted text
     */
    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    /**
     * Converts a JSONObject to String and writes it to a text file
     * @param obj JSONObject to write to file
     * @param name Name of text file (excluding '.txt'
     */
    private void writeFile(JSONObject obj, String name) throws IOException{
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
