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
	
	public TcpChannel(Socket socket) {
		this.socket = socket;
		try {
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.writer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Fehler beim Aufbauen des Input- und Outputstreams");
		}
	}
	
	public String receiveMessageLine() {
		try {
			return reader.readLine();
		} catch (IOException e) {
			System.err.println("Fehler beim Warten auf eine Nachricht");
			return null;
		} 
	}
	
	public void sendMessageLine(String msg) {
		writer.println(msg);
	}
	
	
}
