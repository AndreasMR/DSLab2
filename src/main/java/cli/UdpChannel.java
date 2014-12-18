package cli;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpChannel implements Channel {
	private String host;
	private int udpport;
	private DatagramSocket socket;
	
	public UdpChannel(String host, int udpport) {
		this.host = host;
		this.udpport = udpport;
		try {
			socket = new DatagramSocket(udpport);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void close(){
		if (socket != null && !socket.isClosed())
			socket.close();
	}
	
	@Override
	public String receiveMessageLine() {
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		try {
			socket.receive(packet);
			return packet.getData().toString();
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			return null;
		}
	}

	@Override
	public void sendMessageLine(String msg) {
		try {
			DatagramSocket datagramSocket = new DatagramSocket();
			
			byte[] buffer = msg.getBytes();
	
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(host), udpport);
			datagramSocket.send(packet);
		
			datagramSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
		}
	}
}
