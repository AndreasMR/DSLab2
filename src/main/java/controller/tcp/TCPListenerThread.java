package controller.tcp;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;
import controller.node.NodeManager;
import controller.user.UserManager;

public class TCPListenerThread extends Thread {
	
	private ServerSocket serverSocket;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	
	private UserManager userManager = null;
	private NodeManager nodeManager = null;
	private Config config;
	private PrintStream serverStream;

	public TCPListenerThread(ServerSocket serverSocket, UserManager userManager, NodeManager nodeManager, Config config, PrintStream serverStream) {
		this.serverSocket = serverSocket;
		this.userManager = userManager;
		this.nodeManager = nodeManager;
		this.config = config;
		this.serverStream = serverStream;
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
				threadPool.execute(new TCPSocketProcessor(socket, socketManager, userManager, nodeManager, config, serverStream));

			}
			
		} catch (IOException e) {
			//Socket was closed, proceed with shutdown of thread
		
		} finally {
			socketManager.closeAll();
			if (socket != null && !socket.isClosed())
				try {
					socket.close();
				} catch (IOException e) {
					// Ignored because we cannot handle it
				}
			threadPool.shutdownNow();
//			System.err.println("Closed TCPListenerThread");
		}

	}
}
