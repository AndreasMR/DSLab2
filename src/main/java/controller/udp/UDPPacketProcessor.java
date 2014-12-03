package controller.udp;

import java.net.DatagramPacket;

import controller.node.NodeManager;

public class UDPPacketProcessor implements Runnable{
	
	private DatagramPacket packet = null;
	private NodeManager nodeManager = null;

	public UDPPacketProcessor( DatagramPacket packet, NodeManager nodeManager){
		this.packet = packet;
		this.nodeManager = nodeManager;
	}
	
	@Override
	public void run() {
		
		// get the data from the packet
		String request = new String(packet.getData());

		String[] parts = request.split("\\s+");

		if (parts.length == 3) {
			if(parts[0].equals("!alive")){
				nodeManager.updateAlive(Integer.parseInt(parts[1]), packet.getAddress(), parts[2]);
			}
		}
		
	}

}
