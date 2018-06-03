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
import java.util.Set;

public class Replica extends UnicastRemoteObject implements ReplicaInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5330051950589903193L;
	private Connection con;
	private DataStoreAccess dt;
	private MasterInterface master;
	private File staging_2pc;
	private File commit_2pc;

	public Replica(Connection con, String logfile, MasterInterface master)
			throws IOException, SQLException {
		this.con = con;
		this.dt = new DataStoreAccess(con);
		this.staging_2pc = new File("staging_" + logfile);
		if (!staging_2pc.exists())
			staging_2pc.createNewFile();
		
		this.commit_2pc = new File("commit_" + logfile);
		if (!commit_2pc.exists())
			commit_2pc.createNewFile();
		
		this.master = master;
		recover();
	}

	@Override
	public boolean canCommit(String tid, String command, String key,
			String value) throws SQLException, IOException {
		if (con.isValid(1)) {
			// Log the transactions to commit.
			write(tid + " " + command + " " + key + " " + value + "\n",
					staging_2pc);
			return true;
		}
		return false;
	}

	@Override
	public boolean doCommit(String tid, String command, String key, String value)
			throws SQLException, IOException {

		if (command.equalsIgnoreCase("put")) {
			write(tid + " " + command + " " + key + " " + value + " "
					+ "COMMIT" + "\n", commit_2pc);
			put(key, value);
		} else {
			write(tid + " " + command + " " + key + " " + "COMMIT" + "\n",
					commit_2pc);
			del(key);
		}
		return true;
	}

	@Override
	public boolean doAbort(String tid, String command, String key, String value)
			throws IOException {
		write(tid + " " + command + " " + key + " " + value + " " + "ABORT"
				+ "\n", commit_2pc);
		return true;
	}

	@Override
	public String get(String key) throws RemoteException, SQLException {
		return dt.get(key);
	}

	public void put(String key, String value) throws SQLException {
		dt.put(key, value);
	}

	public void del(String key) throws SQLException {
		dt.del(key);
	}

	private void recover() throws IOException, SQLException {
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

		for (String tid : recover) {
			String decision = master.getPhase2Decision(tid);
			if (decision.equalsIgnoreCase("commit")) {
				String command;
				String key;
				String value = null;

				String trans = map_s.get(tid);
				String[] split = trans.split(" ");
				command = split[0];
				key = split[1];
				if (command.equalsIgnoreCase("put")) {
					value = split[2];

					if (value != null) {
						write(tid + " " + command + " " + key + " " + value
								+ " " + "COMMIT" + "\n", commit_2pc);
						dt.put(key, value);
					}
				} else {
					write(tid + " " + command + " " + key + " " + value + " "
							+ "ABORT" + "\n", commit_2pc);

					dt.del(key);
				}

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

	private void write(String text, File file) throws IOException {
		FileWriter fw;
		BufferedWriter bw;
		fw = new FileWriter(file.getAbsoluteFile(), true);
		bw = new BufferedWriter(fw);
		bw.write(text);
		bw.flush();
		bw.close();
	}

	// ----------------------------------------------
	/**
	 * 
	 * @param args
	 *            MasterIP MasterPort SelfPort logPath dbPath
	 * @throws SQLException
	 * @throws IOException
	 * @throws NotBoundException
	 */
	public static void main(String[] args) throws SQLException, IOException,
			NotBoundException {
		String m_ip = args[0];
		int m_port = Integer.parseInt(args[1]);
		int self_port = Integer.parseInt(args[2]);
		String logfile = args[3];
		String sqlUrl = "jdbc:sqlite:" + args[4];

		Connection con = DriverManager.getConnection(sqlUrl);

		String createTable = "CREATE TABLE IF NOT EXISTS data "
				+ "(key text PRIMARY KEY,value text);";
		Statement stmt = con.createStatement();
		stmt.execute(createTable);

		MasterInterface master = RMIFactory.fetchServerInterface(m_ip, m_port,
				"master");

		Replica replica = new Replica(con, logfile, master);
		ReplicaInterface replicaInt = (ReplicaInterface) replica;
		RMIFactory.registerPort(self_port, "replica", replicaInt);

		String self_ip = InetAddress.getLocalHost().getHostAddress();

		master.join(self_ip, self_port);
	}
}
