import java.rmi.*;

/**
 * The server interface provides a description of the remote methods available as part of the service provided by the remote
 * object serverImpl
 *
 * @Author Ross Newby
 */
public interface ServerInterface extends Remote{

    /**
     * Submit CKAN HTTP request with automatic basic authentication and API header; specified by config file
     * @param url Desired ckan url, excluding or including 'https://'
     * @return This returns the CKAN response as a JSONObject
     * */
    //public JSONObject requestJSON(String url) throws RemoteException;

    public String[] getBuildingList()throws RemoteException;
}
