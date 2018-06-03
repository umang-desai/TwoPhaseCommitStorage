

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface MasterInterface extends Remote{
	
	public String get(String key) throws RemoteException, SQLException;
	public boolean put(String tid, String key, String value) throws RemoteException, SQLException, IOException;
	public boolean del(String tid, String key) throws RemoteException, SQLException, IOException;
	public void join(String host, int port) throws RemoteException, NotBoundException, IOException;
	public String getPhase2Decision(String tid) throws RemoteException, IOException;
}
