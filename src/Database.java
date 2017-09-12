import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Class used for adding records to the EIS quality assurance database in MySQL
 * @Author Ross Newby
 */
public class Database {

    private static String URL = "not set"; // mysql authentication
    private static String USER = "not set";
    private static String PASSWORD = "not set";

    public static String ASSET_DB_NAME = "erroneousassets"; // table names in DB
    public static String ERROR_DB_NAME = "errors";
    public static String QUALITY_LOG_DB_NAME = "qualitylog";
    public static String ERROR_LOOKUP_DB_NAME = "errortypelookup";

    static private final int PAD_SIZE = 30; // for printing DB

    private Connection con; // mysql DB connection
    private Statement st;
    private ResultSet rs;

    public Database(String hostURL){

        /*Read configuration file; populate variables*/
        this.URL = hostURL;
        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("'" + propFileName + "' not found in classpath");
            }

            USER = prop.getProperty("mysqluser");
            PASSWORD = prop.getProperty("mysqlpass");
        }
        catch (Exception e){
            System.out.println("Error Reading Configuration File:");
            e.printStackTrace();
        }

        /*Connect to MySQL database*/
        try {
            Class.forName("com.mysql.jdbc.Driver");

            con = DriverManager.getConnection(URL+"?autoReconnect=true&useSSL=false", USER, PASSWORD);
            st = con.createStatement();
        }
        catch (Exception e){
            System.out.println("MySQL Error:");
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the erroneous asset table in the EIS quality database
     * @param ware Value to insert into hardware field
     * @param logCode Value to insert into logger code field
     * @param logChan Value to insert into logger channel field
     * @param util Value to insert into utility type field
     */
    public void addAsset(String ware, String logCode, String logChan, String util, Timestamp time){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ASSET_DB_NAME+" (hardware, logger_code, logger_channel, utility_type, most_recent_error) VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE most_recent_error = ?");
            stmt.setString(1, ware); // specify each parameter ('?') in the query
            stmt.setString(2,logCode);
            stmt.setString(3, logChan);
            stmt.setString(4, util);
            stmt.setTimestamp(5, time);
            stmt.setTimestamp(6, time);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ASSET_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the erroneous asset table in the EIS quality database
     * @param ware Value to insert into hardware field
     * @param logCode Value to insert into logger code field
     * @param logChan Value to insert into logger channel field
     */
    public void addAsset(String ware, String logCode, String logChan, Timestamp time){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ASSET_DB_NAME+" (hardware, logger_code, logger_channel, most_recent_error) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE most_recent_error = ?");
            stmt.setString(1, ware); // specify each parameter ('?') in the query
            stmt.setString(2,logCode);
            stmt.setString(3, logChan);
            stmt.setTimestamp(4, time);
            stmt.setTimestamp(5, time);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ASSET_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the erroneous asset table in the EIS quality database
     * @param ware Value to insert into hardware field
     * @param logCode Value to insert into logger code field
     */
    public void addAsset(String ware, String logCode, Timestamp time){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ASSET_DB_NAME+" (hardware, logger_code, most_recent_error) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE most_recent_error = ?");
            stmt.setString(1, ware); // specify each parameter ('?') in the query
            stmt.setString(2,logCode);
            stmt.setTimestamp(3, time);
            stmt.setTimestamp(4, time);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ASSET_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the erroneous asset table in the EIS quality database
     * @param errType Value to insert into error type field
     * @param logCode Value to insert into logger code field
     * @param logChan Value to insert into logger channel field
     * @param time Value to insert into time field
     */
    public void addError(int errType, String logCode, String logChan, Timestamp time){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ERROR_DB_NAME+" (error_type, logger_code, logger_channel, timeVal) VALUES(?, ?, ?, ?)");
            stmt.setInt(1, errType); // specify each parameter ('?') in the query
            stmt.setString(2,logCode);
            stmt.setString(3, logChan);
            stmt.setTimestamp(4, time);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ERROR_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the errors table in the EIS quality database
     * @param errType Value to insert into error type field
     * @param logCode Value to insert into logger code field
     * @param logChan Value to insert into logger channel field
     */
    public void addError(int errType, String logCode, String logChan){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ERROR_DB_NAME+" (error_type, logger_code, logger_channel) VALUES(?, ?, ?)");
            stmt.setInt(1, errType); // specify each parameter ('?') in the query
            stmt.setString(2,logCode);
            stmt.setString(3, logChan);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ERROR_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the errors table in the EIS quality database
     * @param errType Value to insert into error type field
     * @param logCode Value to insert into logger code field
     */
    public void addError(int errType, String logCode){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ERROR_DB_NAME+" (error_type, logger_code) VALUES(?, ?)");
            stmt.setInt(1, errType); // specify each parameter ('?') in the query
            stmt.setString(2,logCode);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ERROR_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the quality log table in the EIS quality database
     * @param assets Total number of hardware devices on the system
     * @param erroneousAssets Total number of hardware devices for which errors were detected
     * @param errors Total number of individual errors
     * @param time Time at which this quality test occurred
     */
    public void addLog(int assets, int erroneousAssets, int errors, Timestamp time){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+QUALITY_LOG_DB_NAME+" (total_assets, erroneous_assets, error_count, timeVal) VALUES(?, ?, ?, ?)");
            stmt.setInt(1, assets); // specify each parameter ('?') in the query
            stmt.setInt(2,erroneousAssets);
            stmt.setInt(3, errors);
            stmt.setTimestamp(4, time);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ QUALITY_LOG_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Adds a record to the error type lookup table in the EIS quality database
     * @param id Unique ID for record
     * @param desc Description of the error
     */
    public void addLookup(int id, String desc){
        try {
            PreparedStatement stmt = con.prepareStatement("INSERT INTO "+ERROR_LOOKUP_DB_NAME+" VALUES(?, ?)");
            stmt.setInt(1, id); // specify each parameter ('?') in the query
            stmt.setString(2, desc);

            int i = stmt.executeUpdate();
            System.out.println(i +" record(s) added to "+ ERROR_LOOKUP_DB_NAME);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the number of records found in a table
     * @param tableName Name of the table you wish to query
     * @return Number of records in the table or -1 if the method fails
     */
    public int getTableLength(String tableName){
        try {
            Statement sqlStmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rSet = sqlStmt.executeQuery("SELECT * FROM "+tableName);
            rSet.last();
            return rSet.getRow();
        }
        catch (Exception e){
            return -1;
        }
    }

    /**
     * Executes a set of SQL statements from file. Adaptation from code by Tom Enders:
     * https://coderanch.com/t/306966/databases/Execute-sql-file-java
     * @param path File path to SQL scripts e.g <path to folder>/scripts.sql
     * @throws SQLException SQL syntax error in specified filepath
     * @throws FileNotFoundException Could not find file for specified filepath
     */
    public int executeSQLScript(String path) throws SQLException, FileNotFoundException {

        try {
            FileReader fr = new FileReader(new File(path));
            BufferedReader br = new BufferedReader(fr);
            String s;
            StringBuffer sb = new StringBuffer();

            while((s = br.readLine()) != null)
            {
                sb.append(s);
            }
            br.close();

            // here is our splitter ! We use ";" as a delimiter for each request
            // then we are sure to have well formed statements
            String[] inst = sb.toString().split(";");

            for(int i = 0; i<inst.length; i++)
            {
                // we ensure that there is no spaces before or after the request string
                // in order to not execute empty statements
                if(!inst[i].trim().equals(""))
                {
                    st.executeUpdate(inst[i]);
                    // System.out.println(">>"+inst[i]); // print commands as they execute
                }
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("DB Error: Could not find file "+ path);
            throw e;
        }
        catch(IOException e) {
            System.out.println("DB Error: Could not read file "+ path);
            return 0;
        }
        catch(SQLException e) {
            System.out.println("DB Error: Syntax error in SQL from file "+ path);
            throw e;
        }
        return 1;
    }

    /**
     * Prints all tables in database; used for debugging and referencing
     */
    public void printDatabase() {
        //find out what all the table names in the database are
        ArrayList<String> tables = new ArrayList<>();
        try {
            DatabaseMetaData md = con.getMetaData();

            String[] types = {"TABLE"};
            ResultSet rs = md.getTables(null, null, null, types);

            while (rs.next()) { //read in all tables in the database
                tables.add(rs.getString(3));
            }

            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < tables.size(); i++) {
            ArrayList<String> cols = new ArrayList<>();

            try {
                Statement sqlStmt = con.createStatement();

                String sqlCmdStr = ("select * from " + tables.get(i));
                ResultSet rSet = sqlStmt.executeQuery(sqlCmdStr);
                ResultSetMetaData mData = rSet.getMetaData();

                System.out.println("Table: " + mData.getTableName(2) + " ("+mData.getColumnCount()+" cols)");

                int rowCount = mData.getColumnCount();

                //read in all the column names
                for (int j = 1; j < rowCount + 1; j++) {
                    cols.add(mData.getColumnName(j));
                }

                //print topmost border
                for (int j = 0; j < cols.size(); j++) {
                    System.out.print(" + " +pad(""));
                }
                System.out.println(" + ");

                //print the names of the columns in the table
                for (int j = 0; j < cols.size(); j++) {
                    System.out.print(" | " + pad(cols.get(j)));
                }
                System.out.println(" | ");

                //print middle border
                for (int j = 0; j < cols.size(); j++) {
                    System.out.print(" + " + pad(""));
                }
                System.out.println(" + ");

                //read and print every row in the table
                while (rSet.next()) {
                    for (int j = 0; j < cols.size(); j++) {
                        String item = rSet.getString(cols.get(j));
                        System.out.print(" | " + pad(item));
                    }
                    System.out.println(" | ");
                }

                //print bottom border
                for (int j = 0; j < cols.size(); j++) {
                    System.out.print(" + " + pad(""));
                }
                System.out.println(" + ");

                rSet.close();
                sqlStmt.close();

            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("");
        }
    }

    /**
     * This method takes a String, converts it into an array of bytes; copies those bytes
     * into a bigger byte array (STR_SIZE worth), and pads any remaining bytes with spaces.
     * Finally, it converts the bigger byte array back into a String.
     * @param in String to pad
     * @return Input string extended or cropped to the length of STR_SIZE
     */
    private String pad(String in) {

        if (in == null){
            in = "";
        }

        byte[] org_bytes = in.getBytes();
        byte[] new_bytes = new byte[PAD_SIZE];
        int upb = in.length();

        if (upb > PAD_SIZE)
            upb = PAD_SIZE;

        for (int i = 0; i < upb; i++) {
            new_bytes[i] = org_bytes[i];
        }

        for (int i = upb; i < PAD_SIZE; i++) {
            new_bytes[i] = (byte) ((in.equals("")) ? '-' : ' ');
        }

        return new String(new_bytes);
    }
}
