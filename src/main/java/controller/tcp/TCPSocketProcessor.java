package controller.tcp;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import util.Config;
import util.Keys;
import cli.Base64Channel;
import cli.HmacChannel;
import cli.TamperedException;
import cli.TcpChannel;
import controller.node.NodeInfo;
import controller.node.NodeManager;
import controller.user.UserInfo;
import controller.user.UserManager;

public class TCPSocketProcessor implements Runnable{
	
	private TcpChannel tcpChannel;
	private Base64Channel base64Channel;
	private Socket socket;
	private TCPSocketManager socketManager;
	private UserManager userManager;
	private NodeManager nodeManager;
	private Config config;
	private PrivateKey controller_key;
	private PublicKey user_pubkey;
	private Key secret_hmac_key;

	public TCPSocketProcessor( Socket socket, TCPSocketManager socketManager, UserManager userManager, NodeManager nodeManager, Config config) throws IOException{
		this.socket = socket;
		tcpChannel = new TcpChannel(socket);
		base64Channel = new Base64Channel(tcpChannel);
		this.socketManager = socketManager;
		this.userManager = userManager;
		this.nodeManager = nodeManager;
		Security.addProvider(new BouncyCastleProvider());
		this.config = config;
		try {
			this.controller_key = Keys.readPrivatePEM(new File(config.getString("key")));
		} catch (IOException e) {
			this.controller_key = null;
			e.printStackTrace();
		}
		try {
			this.secret_hmac_key = Keys.readSecretKey(new File(config.getString("hmac.key")));
		} catch (IOException e) {
			this.secret_hmac_key = null;
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		//Map to store Sockets for nodes, so the client can issue commands to nodes independently from other clients
//		Map<NodeInfo, Socket> nodeSockets = new HashMap<NodeInfo, Socket>();
		
		try{

			int port = socket.getPort();


			final String B64 = "a-zA-Z0-9/+";
			SecureRandom secureRandom = null;
			String request = "";
			byte[] message = null;
			String response = "";
			String name = "";
			KeyGenerator generator;
			//final String B64 = "a-zA-Z0-9/+";
			Cipher cipher = null;
			Cipher aes_encryption = null;
			Cipher aes_decryption = null;
			
			// read client requests
			while ((message = base64Channel.receiveMessageLineInBytes()) != null) {
			//--while ((request = reader.readLine()) != null) {

				//get user by port, if null is returned, no user has been logged in via the client on the port
				UserInfo user = userManager.getActiveUser(port);
				
				if (user == null){
					byte[] encrypted_message = message;
					byte[] decrypted_message = null;
					byte[] controller_challenge = null;
					byte[] client_challenge = null;
					byte[] iv_vector = null;
					byte[] aes_key = null;
					byte[] auth_response = null;
					
					// init the cipher
					cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
					cipher.init(Cipher.DECRYPT_MODE, controller_key);
					
					// decrypt the message with doFinal
					decrypted_message = cipher.doFinal(encrypted_message);
					request = new String(decrypted_message);
					
					String[] parts = request.split("\\s+");
					
					if(parts.length == 3 && parts[0].equals("!authenticate")){
						try {
							// There have to exist a public key file for the user, so we can encrypt the message, that is sent
							this.user_pubkey = Keys.readPublicPEM(new File(config.getString("keys.dir")+"/"+parts[1]+".pub.pem"));
						} catch (Exception e) {
							this.user_pubkey = null;
							System.err.println(e.getMessage());
						}
						if (this.user_pubkey != null){
							name = parts[1];
							secureRandom = new SecureRandom();
							final byte[] challenge = new byte[32];
							final byte[] iv = new byte[16];
							
							// get random byte arrays
							secureRandom.nextBytes(challenge);
							secureRandom.nextBytes(iv);
							
							// challenge of the controller
							controller_challenge = Base64.encode(challenge);
							// challenge of the client
							client_challenge = parts[2].getBytes();
							iv_vector = Base64.encode(iv);
							generator = KeyGenerator.getInstance("AES");
							// KEYSIZE is in bits
							generator.init(256);
							// generate the aes secret key
							SecretKey key = generator.generateKey(); 
							aes_key = Base64.encode(key.getEncoded());
							
							// putting everything into one message in bytes
							response = "!ok ";
							auth_response = new byte[response.length()+client_challenge.length+controller_challenge.length+aes_key.length+iv_vector.length+3];
							int i;
							auth_response[0] = '!';auth_response[1] = 'o'; auth_response[2] = 'k'; auth_response[3] = ' ';
							i = 4;
							for (int x = 0; x < client_challenge.length; x++)
								auth_response[i++] = client_challenge[x];
							auth_response[i++] = ' ';
							for (int y = 0; y < controller_challenge.length; y++)
								auth_response[i++] = controller_challenge[y];
							auth_response[i++] = ' ';
							for (int z = 0; z < aes_key.length; z++)
								auth_response[i++] = aes_key[z];
							auth_response[i++] = ' ';
							for (int u = 0; u < iv_vector.length; u++)
								auth_response[i++] = iv_vector[u];
							
							// encrypt the message 
							cipher.init(Cipher.ENCRYPT_MODE, user_pubkey);
							auth_response = cipher.doFinal(auth_response);
							
							// send the message and waiting for an answer 
							base64Channel.sendMessageLineInBytes(auth_response);
							message = base64Channel.receiveMessageLineInBytes();
							
							if (message != null){
								// answer is aes encrypted
								encrypted_message = message;
								aes_decryption = Cipher.getInstance("AES/CTR/NoPadding");
								aes_decryption.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
								decrypted_message = aes_decryption.doFinal(encrypted_message);
								request = new String(decrypted_message);
								// if the challenge that was send is equal, then it is valid
								if (request.equals(new String(controller_challenge))){
									user = userManager.getRegisteredUser(name);
									userManager.activate(port, user);
									aes_encryption = Cipher.getInstance("AES/CTR/NoPadding");
									aes_encryption.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
									auth_response = "!success".getBytes();
									auth_response = aes_encryption.doFinal(auth_response);
									base64Channel.sendMessageLineInBytes(auth_response);
								}
							}
						}else{
							System.err.println("Error: There doesn't exist a key for this user!");
						}
					}
				}else{
					//user is logged in
					request = new String(aes_decryption.doFinal(message));
					//--request = new String(aes_decryption.doFinal(Base64.decode(request)));
					String[] parts = request.split("\\s+");
					if(parts.length == 1 && parts[0].equals("!exit")){
						
						userManager.deactivate(port);
						
						//if(user != null){
						response = "Successfully logged out. Exiting client.";
						//}else{
						//response = "Exiting client.";
						//}
						
						base64Channel.sendMessageLineInBytes(aes_encryption.doFinal(response.getBytes()));
						//--writer.println(new String(Base64.encode(aes_encryption.doFinal(response.getBytes()))));
						
						break;
					}else if(parts.length > 3 && parts[0].equals("!compute")){

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
//								byte[] term  = null;
//								byte[] hmac_term = null;
//								byte[] computedHash = null;
//								byte[] receivedHash = null;
//								String[] term_parts = null;
//								String compute_term = null;
								Socket nodeSocket = null;
								NodeInfo node = null;
								boolean tampered = false;
								
								while((node = nodeManager.getNode(op)) != null){
									
									try{
//										nodeSocket = nodeSockets.get(node);
										
//										if(nodeSocket == null){
											nodeSocket = new Socket(node.getHostname(), node.getPort());
//											nodeSockets.put(node, nodeSocket);
//										}
											
										TcpChannel nodeTcpChannel = new TcpChannel(nodeSocket);
										Base64Channel nodeBase64Channel = new Base64Channel(nodeTcpChannel);
										HmacChannel nodeHmacChannel = new HmacChannel(nodeBase64Channel, this.secret_hmac_key);
										
										nodeHmacChannel.sendMessageLine("!calc " + no1 + " " + op + " " + no2);
										
										try{
											result = nodeHmacChannel.receiveMessageLine();
//											result = nodeReader.readLine();
											if(result == null)
												throw new SocketException("Result from Socket is null");
											
										}catch(SocketException ex){
											//Socket is not connected anymore (node was closed, but could be open again)
											//try to create a new socket
//											nodeSockets.remove(node);
											//try same node again
											continue;
											
										}catch(TamperedException ex){
											tampered = true;
											
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

								if (tampered){
//									System.err.println("The message from the node is tampered! Computation was not successful!");
									response = "The message from the node is tampered! Computation was not successful!";
									lostCredits = 0;
									break;
								}else if (result.startsWith("!tampered")){
//									System.err.println("The term, that was sent to the node, was tampered, so there was no computation!");
									response = "The term, that was sent to the node, was tampered, so there was no computation!";
									lostCredits = 0;
									break;
								}else if(result.equals("!div0")){
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

					/*}else if(parts.length == 3 && parts[0].equals("!login")){
						response = "Error: You are already logged in!";*/

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
							if (response.equals("")){
								response = "There aren't any operators available at the moment!";
							}
						}else if(parts[0].equals("!logout")){
							userManager.deactivate(port);
							user = null;
							response = "Successfully logged out.";
						}else{
							response = "Error: Please enter a valid command.";
						}
					}else{
						response = "Error: Please enter a valid command.";
					}
				
					//print response
					base64Channel.sendMessageLineInBytes(aes_encryption.doFinal(response.getBytes()));
					//--writer.println(new String(Base64.encode(aes_encryption.doFinal(response.getBytes()))));
				}
				
			}

		}catch(IOException ex){
			//Proceed with shutdown of Thread
		} catch (NoSuchPaddingException e1) {
			e1.printStackTrace();
		} catch (IllegalBlockSizeException e1) {
			e1.printStackTrace();
		} catch (BadPaddingException e1) {
			e1.printStackTrace();
		} catch (InvalidKeyException e1) {
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
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
