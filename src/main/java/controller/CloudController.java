package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import util.Config;
import controller.node.NodeInfo;
import controller.node.NodeManager;
import controller.rmi.AdminService;
import controller.tcp.TCPListenerThread;
import controller.udp.UDPListenerThread;
import controller.user.UserInfo;
import controller.user.UserManager;
import controller.user.UserNameComparator;

public class CloudController implements ICloudControllerCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	
	private int tcpPort; //TCP port on which to listen
	private int udpPort; //UDP port on which to listen
	private int nodeTimeout; //time in ms after which a node is set offline
	private int nodeCheckPeriod; //period in ms to check for timeouts
    private int rmax; //maximum resources available to nodes
    private String rmiBindingName;
	private int rmiPort;
	
	private NodeTimeoutThread nodeTimeoutThread = null;
	private DatagramSocket datagramSocket = null;
	private ServerSocket serverSocket = null;
	
	private NodeManager nodeManager = null;
	private UserManager userManager = null;
	
	private Registry reg;
	private AdminService adminService;

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
	public CloudController(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		
		//Read Controller properties from property file
		tcpPort = config.getInt( "tcp.port" );
		udpPort = config.getInt( "udp.port" );
		nodeTimeout = config.getInt( "node.timeout" );
		nodeCheckPeriod = config.getInt( "node.checkPeriod" );
        rmax = config.getInt("controller.rmax");
        rmiBindingName = config.getString("binding.name");
		rmiPort = config.getInt("controller.rmi.port");
		
		nodeManager = new NodeManager(rmax);
		userManager = new UserManager();
		
	}

	@Override
	public void run() {
			
		// create a new thread to check for Timeout of Nodes
		nodeTimeoutThread = new NodeTimeoutThread(nodeManager, nodeTimeout, nodeCheckPeriod);
		nodeTimeoutThread.start();
		
		try {
			// constructs a datagram socket and binds it to the specified port
			datagramSocket = new DatagramSocket(udpPort);
			
			// create a new thread to listen for incoming packets
			new UDPListenerThread(datagramSocket, nodeManager).start();
			
		} catch (IOException e) {
			throw new RuntimeException("Cannot listen on UDP port.", e);
		}
		
		// create and start a new TCP ServerSocket
		try {
			serverSocket = new ServerSocket(tcpPort);
			
			// handle incoming connections from client in a separate thread
			new TCPListenerThread(serverSocket, userManager, nodeManager, config, userResponseStream).start();
			
		} catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}

		// create Registry and set up RMI Service
		try {
			reg = LocateRegistry.createRegistry(rmiPort);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		adminService = new AdminService(userManager, nodeManager);
		
		try {
			Remote adminServiceStub = UnicastRemoteObject.exportObject(adminService, 0);
			reg.rebind(rmiBindingName, adminServiceStub);
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}

		userResponseStream.println("Server is up! Enter \"!exit\" to exit!");
		
		// create a new Reader to read commands from System.in
		BufferedReader reader = new BufferedReader( new InputStreamReader( userRequestStream ) );
		
		String request = "";
		
		try {
			// read commands on the server side
			while ((request = reader.readLine()) != null) {
				if(request.equals("!nodes")){
					userResponseStream.println(nodes());
				}else if(request.equals("!users")){
					userResponseStream.println(users());
				}else if(request.equals("!exit")){
					break;
				}
			}
			
			reader.close();
			
		} catch (IOException e) {
			// IOException from System.in is very very unlikely (or impossible)
			// and cannot be handled
		}

		userResponseStream.println("Shutting down Controller..");
		
		// close socket and listening thread
		try {
			exit();
		} catch (IOException e) {
			// Ignored because we cannot handle it
		}
	}

	@Override
	public String nodes() throws IOException {
		String ret = "";
		NodeInfo[] nodes = nodeManager.getNodes();
		for(int i = 0; i < nodes.length; i++){
			
			if(!ret.equals(""))
				ret += "\n";
			
			NodeInfo n = nodes[i];
			ret += (i+1) + ". IP: " + n.getIP() 
						 + " Port: " + n.getPort() 
					     + (n.isActive() ? " online " : " offline ") 
					     + "Usage: " + n.getUseage();
		}
		return ret;
	}

	@Override
	public String users() throws IOException {
		String ret = "";
		UserInfo[] users = userManager.getUsers();
		Arrays.sort(users, new UserNameComparator());
		for(int i = 0; i < users.length; i++){
			
			if(!ret.equals(""))
				ret += "\n";
			
			UserInfo u = users[i];
			ret += (i+1) + ". " + u.getName()
						 + (userManager.isActiveUser(u) ? " online " : " offline ") 
						 + "Credits: " + u.getCredits();
		}
		return ret;
	}

	@Override
	public String exit() throws IOException {		
		/*
		 * Note that closing a socket also triggers an exception in the
		 * listening thread
		 */
		
		if (nodeTimeoutThread != null)
			nodeTimeoutThread.exit();
		
		//Close UDP Socket
		if (datagramSocket != null)
			datagramSocket.close();

		//Close TCP Socket
		if (serverSocket != null){
			serverSocket.close();
		}
		
		//Unexport AdminService
		UnicastRemoteObject.unexportObject(adminService, true);
		
		//Unbind RMI Service
		try {
			reg.unbind(rmiBindingName);
		} catch (NotBoundException e) {
			// no handling necessary
		}
				
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) {
		CloudController cloudController = new CloudController(args[0], 
						new Config("controller"), System.in, System.out);
		new Thread(cloudController).start();
	}
	
	private class NodeTimeoutThread extends Thread {
		
		boolean exit = false;
		
		private NodeManager nodeManager = null;
		private long nodeTimeout = 0;
		private long nodeCheckPeriod = 0;
		
		public NodeTimeoutThread ( NodeManager nodeManager, int nodeTimeout, int nodeCheckPeriod ) {
			this.nodeManager = nodeManager;
			this.nodeTimeout = nodeTimeout;
			this.nodeCheckPeriod = nodeCheckPeriod;
		}
		
		public void run(){
			try {
				
				long time = 0;
				
				while( !exit ) {
					
					time = System.currentTimeMillis();
					
					NodeInfo[] nodes = nodeManager.getNodes();
					for(NodeInfo n : nodes){
						if( n.isActive() && time - n.getLastAlive() > nodeTimeout ){
							nodeManager.deactivate(n);
						}
					}
					
					Thread.sleep(nodeCheckPeriod);
					
				}
				
			} catch (InterruptedException e) {
				//let the thread run out
			}
		}
		
		public void exit(){
			this.exit = true;
			this.interrupt();
		}
	}

}
