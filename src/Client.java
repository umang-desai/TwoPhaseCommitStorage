import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.UUID;

public class Client {

	private String scriptPath;
	private String m_ip;
	private String m_port;

	public Client(String m_ip, String m_port) {
		this.m_ip = m_ip;
		this.m_port = m_port;
	}

	public Client(String scriptPath) {
		this.scriptPath = scriptPath;
	}

	public void run() throws NotBoundException, SQLException, IOException, RemoteException {
		Scanner scan = new Scanner(System.in);
		MasterInterface master = fetchServerInterface(m_ip,
				Integer.parseInt(m_port), "master");
		while (true) {
			print("-----------------------------------------------------------------------");
			print("Commands available are: GET <Key>, PUT <Key> <Value>, DEL <Key> <Value>");
			String input = scan.nextLine();
			// Analyze command and take action.
			String[] splitCommand = input.split(" ");
			String command = splitCommand[0];

			// TODO maybe add a mechanism so put/del return a boolean to
			// indicate success/failure.

			UUID tid = UUID.randomUUID();
			boolean result = true;
			if (command.equalsIgnoreCase("get")) {
				if (splitCommand.length == 2) {
					String value = master.get(splitCommand[1]);
					print(value);
				} else {
					print("GET command needs one argument, Key");
				}
			} else if (command.equalsIgnoreCase("put")) {
				if (splitCommand.length == 3) {
					result = master.put(tid.toString(), splitCommand[1],
							splitCommand[2]);
				} else {
					print("PUT command needs two arguments, Key Value");
				}
			} else if (command.equalsIgnoreCase("del")) {
				if (splitCommand.length == 2) {
					result = master.del(tid.toString(), splitCommand[1]);
				} else {
					print("DEL command needs one argument, Key");
				}
			} else {
				print("Commands available are: GET <Key>, PUT <Key> <Value>, DEL <Key> <Value> \n Please try again.\n");
			}
			if (!result)
				print("Could not complete " + command
						+ " request. Please try again later.");
		}
	}

	public static MasterInterface fetchServerInterface(String ip, int port,
			String id) throws RemoteException, NotBoundException {
		Registry reg = LocateRegistry.getRegistry(ip, port);
		MasterInterface node = (MasterInterface) reg.lookup(id);
		return node;
	}

	public static void main(String[] args) throws InterruptedException,
			NotBoundException, SQLException, IOException {

		String m_ip = args[0];
		String m_port = args[1];

		Client client = new Client(m_ip, m_port);
		while (true) {
			try {
				client.run();
			} catch (Exception ex) {
				print("Connection to Master is down. Will go to sleep for 3 seconds.");
				Thread.sleep(3000);
				print("Lets try again, shall we?");
			}
		}

	}

	public static void print(String str) {
		System.out.println(str);
	}
}
