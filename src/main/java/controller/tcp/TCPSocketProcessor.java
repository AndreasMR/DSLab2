package controller.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import controller.node.NodeInfo;
import controller.node.NodeManager;
import controller.user.UserInfo;
import controller.user.UserManager;

public class TCPSocketProcessor implements Runnable{
	
	private Socket socket;
	private TCPSocketManager socketManager;
	private UserManager userManager;
	private NodeManager nodeManager;

	public TCPSocketProcessor( Socket socket, TCPSocketManager socketManager, UserManager userManager, NodeManager nodeManager){
		this.socket = socket;
		this.socketManager = socketManager;
		this.userManager = userManager;
		this.nodeManager = nodeManager;
	}
	
	@Override
	public void run() {
		
		//Map to store Sockets for nodes, so the client can issue commands to nodes independently from other clients
//		Map<NodeInfo, Socket> nodeSockets = new HashMap<NodeInfo, Socket>();
		
		try{

			int port = socket.getPort();

			// prepare the input reader for the socket
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// prepare the writer for responding to clients requests
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

			String request;
			String response = "";
			
			// read client requests
			while ((request = reader.readLine()) != null) {
				
				//get user by port, if null is returned, no user has been logged in via the client on the port
				UserInfo user = userManager.getActiveUser(port);
				
				String[] parts = request.split("\\s+");

				if(parts.length == 1 && parts[0].equals("!exit")){
					
					userManager.deactivate(port);
					
					if(user != null){
						response = "Successfully logged out. Exiting client.";
					}else{
						response = "Exiting client.";
					}
					
					writer.println(response);
					
					break;
				}

				if(user == null){
					//client is not logged in
					
					if(parts.length == 3 && parts[0].equals("!login")){

						String name = parts[1];
						String password = parts[2];
						user = userManager.getRegisteredUser(name);

						if(user != null && user.checkPassword(password)){
							//Allow same user to log in via multiple clients at a time
							//Remove comments for this part if a user should only be able to use one client at a time
//							if(userManager.isActiveUser(user)){
//								response = "Error: The User is already logged in!";
//							}else{
								//Login
								userManager.activate(port, user);
								response = "Successfully logged in.";
//							}
						}else{
							response = "Error: Wrong username or password.";
						}

					}else{
						response = "Error: First you have to login via \"!login <username> <password>\".";
					}

				}else{ 
					//user is logged in
					
					if(parts.length > 3 && parts[0].equals("!compute")){

						if(parts.length % 2 != 0){
							response = "Error: Wrong input format for calculation.";

						}else if( ( ( parts.length / 2 ) - 1 ) * 50 > user.getCredits()){
							response = "Error: You do not have enough credits for this calculation.";

						}else{
							
							nodeManager.addOperatorStatistics(parts);
							
							int lostCredits = 0;

							int no1 = Integer.parseInt(parts[1]);

							for(int i = 3; i < parts.length; i+=2){

								char op = parts[i-1].charAt(0);
								int no2 = Integer.parseInt(parts[i]);

								String result = null;
								Socket nodeSocket = null;
								NodeInfo node = null;
								
								while((node = nodeManager.getNode(op)) != null){
									
									try{
//										nodeSocket = nodeSockets.get(node);
										
//										if(nodeSocket == null){
											nodeSocket = new Socket(node.getHostname(), node.getPort());
//											nodeSockets.put(node, nodeSocket);
//										}
										
										// create a reader to retrieve messages send by the node
										BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));

										// create a writer to send messages to the node
										PrintWriter nodeWriter = new PrintWriter(nodeSocket.getOutputStream(), true);

										nodeWriter.println("!calc " + no1 + " " + op + " " + no2);
										
										try{
											result = nodeReader.readLine();
											if(result == null)
												throw new SocketException("Result from Socket is null");
										}catch(SocketException ex){
											//Socket is not connected anymore (node was closed, but could be open again)
											//try to create a new socket
//											nodeSockets.remove(node);
											//try same node again
											continue;
										} finally {
											if(nodeSocket != null && !nodeSocket.isClosed()){
												nodeSocket.close();
											}
										}
										
									} catch (IOException e) {
										//It was not possible to properly connect to the node
										nodeManager.deactivate(node);
										//try next node
										continue;
									} finally {
										if(nodeSocket != null && !nodeSocket.isClosed()){
											nodeSocket.close();
										}
									}
									
									break;
								}
								
								if(node == null){
									//technical error, no operation node available -> subtract no credits
									//Do not add usage to the node
									response = "Error: No available node for requested operation.";
									lostCredits = 0;
									break;
								}

								lostCredits += 50;

								if(result.equals("!div0")){
									//Subtract credits for all operations including this one
									//Do not add usage to the node
									response = "Error: Division by 0.";
									break;

								}else{
									try{
										no1 = Integer.parseInt(result);
										//Successful Calculation
										response = result;
										node.addUseage(50 * result.length());

									}catch(NumberFormatException ex){
										//result may be conveying an error message.
										//No credit loss because of technical error
										//Do not add usage to the node
										response = "Error: Unexpected termination of calculation: " + result + ".";
										lostCredits = 0;
										break;
									}
								}
																
							}
							
							//in the end finally subtract credits from user
							user.addCredits(-lostCredits);
						}

					}else if(parts.length == 3 && parts[0].equals("!login")){
						response = "Error: You are already logged in!";

					}else if(parts.length == 2 && parts[0].equals("!buy")){
						int additionalCredits = 0;
						try{
							additionalCredits = Integer.parseInt(parts[1]);
							user.addCredits(additionalCredits);
							response = "You now have " + user.getCredits() + " credits.";

						}catch(NumberFormatException ex){
							response = "Error: The amount of credits to buy has to be numeric.";
						}

					}else if(parts.length == 1){

						if(parts[0].equals("!credits")){
							response = "You have " + user.getCredits() + " credits left.";

						}else if(parts[0].equals("!list")){
							response = ((nodeManager.getNode('+') != null) ? "+" : "")
									+ ((nodeManager.getNode('-') != null) ? "-" : "")
									+ ((nodeManager.getNode('*') != null) ? "*" : "")
									+ ((nodeManager.getNode('/') != null) ? "/" : "");

						}else if(parts[0].equals("!logout")){
							userManager.deactivate(port);
							response = "Successfully logged out.";

						}else{
							response = "Error: Please enter a valid command.";
						}
					}else{
						response = "Error: Please enter a valid command.";
					}
				}

				//print response
				writer.println(response);
			}

		}catch(IOException ex){
			//Proceed with shutdown of Thread
		}finally{
			socketManager.close(socket);
			
//			for(Socket s : nodeSockets.values()){
//				if (s != null && !s.isClosed())
//					try {
//						s.close();
//					} catch (IOException e) {
//						// Ignored because we cannot handle it
//					}
//			}
//			nodeSockets.clear();
			
			socket = null;
			socketManager = null;
			userManager = null;
			nodeManager = null;
		}
	}

}
