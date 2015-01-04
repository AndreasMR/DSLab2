package node;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import util.Config;
import util.Keys;
import model.ComputationRequestInfo;
import controller.tcp.TCPSocketManager;

public class NodeTCPSocketProcessor implements Runnable{
	
	private Socket socket;
	private TCPSocketManager socketManager;
    private Node node;
    private Config config;
    private Key secret_key;

	public NodeTCPSocketProcessor( Socket socket, TCPSocketManager socketManager, Node node, Config config){
		this.socket = socket;
		this.socketManager = socketManager;
        this.node = node;
        this.config = config;
        try {
			this.secret_key = Keys.readSecretKey(new File(config.getString("hmac.key")));
		} catch (IOException e) {
			this.secret_key = null;
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		try{

			// prepare the input reader for the socket
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// prepare the writer for responding to clients requests
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

			final String B64 = "a-zA-Z0-9/+";
			String response = "";
			String compute_term = null;
			byte[] msg = null;
			byte[] receivedHash = null;
			byte[] computedHash = null;
			byte[] tampered_term = null;
			boolean validHash = false;
			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secret_key);
			
			// read client requests
			String request = null;
			while((request = reader.readLine()) != null){
				String[] parts = request.split("\\s+");
				if (parts.length == 5){
					receivedHash = Base64.decode(parts[0].getBytes());;
					//Line below tests, what happends, if the received hash is tampered!
					//receivedHash[4] = 'A';
					compute_term = parts[1] + " " + parts[2] + " " + parts[3] + " " + parts[4];
					// MESSAGE is the message to sign in bytes
					hMac.update(compute_term.getBytes());
					computedHash = hMac.doFinal();
					validHash = MessageDigest.isEqual(computedHash, receivedHash);
					parts = compute_term.split("\\s+");
				}
				if (!validHash){
					System.err.println("The received message is tampered. The controller will be informed!");
					response = "!tampered " + parts[1] + " " + parts[2] + " " + parts[3];
				}else if(parts.length == 4 && parts[0].equals("!calc")){
					//Format: "!calc " + no1 + " " + op + " " + no2
					double no1 = Integer.parseInt(parts[1]);
					char op = parts[2].charAt(0);
					double no2 = Integer.parseInt(parts[3]);
					Double result = null;

					switch(op){
					case '+': result = no1 + no2; break;
					case '-': result = no1 - no2; break;
					case '*': result = no1 * no2; break;
					case '/': {
						if(no2 == 0){
							response = "!div0";
							break;
						}
						result = no1 / no2; 
						break;
					}
					default: response = "Error: Not a valid operator.";
					}

					if(result != null){						
						response = "" + BigDecimal.valueOf(result).setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
					}

					String logContent = (int)no1 + " " + op + " " + (int)no2 + "\r\n";
					
					if(response.equals("!div0"))
						logContent += "Error: Division by 0.";
					else
						logContent += response;
					
					NodeLogger.createLog(System.currentTimeMillis(), logContent);

				}
                else if(parts.length == 2 && parts[0].equals("!share")){
                    int newShare = Integer.parseInt(parts[1]);
                    if(newShare >= node.getRmin()){
                        writer.println("!ok");
                    }
                    else{
                        System.out.println("nok");
                        writer.println("!nok");
                    }
                    String phase2 = reader.readLine();
                    if(phase2.equals("!commit")){
                        node.setShare(newShare);
                    }
                    //else if(phase2.equals("!rollback")){
                        //no action required
                    //}

                    break; //no response required
                }
                else if(parts.length == 1 && parts[0].equals("!getLogs")){
                	new ObjectOutputStream(socket.getOutputStream()).writeObject(NodeLogger.getLogs());
                	response = "";
                }
                else{
					response = "Error: Not a valid command.";
				}
				
				msg = response.getBytes();
				hMac.update(msg);
				computedHash = Base64.encode(hMac.doFinal());
				tampered_term = new byte[computedHash.length+msg.length+1];
				int q;
				for (q = 0; q < computedHash.length; q++){
					tampered_term[q] = computedHash[q];
				}
				tampered_term[q++] = ' ';
				for (int g = 0; g < msg.length; g++){
					tampered_term[q++] = msg[g];
				}
				response = new String(tampered_term);
				//System.out.println(response.matches("["+B64+"]{43}= [\\s[^\\s]]+"));
				//print response
				if(!response.equals(""))
					writer.println(response);

			}
			
		}catch(IOException ex){
			//Proceed with the shutdown of the thread
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			socketManager.close(socket);
			
			socket = null;
			socketManager = null;
		}
	}

}
