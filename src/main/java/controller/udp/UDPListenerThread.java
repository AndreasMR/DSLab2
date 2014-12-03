package controller.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import controller.node.NodeManager;

/**
 * Thread to listen for incoming data packets on the given socket.
 */
public class UDPListenerThread extends Thread {

	private DatagramSocket datagramSocket;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private NodeManager nodeManager = null;
	
	public UDPListenerThread(DatagramSocket datagramSocket, NodeManager nodeManager) {
		this.datagramSocket = datagramSocket;
		this.nodeManager = nodeManager;
	}
	
	public void run() {
		
		byte[] buffer;
		DatagramPacket packet;
		try {
			while (true) {
				buffer = new byte[1024];
				// create a datagram packet of specified length (buffer.length)
				packet = new DatagramPacket(buffer, buffer.length);

				// wait for incoming packets from client
				datagramSocket.receive(packet);
				
				//Process packet in separate thread
				threadPool.execute(new UDPPacketProcessor(packet, nodeManager));
				
			}

		} catch (IOException e) {
			//datagramSocket was closed, proceed with the shutdown of the thread
		} finally {
			if (datagramSocket != null && !datagramSocket.isClosed())
				datagramSocket.close();
			threadPool.shutdownNow();
		}
		
	}
	
}
