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
	
	public UdpChannel(String host, int udpport) throws SocketException {
		this.host = host;
		this.udpport = udpport;
		socket = new DatagramSocket(udpport);
	}
	
	public void close(){
		if (socket != null && !socket.isClosed())
			socket.close();
	}
	
	@Override
	public String receiveMessageLine() throws IOException {
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		socket.receive(packet);
		return new String(packet.getData());
	}

	@Override
	public void sendMessageLine(String msg) throws IOException {
		DatagramSocket datagramSocket = new DatagramSocket();
		
		byte[] buffer = msg.getBytes();

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(host), udpport);
		datagramSocket.send(packet);
	
		datagramSocket.close();
	}
}
