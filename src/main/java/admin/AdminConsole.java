package admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import model.ComputationRequestInfo;
import util.Config;
import controller.IAdminConsole;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole implements IAdminConsole, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	
	private String rmiBindingName;
	private int rmiPort;
	private String cHost;
	private String keysDir;
	
	private Set<INotificationCallback> subscriptions = new HashSet<INotificationCallback>();

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public AdminConsole(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		
		rmiBindingName = config.getString("binding.name");
		rmiPort = config.getInt("controller.rmi.port");
		cHost = config.getString("controller.host");
		keysDir = config.getString("keys.dir");
		
	}

	@Override
	public void run() {
		
		userResponseStream.println("AdminConsole is up! Enter \"!exit\" to exit!");

		// create a new Reader to read commands from System.in
		BufferedReader reader = new BufferedReader( new InputStreamReader( userRequestStream ) );
		
		boolean exit = false;
		
		while(!exit){
			
			Registry r = null;
			IAdminConsole adminService = null;
			
			try {
				r = LocateRegistry.getRegistry(cHost, rmiPort);
				adminService = (IAdminConsole)r.lookup(rmiBindingName);
			} catch (RemoteException | NotBoundException e) {
//				e.printStackTrace();
				adminService = null;
			}
			
			if(adminService != null){
				userResponseStream.println("Successfully connected to AdminService. Enter valid commands.");
			}else{
				userResponseStream.println("Could not connect to AdminService, press Enter to try again.");
			}
			
			while(true){
				
				String request = null;
				
				try {
					request = reader.readLine();
				} catch (IOException e) {
					//Connection to userRequestStream lost, shut down AdminConsole
					exit = true;
					break;
				}
				
				if(request == null){
					//Connection to userRequestStream lost, shut down AdminConsole
					exit = true;
					break;
				}
				
//				try {
//					r = LocateRegistry.getRegistry(cHost, rmiPort);
//					adminService = (IAdminConsole)r.lookup(rmiBindingName);
//				} catch (RemoteException | NotBoundException e) {
////					e.printStackTrace();
//					adminService = null;
//				}
//				
//				if(adminService != null){
////					userResponseStream.println("Successfully connected to AdminService. Enter valid commands.");
//				}else{
//					userResponseStream.println("Could not connect to AdminService, try again.");
//					//Try to retrieve AdminService Object
//					continue;
//				}
				
				if(adminService == null){
					//Try to retrieve AdminService Object
					break;
				}
				
				String response = null;
				
				if(request.equals("!exit")){
					exit = true;
					break;
					
				}else if(request.equals("!getLogs")){
					
					List<ComputationRequestInfo> logs = null;
					try {
						logs = adminService.getLogs();
					} catch (RemoteException e) {
						//Try to retrieve AdminService Object
						userResponseStream.println("Error: Connection to AdminService lost.");
						break;
					}
					
					if(logs != null){
						for(ComputationRequestInfo log : logs){
							userResponseStream.println(log.getInfo());
						}
						
						if(logs.size() == 0){
							response = "No logs available.";
						}
					}
					
				}else if(request.equals("!statistics")){
					
					LinkedHashMap<Character, Long> stats = null;
					try {
						stats = adminService.statistics();
					} catch (RemoteException e) {
						//Try to retrieve AdminService Object
						userResponseStream.println("Error: Connection to AdminService lost.");
						break;
					}
					
					if(stats != null){
						for(Entry<Character, Long> e : stats.entrySet()){
							userResponseStream.println(e.getKey() + " " + e.getValue());
						}
						
						if(stats.entrySet().size() == 0){
							response = "No statistics available.";
						}
					}
					
				}else if(request.startsWith("!subscribe ")){
					String[] parts = request.split("\\s+");
					
					if(parts.length == 3){
						
						String user = parts[1];
						
						Integer credits = null;
						try{
							credits = Integer.parseInt(parts[2]);
						}catch(NumberFormatException ex){
							credits = null;
						}

						if(credits != null){
							try {
								INotificationCallback callback = new NotificationCallback(userResponseStream);
								UnicastRemoteObject.exportObject(callback, 0);
								subscriptions.add(callback);
								if(adminService.subscribe(user, credits, callback)){
									response = "Successfully subscribed for user " + user + ".";
								}else{
									response = "Error: Not successfully subscribed for user " + user + ".";
									UnicastRemoteObject.unexportObject(callback, true);
									subscriptions.remove(callback);
								}
							} catch (RemoteException e) {
								//Try to retrieve AdminService Object
								userResponseStream.println("Error: Connection to AdminService lost.");
								break;
							}
							
						}else{
							response = "Error: Please enter a valid command.";
						}
						
					}else{
						response = "Error: Please enter a valid command.";
					}
					
				}else{
					response = "Error: Please enter a valid command.";
				}
				
				if(response != null)
					userResponseStream.println(response);
				
			}

		}
		
		try {
			reader.close();
		} catch (IOException e1) {
			
		}
		
		userResponseStream.println("Shutting down AdminConsole..");
		
		//Cleanup before closing AdminConsole
		for(INotificationCallback s : subscriptions){
			try {
				UnicastRemoteObject.unexportObject(s, true);
			} catch (NoSuchObjectException e) {
				// no handling necessary
			}
		}
		subscriptions.clear();
		
	}

	@Override
	public boolean subscribe(String username, int credits, INotificationCallback callback) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public Key getControllerPublicKey() throws RemoteException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void setUserPublicKey(String username, byte[] key)
//			throws RemoteException {
//		// TODO Auto-generated method stub
//	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 */
	public static void main(String[] args) {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config("admin"), System.in, System.out);
		new Thread(adminConsole).start();
	}
}
