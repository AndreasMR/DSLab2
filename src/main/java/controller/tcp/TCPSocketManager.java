package controller.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TCPSocketManager {
	
	private Set<Socket> activeSockets = Collections.synchronizedSet(new HashSet<Socket>());
	
	public void add(Socket socket){
		activeSockets.add(socket);
	}
	
	public void remove(Socket socket){
		activeSockets.remove(socket);
	}
	
	public void close(Socket socket){
		
		if(socket != null){
			
			if(!socket.isClosed()){
				
				try {
					socket.close();
					
				} catch (IOException e) {
					e.printStackTrace();
					// Ignored because we cannot handle it
				}
				
			}
			
			remove(socket);
		}
		
	}
	
	public void closeAll(){
		synchronized(activeSockets){
			for(Socket s : activeSockets.toArray(new Socket[activeSockets.size()])){
				close(s);
			}
		}
	}
}
