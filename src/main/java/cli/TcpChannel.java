package cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpChannel implements Channel {
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	private Socket socket = null;
	
	public TcpChannel(Socket socket) throws IOException {
		this.socket = socket;
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.writer = new PrintWriter(socket.getOutputStream(), true);
	}
	
	public String receiveMessageLine() throws IOException {
		return reader.readLine(); 
	}
	
	public void sendMessageLine(String msg) {
		writer.println(msg);
	}
	
	public String exit() throws IOException{
		if(socket != null && !socket.isClosed()){
			socket.close();
			socket = null;
		}
		return null;
	}
	
	
}
