package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;

import controller.tcp.TCPSocketManager;

public class NodeTCPSocketProcessor implements Runnable{
	
	private Socket socket;
	private TCPSocketManager socketManager;

	public NodeTCPSocketProcessor( Socket socket, TCPSocketManager socketManager ){
		this.socket = socket;
		this.socketManager = socketManager;
	}
	
	@Override
	public void run() {
		
		try{

			// prepare the input reader for the socket
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// prepare the writer for responding to clients requests
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

			String response = "";
			
			// read client requests
			String request = null;
			while((request = reader.readLine()) != null){
				
				String[] parts = request.split("\\s+");

				if(parts.length == 4 && parts[0].equals("!calc")){
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

				}else{
					response = "Error: Not a valid command.";
				}

				//print response
				writer.println(response);

			}
			
		}catch(IOException ex){
			//Proceed with the shutdown of the thread
		}finally{
			socketManager.close(socket);
			
			socket = null;
			socketManager = null;
		}
	}

}
