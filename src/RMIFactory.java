
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class RMIFactory {
	public static void registerPort(int port, String id, MasterInterface obj) throws RemoteException{
		Registry reg = LocateRegistry.createRegistry(port);
		reg.rebind(id, obj);
	}
	
	public static void registerPort(int port, String id, ReplicaInterface obj) throws RemoteException{
		Registry reg = LocateRegistry.createRegistry(port);
		reg.rebind(id, obj);
	}
	
	public static MasterInterface fetchServerInterface(String ip, int port, String id)
			throws RemoteException, NotBoundException {
		Registry reg = LocateRegistry.getRegistry(ip, port);
		MasterInterface node = (MasterInterface) reg.lookup(id);
		return node;
	}
	
	public static ReplicaInterface fetchReplicaInterface(String ip, int port, String id)
			throws RemoteException, NotBoundException {
		Registry reg = LocateRegistry.getRegistry(ip, port);
		ReplicaInterface node = (ReplicaInterface) reg.lookup(id);
		return node;
	}
}
