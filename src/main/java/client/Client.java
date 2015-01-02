package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import cli.Base64Channel;
import cli.TcpChannel;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import util.Config;
import util.Keys;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private TcpChannel tcpChannel;
	private Base64Channel base64Channel;
	private Socket socket;
	
	private String cHost;
	private int cTCPPort;
	private PublicKey controller_pubkey;
	private PrivateKey user_key;

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
		Security.addProvider(new BouncyCastleProvider());
		try {
			this.controller_pubkey = Keys.readPublicPEM(new File(config.getString("controller.key")));
		} catch (IOException e) {
			this.controller_pubkey = null;
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		
		userResponseStream.println("Client is up! Enter \"!exit\" to exit!");

		// create a new Reader to read commands from System.in
		BufferedReader reader = new BufferedReader( new InputStreamReader( userRequestStream ) );
		
		boolean exit = false;
		while(!exit){
			
			if(connectToController()){
				
				//--BufferedReader serverReader = null;
				//--PrintWriter serverWriter = null;
				boolean authenticated = false;
				//final String B64 = "a-zA-Z0-9/+";
				Cipher cipher = null;
				Cipher aes_encryption = null;
				Cipher aes_decryption = null;
				
				SecureRandom secureRandom;
				
				try {
					// create a reader to retrieve messages send by the server
					//--serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					// create a writer to send messages to the server
					//--serverWriter = new PrintWriter(socket.getOutputStream(), true);

					userResponseStream.println("Successfully connected to Controller. Enter valid commands.");
					
					// read commands on the client side
					while (true){
						byte[] byte_request;
						byte[] byte_challenge;
						byte[] message;
						byte[] cipherText = null;
						byte[] secret_key = null;
						String response = "";
						String[] parts;
						
						String request = reader.readLine();
						
						if(request == null){
							request = "!exit";
							exit = true;
							break;
						}else{
							parts = request.split("\\s+");
							if (!authenticated){
								if (request.equals("!exit")){
									exit = true;
									break;
								}
								if (parts.length == 2 && parts[0].equals("!authenticate")){
									try {
										this.user_key = Keys.readPrivatePEM(new File(config.getString("keys.dir")+"/"+parts[1]+".pem"));
									} catch (IOException e) {
										this.user_key = null;
										userResponseStream.println("Error: There doesn't exist a private key for this user");
									}
									if (user_key != null){
										secureRandom = new SecureRandom();
										final byte[] number = new byte[32];
										secureRandom.nextBytes(number);
										byte_challenge = Base64.encode(number);
										byte_request = request.getBytes();
										message = new byte[byte_challenge.length+byte_request.length+1];
										int i;
										for (i = 0; i < byte_request.length; i++){
											message[i] = byte_request[i];
										}
										message[i++] = ' ';
										for (int j = 0; j < byte_challenge.length; j++){
											message[i++] = byte_challenge[j];
										}
										//System.out.println(new String(message));
										cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
										cipher.init(Cipher.ENCRYPT_MODE, controller_pubkey);
										cipherText = cipher.doFinal(message);
										message = null;
										base64Channel.sendMessageLineInBytes(cipherText);
										message = base64Channel.receiveMessageLineInBytes();
										//--cipherText = Base64.encode(cipherText);
										//--request = new String(cipherText);
										//--tcpChannel.sendMessageLine(request);
										//--response = tcpChannel.receiveMessageLine();
										//--serverWriter.println(request);
										//--response = serverReader.readLine();
										if (message != null){
											//--message = null;
											cipher.init(Cipher.DECRYPT_MODE, user_key);
											message = cipher.doFinal(/*Base64.decode(*/message/*)*/);
											response = new String(message);
											parts = response.split("\\s+");
											//System.out.println(response);
											if (parts.length == 5 && new String(byte_challenge).equals(parts[1])){
												aes_encryption = Cipher.getInstance("AES/CTR/NoPadding");
												secret_key = Base64.decode(parts[3]);
												SecretKey key = new SecretKeySpec(secret_key, 0, secret_key.length, "AES");
												aes_encryption.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(Base64.decode(parts[4])));
												cipherText = aes_encryption.doFinal(parts[2].getBytes());
												message = null;
												base64Channel.sendMessageLineInBytes(cipherText);
												message = base64Channel.receiveMessageLineInBytes();
												//--cipherText = Base64.encode(cipherText);
												//--request = new String(cipherText);
												//--tcpChannel.sendMessageLine(request);
												//--response = tcpChannel.receiveMessageLine();
												//--serverWriter.println(request);
												//--response = serverReader.readLine();
												if (message != null){
													aes_decryption = Cipher.getInstance("AES/CTR/NoPadding");
													aes_decryption.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Base64.decode(parts[4])));
													response = new String(aes_decryption.doFinal(message));
													if (response.equals("!success")){
														authenticated = true;
														userResponseStream.println("Authentication was successful!");
													}
												}
											}else{
												userResponseStream.println("Error: The authentication phase failed!");
											}
										}
									}
								}else{
									userResponseStream.println("usage: !authenticate <name>");
								}
							}else{
								message = null;
								base64Channel.sendMessageLineInBytes(aes_encryption.doFinal(request.getBytes()));
								message = base64Channel.receiveMessageLineInBytes();
								//--tcpChannel.sendMessageLine(new String(Base64.encode(aes_encryption.doFinal(request.getBytes()))));
								//--response = tcpChannel.receiveMessageLine();
								// write provided user input to the socket
								//--serverWriter.println(new String(Base64.encode(aes_encryption.doFinal(request.getBytes()))));
								//System.out.println(new String(aes_decryption.doFinal(Base64.decode(new String(Base64.encode(aes_encryption.doFinal(request.getBytes())))))));
								// read server response and write it to console
								//--response = serverReader.readLine();
								
								if(message == null){
									throw new IOException("Connection to controller lost");
								}
								userResponseStream.println(new String(aes_decryption.doFinal(message)));
								//userResponseStream.println(new String(aes_decryption.doFinal(Base64.decode(response))));
								if (request.equals("!logout")){
									authenticated = false;
								}
								if(request.equals("!exit")){
									authenticated = false;
									exit = true;
									break;
								}
							}
						}
					}
										
				} catch (IOException e) {
					// connection to controller lost, try to reconnect
					continue;
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalBlockSizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BadPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidAlgorithmParameterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			tcpChannel = new TcpChannel(socket);
			base64Channel = new Base64Channel(tcpChannel);
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
