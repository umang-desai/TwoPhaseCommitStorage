import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Master extends UnicastRemoteObject implements MasterInterface {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4444715532660516786L;
	private Connection con;
	private DataStoreAccess dt;
	private ReplicaRegistry registry;
	private File staging_2pc;
	private File commit_2pc;
	private ArrayList<String> key_lock;

	public Master(Connection con, String logfile) throws IOException,
			NumberFormatException, NotBoundException {
		this.con = con;
		this.dt = new DataStoreAccess(con);

		this.staging_2pc = new File("staging_" + logfile);
		if (!staging_2pc.exists())
			staging_2pc.createNewFile();

		this.commit_2pc = new File("commit_" + logfile);
		if (!commit_2pc.exists())
			commit_2pc.createNewFile();

		this.registry = new ReplicaRegistry();
		this.key_lock = new ArrayList<String>();

		recover();
	}

	/**
	 * Exposed to: Client
	 */
	@Override
	public String get(String key) throws RemoteException, SQLException {
		try {
			ReplicaInterface replica = registry.getRandomReplica();
			return replica.get(key);
		} catch (Exception ex) {

		}
		return dt.get(key);
	}

	/**
	 * Exposed to: Client
	 * 
	 * @throws IOException
	 */
	@Override
	public boolean put(String tid, String key, String value)
			throws SQLException, IOException {
		if (key_lock.contains(key))
			return false;
		else
			key_lock.add(key);

		// Log the transaction
		String command = "put";

		// Stage changes
		try {
			write(tid + " " + command + " " + key + " " + value + "\n",
					staging_2pc);
			// Ask replicas if can commit. Confirm all votes.
			boolean canCommit = askAllCanCommit(tid, command, key, value);

			// If all votes in, commit
			if (canCommit) {
				write(tid + " " + command + " " + key + " " + value + " "
						+ "COMMIT" + "\n", commit_2pc);
				dt.put(key, value);
				// and ask replicas to commit.
				doAllCommit(tid, command, key, value);
				key_lock.remove(key);

				return true;
			} else {
				write(tid + " " + command + " " + key + " " + value + " "
						+ "ABORT" + "\n", commit_2pc);
				doAllAbort(tid, command, key, value);
				key_lock.remove(key);
				return false;
			}
		} catch (Exception ex) {
			
		}
		return false;

		// doAllCommit();
		// If failures in votes, ask replicas to abort.

		// If all votes success but replica doesnt get final commit command,
		// launch enquiry from replica.
	}

	/**
	 * Exposed to: Client
	 * 
	 * @throws IOException
	 */
	@Override
	public boolean del(String tid, String key) throws SQLException, IOException {
		if (key_lock.contains(key))
			return false;
		else
			key_lock.add(key);
		try {
			String command = "del";
			write(tid + " " + command + " " + key, staging_2pc);
			// Ask replicas if can commit. Confirm all votes.
			boolean canCommit = askAllCanCommit(tid, command, key, null);

			// If all votes in, commit
			if (canCommit) {
				write(tid + " " + command + " " + key + " " + "COMMIT" + "\n",
						commit_2pc);
				dt.del(key);
				// and ask replicas to commit.
				doAllCommit(tid, command, key, null);
				key_lock.remove(key);
				return true;
			} else {
				write(tid + " " + command + " " + key + " " + "ABORT" + "\n",
						commit_2pc);
				doAllAbort(tid, command, key, null);
				key_lock.remove(key);
				return false;
			}
		} catch (Exception ex) {
			print(ex.getMessage());
		} finally {

		}
		return false;
	}

	/**
	 * Exposed to: Replica
	 * 
	 * @throws NotBoundException
	 * @throws IOException
	 */
	@Override
	public void join(String host, int port) throws NotBoundException,
			IOException {
		registry.addReplica(host, port);

		// TODO create log which keeps the replica information saved incase
		// master crashes and has to recover while replicas are still up.
		// DONE

		print(host + ":" + port + ", has joined the replica network.");
	}

	private void recover() throws IOException {
		// Fetch transaction data from both files.
		FileReader fileReader = new FileReader(staging_2pc);
		// Always wrap FileReader in BufferedReader.
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		HashMap<String, String> map_s = new HashMap<String, String>();
		while ((line = bufferedReader.readLine()) != null) {
			String[] split = line.split(" ", 2);
			map_s.put(split[0], split[1]);
		}
		// Always close files.
		fileReader.close();
		bufferedReader.close();

		// Fetch transaction data from both files.
		fileReader = new FileReader(commit_2pc);

		// Always wrap FileReader in BufferedReader.
		bufferedReader = new BufferedReader(fileReader);
		line = null;
		HashMap<String, String> map_c = new HashMap<String, String>();
		while ((line = bufferedReader.readLine()) != null) {
			String[] split = line.split(" ", 2);
			map_c.put(split[0], split[1]);
		}
		// Always close files.
		fileReader.close();
		bufferedReader.close();
		ArrayList<String> recover = new ArrayList<String>();

		// We match the id's
		Set<String> set_s = map_s.keySet();
		Set<String> set_c = map_c.keySet();

		for (String string : set_s) {
			if (!set_c.contains(string)) {
				recover.add(string);
			}
		}
		// Erase Log.
		PrintWriter writer = new PrintWriter(staging_2pc);
		writer.print("");
		writer.close();
		writer = new PrintWriter(commit_2pc);
		writer.print("");
		writer.close();
	}

	@Override
	public String getPhase2Decision(String tid) throws IOException {
		// Fetch transaction data from both files.
		FileReader fileReader = new FileReader(commit_2pc);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		// Always wrap FileReader in BufferedReader.
		bufferedReader = new BufferedReader(fileReader);
		String line = null;

		HashMap<String, String> map_c = new HashMap<String, String>();
		while ((line = bufferedReader.readLine()) != null) {
			String[] split = line.split(" ", 2);
			map_c.put(split[0], split[1]);
		}
		// Always close files.
		bufferedReader.close();

		String s = map_c.get(tid);
		String[] split = s.split(" ");

		return split[split.length - 1];

		// This process could be more efficient. Lack of time have my hands
		// tied. :(
	}

	// ----------NON RMI methods-------------

	private boolean askAllCanCommit(String tid, String command, String key,
			String value) throws SQLException, IOException {
		boolean canCommit = false;
		Iterator<ReplicaRegistry.ReplicaBundle> iter = registry
				.replicaIterator();
		while (iter.hasNext()) {
			ReplicaRegistry.ReplicaBundle bundle = iter.next();
			ReplicaInterface replica = bundle.getReplica();
			canCommit = replica.canCommit(tid, command, key, value);
			if (!canCommit)
				break;
		}
		return canCommit;
	}

	private void doAllCommit(String tid, String command, String key,
			String value) throws SQLException, IOException {

		Iterator<ReplicaRegistry.ReplicaBundle> iter = registry
				.replicaIterator();
		while (iter.hasNext()) {
			ReplicaRegistry.ReplicaBundle bundle = iter.next();
			ReplicaInterface replica = bundle.getReplica();
			replica.doCommit(tid, command, key, value);
		}
	}

	private void doAllAbort(String tid, String command, String key, String value)
			throws IOException {
		Iterator<ReplicaRegistry.ReplicaBundle> iter = registry
				.replicaIterator();
		while (iter.hasNext()) {
			ReplicaRegistry.ReplicaBundle bundle = iter.next();
			ReplicaInterface replica = bundle.getReplica();
			replica.doAbort(tid, command, key, value);
		}
	}

	private void write(String text, File file) throws IOException {
		FileWriter fw;
		BufferedWriter bw;
		fw = new FileWriter(file.getAbsoluteFile(), true);
		bw = new BufferedWriter(fw);
		bw.write(text);
		bw.flush();
		bw.close();
	}

	public static void main(String[] args) throws SQLException, IOException,
			NumberFormatException, NotBoundException {
		// Register MASTER on rmi registry. This will be exposed to client and
		// replica.

		String port = args[0];
		String logfile = args[1];
		String sqlUrl = "jdbc:sqlite:" + args[2];
		Connection con = DriverManager.getConnection(sqlUrl);

		String createTable = "CREATE TABLE IF NOT EXISTS data "
				+ "(key text PRIMARY KEY,value text);";
		Statement stmt = con.createStatement();
		stmt.execute(createTable);

		Master master = new Master(con, logfile);

		MasterInterface masterInt = (MasterInterface) master;
		RMIFactory.registerPort(Integer.parseInt(port), "master", masterInt);

		print("Master launched on IP: " + InetAddress.getLocalHost().getHostAddress() + ", port: " + port
				+ ". Ready to accept requests.");
	}

	public static void print(String str) {
		System.out.println(str);
	}
}
