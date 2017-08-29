import java.io.*;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * @Author Ross Newby
 */
public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/eisquality";
    private static String USER = "not set";
    private static String PASSWORD = "not set";
    static private final int PAD_SIZE = 25;

    private Connection con;
    private Statement st;
    private ResultSet rs;

    public Database(){

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
     *
     * @param tableName
     * @param values
     */
    public void addRecord(String tableName, String values){

        try {
            String query = ("INSERT INTO "+ tableName + " VALUES (default, " + values + ")");
            //System.out.println("Executing >> " + query); //debug
            st.executeUpdate(query);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param tableName
     * @param values
     */
    public void addRecord(String tableName, Map<String, String> values){
        try {
            StringBuilder columnNames = new StringBuilder();
            StringBuilder valueNames = new StringBuilder();

            columnNames.append(" (");
            valueNames.append(" (");
            for (String key: values.keySet()){
                columnNames.append(key + ", ");
                valueNames.append(values.get(key) + ", ");
            }
            columnNames.setLength(columnNames.length()-2);
            columnNames.append(") ");
            valueNames.setLength(valueNames.length()-2);
            valueNames.append(") ");

            String query = ("INSERT INTO "+ tableName + columnNames + "VALUES" + valueNames);
            //System.out.println("Executing >> " + query); //debug
            st.executeUpdate(query);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // https://coderanch.com/t/306966/databases/Execute-sql-file-java by Tom Enders
    public void executeSQLScript(String path) throws SQLException {

        try {
            FileReader fr = new FileReader(new File(path));
            // be sure to not have line starting with "--" or "/*" or any other non aplhabetical character

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
        catch(Exception e) {
            System.out.println("DB Error:");
            e.printStackTrace();
        }

    }

    /**
     * <></>
     * @param tableName
     */
    public void printTable(String tableName){

        try {
            String query = ("SELECT * FROM "+tableName);
            rs = st.executeQuery(query);

            System.out.println("Table (" + tableName + "):"); // title
            System.out.println("+ " + pad("") + "+ " + pad("") + "+"); // top divider
            System.out.println("| " + pad("Error") + "| " + pad("Repetitions") + "|"); // headers
            System.out.println("+ " + pad("") + "+ " + pad("") + "+"); // top divider

            while (rs.next()){ // print every record
                String err = rs.getString("error_type");
                String rep = Integer.toString(rs.getInt("repetitions")); //rounds to int
                System.out.println("| " + pad(err) + "| " + pad(rep) + "|");
            }
            System.out.println("+ " + pad("") + "+ " + pad("") + "+"); // bottom divider
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // this method takes a String, converts it into an array of bytes;
    // copies those bytes into a bigger byte array (STR_SIZE worth), and
    // pads any remaining bytes with spaces. Finally, it converts the bigger
    // byte array back into a String, which it then returns.
    // e.g. if the String was "s_name", the new string returned is
    // "s_name                    " (the six characters followed by 18 spaces).
    private String pad(String in) {
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
