import java.io.*;
import org.json.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class Driver extends TimerTask {

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
    int totalErrors = 0; // number of errors found in system; must be global to access in threads
    int totalErroneousAssets = 0;

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
        serverMenu();
        System.out.println("Setup Complete!"); // confirmation message
    }

    /**
     * Called when Driver is invoked on a timer
     */
    @Override
    public void run() {
        updateDB(); // When Driver is called on a Timer, update the DB
    }

    /**
     * Used to manually input commands to the Driver
     */
    public void serverMenu(){

        while (!"9".equals(input)) {
            INVALID:
            {
                System.out.println("-- Actions --");
                System.out.println("Select an Action:\n" + // Menu Options
                                    //"  1) Print Database\n" +
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

        /*Test metadata and meter data on separate threads*/
        totalErrors = 0;
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(new Thread() { // execute code on new thread
            public void run() {
                //System.out.println("Meter Errors: "+ testAllMeters());
                totalErrors =+ testAllMeters();
            }
        });
        es.execute(new Thread() { // execute code on new thread
            public void run() {
                //System.out.println("Metadata Errors: "+ testMetadata());
                totalErrors =+ testMetadata();
            }
        });

        /*Wait for both threads to end; meaning all metadata and meter readings have been tested*/
        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e) {
            System.out.println("Initialising Failed: Data analysis was interrupted");
            return 0;
        }

        /*Log an overview of quality to the DB*/
        int nAssets = meterJSON.length() + loggerJSON.length();
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now.getTime()); // use DB time value as current time
        database.addLog(nAssets, database.getTableLength("erroneousassets"), database.getTableLength("errors"), timestamp);

        System.out.println("Database Initialised!"); // confirmation message
        return 1;
    }

    /**
     * Update the EIS quality database by only analysing records from ckan which are currently unaccounted for
     * @return Number of errors found
     */
    public int updateDB(){

        String month = new SimpleDateFormat("MMM").format(Calendar.getInstance().getTime()).toLowerCase();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String fileNameEnding = "-"+month+"-"+year; // file ending used in ckan file e.g. '-sep-2017'

        JSONArray meterList = meterJSON.getJSONObject("result").getJSONArray("records"); //list of meters
        int errors = 0; // total number of errors
        int untested = 0; // number of untested meters

        // TODO Each process uses a very large qty of heap space for CKAN requests, therefore multithreading would be better here, but it needs testing with thread limits
        for (int i = 0; i < meterList.length(); i++) { // for every meter

            String code = meterList.getJSONObject(i).getString("Logger Asset Code"); // logger code
            String chan = meterList.getJSONObject(i).getString("Logger Channel"); // logger channel
            String util = meterList.getJSONObject(i).getString("Utility Type"); // utility type

            if (!code.equals("") && !chan.equals("")) { // can only test meter if it has a logger code and channel

                String type = meterList.getJSONObject(i).getString("Classification Group"); // finds out whether the file is from EMS or BMS

                if (type.equals(BMS_CLASSIFICATION_GROUP)) { // if the meter is from BMS
                    try {
                        List<JSONObject> json = getBMSMeterJSON(code, chan, "bms"+fileNameEnding); // List of JSON objects, representing every meter reading

                        /*If no readings for this meter were found in CKAN; this is the first (and only) error*/
                        if (json.size() == 0){

                            errors++;
                            Date now = new Date(); // use DB time value as current time
                            Timestamp timestamp = new Timestamp(now.getTime());
                            database.addError(20, code, chan, timestamp); // Write error to DB
                            database.addAsset("meter", code, chan, util, timestamp);
                        }
                        else {
                            errors =+ testMeter(json, util); // test every meter
                        }
                    }
                    catch (Exception e){
                        untested++;
                        // Nothing more; continue processing next meter
                    }
                }
                else if (type.equals(EMS_CLASSIFICATION_GROUP)){ // if the meter is from EMS
                    try {
                        //TODO Uncomment once JSON values can be read effectively from ckan; refer to TODO in getEMSMeterJSON method
//                        List<JSONObject> json = getEMSMeterJSON(code, chan, "ems"+fileNameEnding); // List of JSON objects, representing every meter reading
//
//                        /*If no readings for this meter were found in CKAN; this is the first (and only) error*/
//
//                        if (json.size() == 0){
//
//                            errors+=
//                            Date now = new Date(); // use DB time value as current time
//                            Timestamp timestamp = new Timestamp(now.getTime());
//                            database.addError(20, code, chan, timestamp); // Write error to DB
//                            database.addAsset("meter", code, chan, util, timestamp);
//                        }
//                        else {
//                             errors =+ testMeter(json, util); // test every meter
//                        }
                    }
                    catch (Exception e){
                        untested++;
                        // Nothing more; continue processing next meter
                    }
                }
            }
            else { // meter doesn't have a code and channel
                untested++;
                // Nothing more; continue processing next meter
            }
        }
        System.out.println("Finished Update! Debug: Could Not Test "+untested+" meters"); // debug

        /*Log an overview of quality to the DB*/
        int nAssets = meterJSON.length() + loggerJSON.length();
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now.getTime()); // use DB time value as current time
        database.addLog(nAssets, database.getTableLength("erroneousassets"), database.getTableLength("errors"), timestamp);

        return errors; // successfully updated DB
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

        /*Test every logger in metadata*/
        int errors = 0; // number of errors found; to return
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now.getTime()); // all errors logged with current time

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
                database.addError(1, loggerCodes[i], "", timestamp);
            }

            /*Test for loggers with missing data fields (asset code, logger channel, description etc...*/
            if (loggerList.getJSONObject(i).getString("Building Code").equals("")){ // no building code
                errorDetected = true;
                errors++;
                database.addError(2, loggerCodes[i], "", timestamp);
            }
            if (loggerList.getJSONObject(i).getString("Description").equals("")){ // no description
                errorDetected = true;
                errors++;
                database.addError(3, loggerCodes[i], "", timestamp);
            }

            /*If an error was found for the logger, add this logger to quality database*/
            if (errorDetected){
                errors++;
                database.addAsset("logger", loggerCodes[i], timestamp);
            }
        }

        /*Test every meter in metadata*/
        for (int i = 0; i < meterCodes.length; i++){ // for every meter
            boolean errorDetected = false;
            String chan = meterList.getJSONObject(i).getString("Logger Channel");

            /*Test for meters without loggers associated with it in the metadata*/
            boolean found = false;
            for (int j = 0; j < loggerCodes.length; j++){ // look at every logger
                if (meterCodes[i].equals(loggerCodes[j])) {
                    found = true;
                    break; // break if logger matching meter is found
                }
            }
            if (!found){ // if no logger matching the meter was found
                errorDetected = true;
                errors++;
                database.addError(10, meterCodes[i], chan, timestamp); // log an error
            }

            /*Test for meters with missing data fields (asset code, logger channel, description etc...*/
            if (meterList.getJSONObject(i).getString("Asset Code").equals("")){
                errorDetected = true;
                errors++;
                database.addError(11, meterCodes[i], chan, timestamp);
            }
            if (meterList.getJSONObject(i).getString("Description").equals("")){
                errorDetected = true;
                errors++;
                database.addError(12, meterCodes[i], chan, timestamp);
            }

            /*If an error was found for the meter, add this meter to database*/
            if (errorDetected){
                database.addAsset("meter", meterCodes[i], chan, meterList.getJSONObject(i).getString("Utility Type"), timestamp);
            }
        }
        return errors;
    }

    /**
     * Test every meter for EMS and BMS for errors
     * @return The number of errors found
     */
    private int testAllMeters(){

        JSONArray meterList = meterJSON.getJSONObject("result").getJSONArray("records"); //list of meters
        int errors = 0; // to return
        int untested = 0; // number of meters that were not tested

        // TODO Each process uses a very large qty of heap space for CKAN requests, therefore multithreading would be better here, but it needs testing with thread limits
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

                            errors++;
                            Date now = new Date(); // use DB time value as current time
                            Timestamp timestamp = new Timestamp(now.getTime());
                            database.addError(20, code, chan, timestamp); // Write error to DB
                            database.addAsset("meter", code, chan, util, timestamp);
                        }
                        else {
                            errors =+ testMeter(json, util); // test every meter
                        }
                    }
                    catch (Exception e){
                        untested++;
                        // Nothing more; continue processing next meter
                    }
                }
                else if (type.equals(EMS_CLASSIFICATION_GROUP)){ // if the meter is from EMS
                    try {
                        // TODO Uncomment once JSON values can be read effectively from ckan for EMS; refer to TODO in getEMSMeterJSON method
//                        List<JSONObject> json = getEMSMeterJSON(code, chan); // List of JSON objects, representing every meter reading
//
//                        /*If no readings for this meter were found in CKAN; this is the first (and only) error*/
//
//                        if (json.size() == 0){
//
//                            errors++;
//                            Date now = new Date(); // use DB time value as current time
//                            Timestamp timestamp = new Timestamp(now.getTime());
//                            database.addError(20, code, chan, timestamp); // Write error to DB
//                            database.addAsset("meter", code, chan, util, timestamp);
//                        }
//                        else {
//                            errors =+ testMeter(json, util); // test every meter
//                        }
                    }
                    catch (Exception e){
                        untested++;
                        // Nothing more; continue processing next meter
                    }
                }
            }
            else { // meter doesn't have a code and channel
                untested++;
                // Ignore
            }
        }
        System.out.println("Finished! Debug: Could Not Test "+untested+" meters"); // debug
        return errors; // number of meters which were not tested
    }

    /**
     * Tests a specified meter / sensor for errors and adds any detected errors to the sql database
     * @param jsonValues List of JSON objects representing every meter reading
     * @param utilityType The utility type of the meter
     */
    private int testMeter(List<JSONObject> jsonValues, String utilityType){

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
        String loggerCode;
        String moduleKey;
        try {
            loggerCode = jsonValues.get(0).getString("Logger Asset Code");
            moduleKey = jsonValues.get(0).getString("Logger Channel");
        }
        catch (Exception e){
            return 0; // method fails; jsonValues was likely empty
        }

        int errors = 0;
        boolean errorDetected = false;
        Date mostRecentError = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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
            Timestamp timestamp = new Timestamp(mostRecentError.getTime());
            database.addAsset("meter", loggerCode, moduleKey, utilityType, timestamp);
        }
        return 1; // successfully tested meter
    }

    /**
     * Convert all data for a single BMS meter to a single list of JSON objects.
     * Both the logger code and module key make a unique identifier for the Meter
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key aka logger channel
     * @return List of all data for the specified BMS meter
     */
    private List<JSONObject> getBMSMeterJSON(String loggerCode, String moduleKey){
        return getBMSMeterJSON(loggerCode, moduleKey, "");
    }

    /**
     * Convert all data for a single BMS meter to a single list of JSON objects.
     * Both the logger code and module key make a unique identifier for the Meter
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key aka logger channel
     * @param file BMS file name to read from, if unspecified, all BMS files will be read
     * @return List of all data for the specified BMS meter
     */
    private List<JSONObject> getBMSMeterJSON(String loggerCode, String moduleKey, String file){

        readingJSON = null; // reset JSONObject for reading; precaution
        List<JSONObject> jsonValues = new ArrayList<>(); // to return

        try {
            /*List of BMS files in CKAN*/
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=bms");
            JSONObject bmsJSON = ckanReq.requestJSON();
            JSONArray bmsList = bmsJSON.getJSONObject("result").getJSONArray("resources"); // Array of bms files in CKAN (JSON Objects)
            Map<String, String> fileMap = new HashMap<>();
            for (int i = 0; i < bmsList.length(); i++){ // for every BMS file name in ckan

                String fileName = bmsList.getJSONObject(i).getString("name"); // next BMS filename

                if (file.equals("")) { // if no file is specified as parameter, list all BMS filenames

                    if (!fileName.equals("bmsdevicemeta") && !fileName.equals("bmsmodulemeta")) { // don't include bms metadata in list

                        //TODO Data in CKAN pre Dec-2016 is a different format (or in some cases blank), this 'if' can be removed if ckan is changed
                        if (fileName.contains("2017") || fileName.equals("bms-dec-2016")) {
                            fileMap.put(bmsList.getJSONObject(i).getString("id"), fileName); // add file name to list
                        }
                    }
                }
                else if (fileName.equals(file)){ // if a file is specified as parameter, only list this filename

                    fileMap.put(bmsList.getJSONObject(i).getString("id"), fileName); //add file name to list
                }
            }

            readingJSON = new JSONObject(); // empty JSON to repeatedly update with meter data

            /*Get data for the specified meter from every bms file name listed*/
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
                        }
                        catch (Exception e) {
                            System.out.println("Could not read " + fileMap.get(fileID));
                            //e.printStackTrace();
                        }
                    }
                });
            }
            readingJSON.accumulate("files", fileMap); // append list of files read to JSON

            /*Wait for all thread to end*/
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            // System.out.println("All records read: " + loggerCode + "-" + moduleKey); // debug

            /*Add each JSONObject from meter into a list (which will later be returned)*/
            JSONArray meterArray = readingJSON.getJSONArray("records");
            for (int i = 1; i < meterArray.length(); i++) { // for every record, which is a JSON array of JSON arrays
                JSONArray jsonArray = meterArray.getJSONArray(i); // get the JSON array at position i
                for (int j = 0; j < jsonArray.length(); j++) {
                    jsonValues.add(jsonArray.getJSONObject(j)); // add every JSON object in the array to the return list
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
     * Both the logger code and module key make a unique identifier for the Meter
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key aka logger channel
     * @return List of all data for the specified BMS meter
     */
    private List<JSONObject> getEMSMeterJSON(String loggerCode, String moduleKey){
        return getBMSMeterJSON(loggerCode, moduleKey, "");
    }

    /**
     * Convert all data for a single EMS meter to a single list of JSON objects.
     * @param loggerCode Meters / sensor's logger code
     * @param moduleKey Meter / sensor's module key aka logger channel
     * @param file EMS file name to read from, if unspecified, all EMS files will be read
     * @return List of all data for the specified EMS meter
     */
    public List<JSONObject> getEMSMeterJSON(String loggerCode, String moduleKey, String file){

        readingJSON = null; // reset JSONObject for reading; precaution
        List<JSONObject> jsonValues = new ArrayList<>(); // to return

        /*TODO New EMS metadata does not relate to EMS records, the same logic as getBMSMeterJSON can be used here, but with different filenames and maybe different field names, depending on whether the old metadata is used*/
        try {
            /*List of EMS files in CKAN*/
            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/package_show?id=bms");
            JSONObject bmsJSON = ckanReq.requestJSON();
            JSONArray emsList = bmsJSON.getJSONObject("result").getJSONArray("resources"); // Array of EMS files in CKAN (JSON Objects)
            Map<String, String> fileMap = new HashMap<>();
            for (int i = 0; i < emsList.length(); i++){ // for every EMS file name in ckan

                String fileName = emsList.getJSONObject(i).getString("name"); // next EMS filename

                if (file.equals("")) { // if no file is specified as parameter, list all EMS filenames
                    if (!fileName.equals("emsmeta")) { // don't include EMS metadata in list

                        fileMap.put(emsList.getJSONObject(i).getString("id"), fileName); // add file name to list
                    }
                }
                else if (fileName.equals(file)){ // if a file is specified as parameter, only list this filename

                    fileMap.put(emsList.getJSONObject(i).getString("id"), fileName); //add file name to list
                }
            }

            readingJSON = new JSONObject(); // empty JSON to repeatedly update with meter data

            /*Get data for the specified meter from every EMS file name listed*/
            ExecutorService es = Executors.newCachedThreadPool();
            for (String fileID: fileMap.keySet()) { // for every EMS file
                es.execute(new Thread() { // execute code on new thread
                    public void run() {
                        try {
                            /*Get meter data from bms file*/
                            // TODO This is where you need to change the query, EMS doesnt use 'device_id' and 'module_key' in the data, only an 'id', the new metadata should be changing this to make this statement for work, but for now, it doesn't
                            CKANRequest ckanReq = new CKANRequest("ckan.lancaster.ac.uk/api/3/action/datastore_search_sql?sql=SELECT%20*%20FROM%20\""
                                    + fileID + "\"%20WHERE%20device_id='"+loggerCode+"'%20AND%20module_key='"+moduleKey+"'");
                            JSONObject newJSON = ckanReq.requestJSON(); // JSON object of meter data from this file

                            /*Append meter data to JSON*/
                            JSONArray toAccumulate = newJSON.getJSONObject("result").getJSONArray("records"); // records from JSON object to append
                            readingJSON.accumulate("records", toAccumulate); // append meter data to the new JSON object
                        }
                        catch (Exception e) {
                            System.out.println("Could not read " + fileMap.get(fileID));
                            //e.printStackTrace();
                        }
                    }
                });
            }
            readingJSON.accumulate("files", fileMap); // append list of files read to JSON

            /*Wait for all thread to end*/
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            // System.out.println("All records read: " + loggerCode + "-" + moduleKey); // debug

            /*Add each JSONObject from meter into a list (which will later be returned)*/
            JSONArray meterArray = readingJSON.getJSONArray("records");
            for (int i = 1; i < meterArray.length(); i++) { // for every record, which is a JSON array of JSON arrays
                JSONArray jsonArray = meterArray.getJSONArray(i); // get the JSON array at position i
                for (int j = 0; j < jsonArray.length(); j++) {
                    jsonValues.add(jsonArray.getJSONObject(j)); // add every JSON object in the array to the return list
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return jsonValues; // Returns list of JSON objects for all meter readings
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

        /*Start the driver menu for manual input*/
//        (new Thread() {
//            public void run() {
//                new Driver().serverMenu();
//            }
//        }).start();

        /*Update the database every morning*/
        Calendar date = Calendar.getInstance(); // initialise an object for 8am
        date.set(Calendar.HOUR_OF_DAY, 8);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        Timer timer = new Timer(); // Schedule to run every day 8am
        int millisecInADay = 1000 * 60 * 60 * 24;
        timer.schedule(new Driver(), date.getTime(), millisecInADay);
    }
}
