//package java.eis.quality;

import java.io.*;
import org.json.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main Class quality assurance processing
 *
 * @Author Ross Newby
 */
public class Driver {

    /*Variables used for metadata and other JSONs*/
    private JSONObject packageJSON = null; // JSON objects for all metadata
    private JSONObject meterJSON = null;
    private JSONObject loggerJSON = null;
    private Thread meterThread, loggerThread; // threads for initial metadata reading
    private JSONObject readingJSON = null; // Global JSON object for reading multiple requests in threads

    /*Used to access CKAN and other files, if paths / names change; amend them here*/
    private static final String DB_INIT_FILEPATH = "src/eisqualityinit.sql"; // mysql database initialisation file
    private static final String DB_HOST = "jdbc:mysql://localhost:3306/eisquality";
    private static final String METER_METADATA_NAME = "Planon metadata - Meters Sensors"; // names of metadata files in CKAN
    private static final String LOGGER_METADATA_NAME = "Planon metadata - Loggers Controllers";
    private static final String BMS_CLASSIFICATION_GROUP = "Energy sensor"; // identifier for EMS records in the metadata
    private static final String EMS_CLASSIFICATION_GROUP = "Energy meter"; // identifier for BMS records

    private Database database = null; // mysql database
    private Scanner scanner = new Scanner(System.in); // used for basic console line input
    private String input = null;

    /**
     * Initialises a server by reading basic authentication details and API data from config file. Also queries CKAN datastore for all
     * metadata and returns once JSON objects have been read completely. EIS Quality database in MySQL is initialised.
     */
    public Driver() {

        /*Get CKAN metadata*/
        try {
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=planonmetadata");
            packageJSON = ckanReq.requestJSON();
            JSONArray packageList = packageJSON.getJSONObject("result").getJSONArray("resources"); // Array of resource names available in CKAN

            String lookingFor = METER_METADATA_NAME; // search for meter / sensor metadata
            for (int i = 0; i < packageList.length(); i++) { // for every package name in CKAN
                if (packageList.getJSONObject(i).getString("name").equals(lookingFor)){ // if package is meter / sensor data
                    // Get package ID number and use this in another CKAN request for meter / sensor metadata
                    String id = packageList.getJSONObject(i).getString("id");
                    meterThread = new Thread() {
                        public void run() {
                            try {
                                CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                                meterJSON = ckanReq.requestJSON();
                            }
                            catch (Exception e){
                                System.out.print("Setup Failed: Could not read Meter metadata from CKAN");
                                return;
                            }
                        }
                    };
                    meterThread.start();
                }
            }
            lookingFor = LOGGER_METADATA_NAME; // Search for logger / controller metadata
            for (int i = 0; i < packageList.length(); i++) { // for every package name in CKAN
                if (packageList.getJSONObject(i).getString("name").equals(lookingFor)){ // if package is loggers / controllers
                    // Get package ID number and use this in another CKAN request for logger / controller metadata
                    String id = packageList.getJSONObject(i).getString("id");
                    loggerThread = new Thread() {
                        public void run() {
                            try {
                                CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\"" + id + "\"");
                                loggerJSON = ckanReq.requestJSON();
                            }
                            catch (Exception e){
                                System.out.print("Setup Failed: Could not read Logger metadata from CKAN");
                                return;
                            }
                        }
                    };
                    loggerThread.start();
                }
            }
        }
        catch (Exception e){
            System.out.print("Setup Failed: Could not read Planon data from CKAN");
            return;
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

        System.out.println("Setup Complete!"); // confirmation message
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
                System.out.println("Select an Action:\n" + // Menu Options
                                    "  1) Print Database\n" +
                                    "  2) Fresh Database Initialisation\n" + // Initialise the DB and all its data, reading every available record from CKAN
                                    "  3) Update Database\n" );
                input = scanner.nextLine();

                if ("1".equals(input)){ // Print Database; debug and testing tool
                    if (database != null) {
                        database.printDatabase();
                    }
                    else {
                        System.out.println("No DB Initialised");
                    }
                }
                else if ("2".equals(input)){ // Initialise the DB and all its data, reading every available record from CKAN

                    System.out.println("This will completely re-populate the DB. Are you sure? Y/N"); // This process will take a long time and erase all records; hence confirmation
                    String response = scanner.nextLine();
                    if (response.toLowerCase().equals("y")){
                        initDB();
                    }
                    else {
                        break INVALID;
                    }
                }
                else if ("3".equals(input)){
                    if (database != null) {
                        updateDB();
                    }
                    else {
                        System.out.println("No DB Initialised");
                    }
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

    /**
     * Perform a full initialisation of the EIS quality database; creating the database schema itself, before analysing every data
     * record in CKAN and populating the database with found errors
     * @return Returns 1 if successful, 0 if error occurred
     */
    public int initDB() {

        System.out.println("Initialising Database...");

        /*Initialise Database Schema*/
        database = new Database(DB_HOST);
        try {
            database.executeSQLScript(DB_INIT_FILEPATH);
        }
        catch (Exception e) {
            System.out.println("Initialising Failed: Could not start DB; check "+ DB_INIT_FILEPATH);
            return 0;
        }
        database.printDatabase(); //debug

        /*Test metadata and meter data on separate threads*/
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(new Thread() { // execute code on new thread
            public void run() {
                System.out.println("Meter Errors: "+ testAllMeters());
            }
        });
        es.execute(new Thread() { // execute code on new thread
            public void run() {
                System.out.println("Metadata Errors: "+ testMetadata());
            }
        });

        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e) {
            System.out.println("Initialising Failed: Data analysis was interrupted");
            return 0;
        }

        System.out.println("Database Initialised!"); // confirmation message
        return 1;
    }

    /**
     * Update the EIS quality database by only analysing records from ckan which are currently unaccounted for
     * @return Returns 1 if successful, 0 if error occurred
     */
    public int updateDB(){
        // Update DB Records
        System.out.println("Not Implemented.");
        return 1;
    }

    /**
     * Tests the CKAN metadata for errors and adds any detected errors to the SQL database
     * @returns The number of errors found
     */
    private int testMetadata() {
        System.out.println("Processing Metadata..."); //debug
        String[] loggerCodes, meterCodes;

        /*First; must get all logger codes from both meter and logger metadata. This is used later to test whether loggers have meters
        * associated with them in the metadata and visa-versa*/

        /*All logger codes in logger metadata*/
        ArrayList<String> loggerCodeArray = new ArrayList<>();
        JSONArray loggerList = loggerJSON.getJSONObject("result").getJSONArray("records"); //list of loggers
        for (int i = 0; i < loggerList.length(); i++) { // every logger
            String code = loggerList.getJSONObject(i).getString("Logger Serial Number"); //store logger code
            loggerCodeArray.add(code);
        }
        loggerCodes = convertToArray(loggerCodeArray); // convert to regular String arrays

        /*All logger codes in meter metadata*/
        ArrayList<String> meterArray = new ArrayList<>();
        JSONArray meterList = meterJSON.getJSONObject("result").getJSONArray("records"); //list of meters
        for (int i = 0; i < meterList.length(); i++) { // every meter
            String code = meterList.getJSONObject(i).getString("Logger Asset Code"); // store logger code
            meterArray.add(code);
        }
        meterCodes = convertToArray(meterArray); // convert to regular String arrays

        int errors = 0;
        /*Test every logger in metadata*/

        for (int i = 0; i < loggerCodes.length; i++){ // for every logger
            boolean errorDetected = false;

            /*Test for loggers without meters associated with it in the metadata*/
            boolean found = false;
            for (int j = 0; j < meterCodes.length; j++){ // look at every meter
                if (loggerCodes[i].equals(meterCodes[j])) {
                    found = true;
                    break; // break if meter matching logger is found
                }
            }
            if (!found){
                errorDetected = true;
                errors++;
                database.addError(1, loggerCodes[i], "");
            }

            /*Test for loggers with missing data fields (asset code, logger channel, description etc...*/
            if (loggerList.getJSONObject(i).getString("Building Code").equals("")){ // no building code
                errorDetected = true;
                errors++;
                database.addError(2, loggerCodes[i], "");
            }
            if (loggerList.getJSONObject(i).getString("Description").equals("")){ // no description
                errorDetected = true;
                errors++;
                database.addError(3, loggerCodes[i], "");
            }

            /*If an error was found for the logger, add this logger to quality database*/
            if (errorDetected){
                errors++;
                database.addAsset("logger", loggerCodes[i]);
            }
        }

        /*Test every meter in metadata*/
        for (int i = 0; i < meterCodes.length; i++){ // for every meter
            boolean errorDetected = false;

            /*Test for meters without loggers associated with it in the metadata*/
            boolean found = false;
            for (int j = 0; j < loggerCodes.length; j++){ // look at every logger
                if (meterCodes[i].equals(loggerCodes[j])) {
                    found = true;
                    break; // break if logger matching meter is found
                }
            }
            if (!found){
                errorDetected = true;
                errors++;
                database.addError(10, meterCodes[i], "");
            }

            /*Test for meters with missing data fields (asset code, logger channel, description etc...*/
            if (meterList.getJSONObject(i).getString("Asset Code").equals("")){
                errorDetected = true;
                errors++;
                database.addError(11, meterCodes[i], "");
            }
            if (meterList.getJSONObject(i).getString("Description").equals("")){
                errorDetected = true;
                errors++;
                database.addError(12, meterCodes[i], "");
            }

            /*If an error was found for the meter, add this meter to database*/
            if (errorDetected){
                database.addAsset("meter", meterCodes[i], meterList.getJSONObject(i).getString("Logger Channel"), meterList.getJSONObject(i).getString("Utility Type"));
            }
        }
        return errors;
    }

    /**
     * Test every meter for EMS and BMS for errors
     * @return The number of meters which could not be processed for errors
     */
    private int testAllMeters(){

        JSONArray meterList = meterJSON.getJSONObject("result").getJSONArray("records"); //list of meters
        int toReturn = 0;
        for (int i = 0; i < meterList.length(); i++) { // for every meter

            String code = meterList.getJSONObject(i).getString("Logger Asset Code"); // logger code
            String chan = meterList.getJSONObject(i).getString("Logger Channel"); // logger channel
            String util = meterList.getJSONObject(i).getString("Utility Type"); // utility type

            if (!code.equals("") && !chan.equals("")) { // can only test meter if it has a logger code and channel

                String type = meterList.getJSONObject(i).getString("Classification Group");

                if (type.equals(BMS_CLASSIFICATION_GROUP)) { // if the meter is from BMS
                    try {
                        List<JSONObject> json = getBMSMeterJSON(code, chan); // List of JSON objects, representing every meter reading

                        /*If no readings for this meter were found in CKAN; this is the first (and only) error*/
                        if (json.size() == 0){

                            Date now = new Date(); // use DB time value as current time
                            Timestamp timestamp = new Timestamp(now.getTime());
                            database.addError(20, code, chan, timestamp); // Write error to DB
                        }
                        else {
                            testMeter(json, util); // test every meter
                        }
                    }
                    catch (Exception e){
                        toReturn++;
                        // Nothing more; continue processing next meter
                    }
                }
                else if (type.equals(EMS_CLASSIFICATION_GROUP)){ // if the meter is from EMS
                    try {
                        List<JSONObject> json = getBMSMeterJSON(code, chan); // List of JSON objects, representing every meter reading

                        /*If no readings for this meter were found in CKAN; this is the first (and only) error*/
                        if (json.size() == 0){

                            Date now = new Date(); // use DB time value as current time
                            Timestamp timestamp = new Timestamp(now.getTime());
                            database.addError(20, code, chan, timestamp); // Write error to DB
                        }
                        else {
                            testMeter(json, util); // test every meter
                        }
                    }
                    catch (Exception e){
                        toReturn++;
                        // Nothing more; continue processing next meter
                    }
                }
                // TODO Multithreading (if possible, as each process uses a very large qty of heap space for CKAN requests, thread limit may be the answer)
            }
            else { // meter doesn't have a code and channel
                toReturn++;
            }
        }
        return toReturn; // number of meters which were not tested
    }

    /**
     * Tests a specified BMS meter for errors and adds any detected errors to the sql database
     * @param jsonValues List of JSON objects representing every meter reading
     * @param utilityType
     */
    private void testMeter(List<JSONObject> jsonValues, String utilityType){

        int errors = 0;
        boolean errorDetected = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        String loggerCode = jsonValues.get(0).getString("Logger Asset Code");
        String moduleKey = jsonValues.get(0).getString("Logger Channel");

        /*If no readings for this meter were found in CKAN; this is the first (and only) error*/
//        if (jsonValues.size() == 0){
//
//            Date now = new Date(); // use DB time value as current time
//            Timestamp timestamp = new Timestamp(now.getTime());
//            database.addError(20, loggerCode, moduleKey, timestamp); // Write error to DB
//            return; // return from method; no records to test therefore no need to continue
//        }

        /*Sort the meter readings by their timestamp*/
        Collections.sort( jsonValues, new Comparator<JSONObject>() {

            private static final String KEY_NAME = "timestamp"; // key to sort JSONArray by

            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = new String();
                String valB = new String();

                try {
                    valA = a.getString(KEY_NAME);
                    valB = b.getString(KEY_NAME);
                }
                catch (JSONException e) {
                    System.out.println("Error: No value for key '"+KEY_NAME+"' in JSONObject");
                }

                return -valA.compareTo(valB); // -ve to sort in descending order; most recent dates first
            }
        });

        /*Error Tests for Meter:*/

        /*Check whether meter has recent data*/
        Date now = new Date(); // time now
        Calendar cal = Calendar.getInstance();
        cal.setTime(now); // initiate calendar instance with current time
        cal.add(Calendar.DATE, -2); // subtract 2 days from calender
        Date dateBefore2Days = cal.getTime(); // new date object representing 2 days ago
        try {

            Date mostRecentMeterReading = dateFormat.parse(jsonValues.get(0).getString("timestamp").replace("T", " ")); // Must remove the 'T' from CKAN response for string to be parsable

            if (mostRecentMeterReading.compareTo(dateBefore2Days) < 0){ // if most recent meter reading is more than 2 days old
                errors++;
                errorDetected = true;
                Timestamp timestamp = new java.sql.Timestamp(mostRecentMeterReading.getTime());
                database.addError(21, loggerCode, moduleKey, timestamp);
            }
        }
        catch (Exception e){
            //System.out.println("Failed to parse meter's timestamp to Date format");
            e.printStackTrace();
        }

        /*Check Quality of reading: -ve data, no data etc.*/
        for (int i = 0; i < jsonValues.size(); i++){ // for every CKAN record
            String readS = jsonValues.get(i).getString("param_value");
            double reading = Double.parseDouble(readS); // meter reading from every CKAN record

            /*Test for negative data readings*/
            if (reading < 0){ // no readings should be -ve
                errors++;
                errorDetected = true;
                try {
                    String timeS = jsonValues.get(i).getString("timestamp").replace("T", " "); // Must remove the 'T' from CKAN response for string to be parsable
                    Date date = dateFormat.parse(timeS);
                    Timestamp timestamp = new Timestamp(date.getTime());
                    database.addError(22, loggerCode, moduleKey, timestamp);
                }
                catch (ParseException e){
                    e.printStackTrace();
                }
            }

            /*Test for strange data readings, based on utility type*/
            if (utilityType.equals("insert utility type")){ // if utility type is specified
                // TODO test for different utility types
                // error_type for non-normal data reading = 23
            }
        }

        /*Check time interval of readings are correct*/
        // TODO test time intervals
        // error_type for non-normal data reading = 24

        System.out.println("Meter " + loggerCode +"-" + moduleKey + " Errors: " + errors); // debug

        /*If an error was found for the meter, add this meter to database assets*/
        if (errorDetected){
            database.addAsset("meter", loggerCode, moduleKey, utilityType);
        }
    }

    /**
     * Convert all data for a single BMS meter to a single list of JSON objects.
     * Both the logger code and module key make a unique identifier for the Meter
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key aka logger channel
     * @return List of all data for the specified BMS meter
     */
    private List<JSONObject> getBMSMeterJSON(String loggerCode, String moduleKey){

        readingJSON = null; // reset JSONObject for reading; precaution
        List<JSONObject> jsonValues = new ArrayList<>();

        try {
            /*List of BMS files in CKAN*/
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=bms");
            JSONObject bmsJSON = ckanReq.requestJSON();
            JSONArray bmsList = bmsJSON.getJSONObject("result").getJSONArray("resources"); // Array of bms files in CKAN (JSON Objects)
            Map<String, String> fileMap = new HashMap<>();
            for (int i = 0; i < bmsList.length(); i++){ // each BMS file
                String fileName = bmsList.getJSONObject(i).getString("name"); // next BMS filename
                if (!fileName.equals("bmsdevicemeta") && !fileName.equals("bmsmodulemeta")){ // don't include bms metadata in list

                    if(fileName.contains("2017") || fileName.equals("bms-dec-2016")) { //TODO Account for older 2016 data
                        fileMap.put(bmsList.getJSONObject(i).getString("id"), fileName);
                    }
                }
            }

            readingJSON = new JSONObject(); // empty JSON to repeatedly update with meter data

            /*Get data for the specified meter from every bms file*/
            ExecutorService es = Executors.newCachedThreadPool();
            for (String fileID: fileMap.keySet()) { // for every bms file
                es.execute(new Thread() { // execute code on new thread
                    public void run() {
                        try {
                            /*Get meter data from bms file*/
                            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\""
                                                                    + fileID + "\"%20WHERE%20device_id='"+loggerCode+"'%20AND%20module_key='"+moduleKey+"'");
                            JSONObject newJSON = ckanReq.requestJSON(); // JSON object of meter data from this file

                            /*Append meter data to JSON*/
                            JSONArray toAccumulate = newJSON.getJSONObject("result").getJSONArray("records"); // records from JSON object to append
                            readingJSON.accumulate("records", toAccumulate); // append meter data to the new JSON object

                            /*TODO Is it better to have this slower, but neater approach?*/
                            /*Adding each JSONObject separately (below) results in a better return JSON but much longer processing time
                            for (int i = 0; i < toAccumulate.length(); i++){
                                readingJSON.accumulate("records", toAccumulate.getJSONObject(i));
                            }*/
                        }
                        catch (Exception e) {
                            System.out.println("Could not read " + fileMap.get(fileID));
                            //e.printStackTrace();
                        }
                    }
                });
            }
            readingJSON.accumulate("files", fileMap); // append list of files to JSON

            /*Wait for all thread to end*/
            //System.out.println("Waiting on Threads for " + loggerCode + "-" + moduleKey + "...");
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            System.out.println("All records read: " + loggerCode + "-" + moduleKey);

            /*Add each JSONObject from meter into a list (which will later be returned)*/
            JSONArray meterArray = readingJSON.getJSONArray("records");
            for (int i = 1; i < meterArray.length(); i++) { //TODO change looping if taking the slower (but neater) approach above
                JSONArray jsonArray = meterArray.getJSONArray(i);
                for (int j = 0; j < jsonArray.length(); j++) {
                    jsonValues.add(jsonArray.getJSONObject(j));
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return jsonValues; // Returns list of JSON objects for all meter readings
    }

    /**
     * Convert all data for a single EMS meter to a single list of JSON objects.
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key aka logger channel
     * @return List of all data for the specified EMS meter
     */
    public List<JSONObject> getEMSMeterJSON(String loggerCode, String moduleKey){

        readingJSON = null; // reset JSONObject for reading; precaution
        List<JSONObject> jsonValues = new ArrayList<>();

        /*TODO New EMS metadata does not relate to EMS records, the same logic as getBMSMeterJSON can be used here, but with different filenames and maybe different field names, depending on whether the old metadata is used*/

        return jsonValues; // Returns list of JSON objects for all meter readings
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
     * A set of elements from a specified JSON attribute; duplicate elements are ignored
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
    private void writeFile(Object obj, String name) throws IOException{
        String fileName = name+".txt";
        try (FileWriter file = new FileWriter(fileName)) {
            file.write(obj.toString());
            System.out.println("Successfully Copied Object to File: " + fileName);
        }
    }

    /**
     * Terminates application
     */
    private void exit(){
        System.out.println("Exiting...");
        System.exit(1);
    }

    public static void main (String args []){

        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Running...");
        new Driver();
    }

}
