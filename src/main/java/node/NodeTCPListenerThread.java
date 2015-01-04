package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import controller.tcp.TCPSocketManager;

public class NodeTCPListenerThread extends Thread {
	
	private ServerSocket serverSocket;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
    private Node node;
    private Config config;
	
	public NodeTCPListenerThread(ServerSocket serverSocket, Node node, Config config) {
		this.serverSocket = serverSocket;
        this.node = node;
        this.config = config;
	}

	public void run() {

		TCPSocketManager socketManager = new TCPSocketManager();
		Socket socket = null;
		
		try {
			
			while (true) {
				// wait for Client to connect
				socket = serverSocket.accept();

				socketManager.add(socket);

				//Process Socket in separate Thread
				threadPool.execute(new NodeTCPSocketProcessor(socket, socketManager, node, config));

			}
			
		} catch (IOException e) {
			//serverSocket was closed, proceed with the shutdown of the thread
			
		} finally {
			socketManager.closeAll();
			if (socket != null && !socket.isClosed())
				try {
					socket.close();
				} catch (IOException e) {
					// Ignored because we cannot handle it
				}
			threadPool.shutdownNow();
		}
		
	}
}
