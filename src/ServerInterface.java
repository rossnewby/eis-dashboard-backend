/**
 * The server interface provides a description of the remote methods available as part of the service provided by the remote
 * object serverImpl
 *
 * @Author Ross Newby
 */
import java.rmi.*;

public interface ServerInterface extends Remote{

    //
    public int method () throws RemoteException;

    //
    public int method2 () throws RemoteException;

    //
    public int method3 () throws RemoteException;
}
