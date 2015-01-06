package controller.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import controller.node.NodeInfo;
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
		String request = new String(packet.getData()).trim();
        if(request.equals("!hello")){
            String response = nodeManager.getRmax() + " ";
            NodeInfo[] nodes = nodeManager.getNodes();
            for(NodeInfo node : nodes){
                if(node.isActive()){
                    response += node.getIP() + " " + node.getPort() + " ";
                }
            }

            byte[] buf = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.send(responsePacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally{
                if(socket != null){
                    socket.close();
                }
            }
        }
        String[] parts = request.split("\\s+");

		if (parts.length == 3) {
			if(parts[0].equals("!alive")){
				nodeManager.updateAlive(Integer.parseInt(parts[1]), packet.getAddress(), parts[2]);
			}
		}
		
	}

}
