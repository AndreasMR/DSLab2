package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

import util.Config;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	
	private Socket socket;
	
	private String cHost;
	private int cTCPPort;

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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		this.cHost = config.getString("controller.host");
		this.cTCPPort = config.getInt("controller.tcp.port");
		
	}

	@Override
	public void run() {
		
		userResponseStream.println("Client is up! Enter \"!exit\" to exit!");

		// create a new Reader to read commands from System.in
		BufferedReader reader = new BufferedReader( new InputStreamReader( userRequestStream ) );
		
		boolean exit = false;
		while(!exit){
			
			if(connectToController()){
				
				BufferedReader serverReader = null;
				PrintWriter serverWriter = null;
				
				try {
					// create a reader to retrieve messages send by the server
					serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					// create a writer to send messages to the server
					serverWriter = new PrintWriter(socket.getOutputStream(), true);

					userResponseStream.println("Successfully connected to Controller. Enter valid commands.");
					
					// read commands on the client side
					while (true){
						String request = reader.readLine();
						
						if(request == null){
							request = "!exit";
						}
						
						// write provided user input to the socket
						serverWriter.println(request);
						
						// read server response and write it to console
						String response = serverReader.readLine();
						
						if(response == null){
							throw new IOException("Connection to controller lost");
						}
						
						userResponseStream.println(response);
						
						if(request.equals("!exit")){
							exit = true;
							break;
						}
					}
										
				} catch (IOException e) {
					// connection to controller lost, try to reconnect
					continue;
				}
				
			}else{
				userResponseStream.println("Could not connect to Controller, press Enter to try again.");
				try {
					String request = reader.readLine();
					if(request == null || request.equals("!exit")){
						exit = true;
					}
				} catch (IOException e) {
					//the client was closed
					e.printStackTrace();
					exit = true;
				}
			}
		}
		
		try {
			reader.close();
		} catch (IOException e1) {
			
		}
		
		userResponseStream.println("Shutting down client..");
		
		// close socket
		try {
			exit();
		} catch (IOException e) {
			// Ignored because we cannot handle it
		}
	}
	
	public boolean connectToController(){
		// close socket
		try {
			exit();
		} catch (IOException e) {
			// Ignored because we cannot handle it
		}
		
		// create and start a new TCP ServerSocket
		try {
			socket = new Socket(cHost, cTCPPort);

		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	public String login(String username, String password) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String logout() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String credits() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String buy(long credits) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String compute(String term) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exit() throws IOException {
		if(socket != null && !socket.isClosed()){
			socket.close();
			socket = null;
		}
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in, System.out);
		new Thread(client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
