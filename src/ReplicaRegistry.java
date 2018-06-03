import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class ReplicaRegistry {
	private ArrayList<ReplicaBundle> replicaList;
	private File registryBackup;
	private String backupFilename = "master_registry_backup.txt";

	public ReplicaRegistry() throws IOException, NumberFormatException,
			NotBoundException {
		replicaList = new ArrayList<ReplicaBundle>();
		registryBackup = new File(backupFilename);
		if (!registryBackup.exists())
			registryBackup.createNewFile();
		else
			recover();	
	}

	public boolean addReplica(String ip, int port) throws NotBoundException,
			IOException {
		int remove = checkDupReplica(ip, port);
		if (remove > -1)
			replicaList.remove(remove);

		ReplicaInterface replica = RMIFactory.fetchReplicaInterface(ip, port,
				"replica");
		ReplicaBundle bundle = new ReplicaBundle(replica, ip, port);
		write(ip + " " + port + "\n", registryBackup);
		replicaList.add(bundle);
		return true;
	}

	private int checkDupReplica(String ip, int port) {
		for (int i = 0; i < replicaList.size(); i++) {
			ReplicaBundle bundle = replicaList.get(i);
			if (bundle.ip.equalsIgnoreCase(ip) && bundle.port == port)
				return i;
		}
		return -1;
	}

	public ReplicaInterface getRandomReplica() {
		int index = 0;

		if (replicaList.size() == 0) {
			return null;
		} else if (replicaList.size() == 1) {
			index = 0;
		} else {
			Random random = new Random();
			index = random.nextInt(replicaList.size());
		}
		return replicaList.get(index).getReplica();
	}

	private void recover() throws IOException, NumberFormatException,
			NotBoundException {
		// FileReader reads text files in the default encoding.
		FileReader fileReader = new FileReader(registryBackup);

		// Always wrap FileReader in BufferedReader.
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = null;
		ArrayList<String> list = new ArrayList<String>();
		while ((line = bufferedReader.readLine()) != null) {
			list.add(line);	
		}
		// Always close files.
		bufferedReader.close();

		PrintWriter writer = new PrintWriter(registryBackup);
		writer.print("");
		writer.close();
		
		for(String conInfo : list){
		String[] split = conInfo.split(" ");
		try {
			this.addReplica(split[0], Integer.parseInt(split[1]));
		} catch (Exception ex) {
			print(ex.getMessage());
		}
		}
	}

	public Iterator<ReplicaBundle> replicaIterator() {
		return replicaList.iterator();
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

	// TODO potentially add counter to check for timeout threshold.
	class ReplicaBundle {
		private ReplicaInterface replica;
		private String ip;
		private int port;

		public ReplicaBundle(ReplicaInterface replica, String ip, int port) {
			this.replica = replica;
			this.ip = ip;
			this.port = port;
		}

		public ReplicaInterface getReplica() {
			return replica;
		}

		public String getIp() {
			return ip;
		}

		public int getPort() {
			return port;
		}
	}

	public static void print(String str) {
		System.out.println(str);
	}
}
