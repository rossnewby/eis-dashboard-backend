/**
 * The server interface provides a description of the remote methods available as part of the service provided by the remote
 * object serverImpl
 *
 * @Author Ross Newby
 */
import org.json.JSONObject;

import java.rmi.*;

public interface ServerInterface extends Remote{

    /*Submit CKAN HTTP request; acquire response as JSON string*/
    public JSONObject ckanRequest(String url) throws RemoteException;
}
