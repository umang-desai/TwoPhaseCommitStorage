

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface ReplicaInterface extends Remote{
	
	//Ask replica if they are ready for commit.
	public boolean canCommit(String tid, String command, String key, String value) throws RemoteException, SQLException, IOException;
	public boolean doCommit(String tid, String command, String key,
			String value) throws RemoteException, SQLException, IOException;
	public boolean doAbort(String tid, String command, String key,
			String value) throws RemoteException, IOException;
	public String get(String key) throws RemoteException, SQLException;
}
