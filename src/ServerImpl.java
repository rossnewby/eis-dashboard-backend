import java.rmi.RemoteException;

/**
 * Contains the methods which can be remotely invoked
 *
 * @Author Ross Newby
 */
public class ServerImpl {

    //
    public int method () throws RemoteException{
        return 1;
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
