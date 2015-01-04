package node;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import util.Config;

public class Node implements INodeCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	
	private String logdir;
	private int tcpPort;
	private String cHost;
	private int cUDPPort;
	private int nAlive;
	private String nOperators;

    private int rmin;
    private int share;
    private int nodesLeftToMessage;
	
	private NodeAliveThread nodeAliveThread;
	private ServerSocket serverSocket;
	
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Node(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		logdir = config.getString("log.dir");
		tcpPort = config.getInt("tcp.port");
		cHost = config.getString("controller.host");
		cUDPPort = config.getInt("controller.udp.port");
		nAlive = config.getInt("node.alive");
		nOperators = config.getString("node.operators");
        rmin = config.getInt("node.rmin");

		
		NodeLogger.setLogDir(logdir);
		
		new File(logdir).mkdirs();
	}

	@Override
	public void run() {
        String received = null;
        try {
            //request resource info from controller
            DatagramSocket socket = new DatagramSocket();
            System.out.println("foo");
            sendUDPMessageToController("!hello", socket);
            System.out.println("foo2");

            //receive answer
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("foo3");

            socket.receive(packet);
            received = new String(packet.getData(), 0, packet.getLength()).trim();
            System.out.println("reply to hello: " + received); //TODO: delete debug

        } catch (SocketException e) {
            e.printStackTrace(); //TODO handle
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] parts = received.split(" ");
        int rmax = Integer.parseInt(parts[0]);
        int nodeCount = (parts.length - 1) / 2;
        int resourcesPerNode = rmax / (nodeCount + 1);
        if(resourcesPerNode < this.rmin){
            userResponseStream.println("Insufficient controller resources for node. Shutting down.");
            try {
                exit();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if(nodeCount > 0) {
            String[] IPs = new String[nodeCount];
            int[] ports = new int[nodeCount];
            int i = 0;
            while (i < nodeCount) {
                IPs[i] = parts[i * 2 + 1];
                ports[i] = Integer.parseInt(parts[i * 2 + 2]);
                i++;
            }
            boolean canComeOnline = new NodeCommitter(IPs, ports, resourcesPerNode).tryCommit();
            if (!canComeOnline) {
                userResponseStream.println("insufficient controller resources for node. Shutting down.");
                try {
                    exit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            else{
                share = resourcesPerNode;
            }
        }

        // create a new thread to check send isAlive packets
		nodeAliveThread = new NodeAliveThread();
		nodeAliveThread.start();

		// create and start a new TCP ServerSocket
		try {
			serverSocket = new ServerSocket(tcpPort);

			// handle incoming connections from client in a separate thread
			new NodeTCPListenerThread(serverSocket, this, config).start();

		} catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}

		userResponseStream.println("Node-Server is up! Enter \"!exit\" to exit!");

		// create a new Reader to read commands from System.in
		BufferedReader reader = new BufferedReader( new InputStreamReader( userRequestStream ) );

		String request = "";

		try {
			// read commands on the server side
			while ((request = reader.readLine()) != null) {
				if(request.equals("!exit")){
					break;
				}
                else if(request.equals("!resources")){
                    userResponseStream.println(share);
                }
			}
			
			reader.close();

		} catch (IOException e) {
			// IOException from System.in is very very unlikely (or impossible)
			// and cannot be handled
		}
		
		userResponseStream.println("Shutting down Node..");

		// close socket and listening thread
		try {
			exit();
		} catch (IOException e) {
			// Ignored because we cannot handle it
		}

	}

	@Override
	public String exit() throws IOException {
		/*
		 * Note that closing a socket also triggers an exception in the
		 * listening thread
		 */
		
		if(nodeAliveThread != null)
			nodeAliveThread.exit();
		
		//Close TCP Socket
		if (serverSocket != null)
			serverSocket.close();
		
		NodeLogger.cleanup();
			
		return null;
	}

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {
		Node node = new Node(args[0], new Config(args[0]), System.in, System.out);
		new Thread(node).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String resources() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private class NodeAliveThread extends Thread{
		
		private boolean exit = false;
		
		@Override
		public void run(){
			try {
				
				while( !exit ) {
					
					// open a new DatagramSocket
					/*DatagramSocket datagramSocket = new DatagramSocket();
					
					byte[] buffer = ("!alive " + tcpPort + " " + nOperators).getBytes();
					
					// create the datagram packet with all the necessary information
					// for sending the packet to the server
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(cHost), cUDPPort);
					datagramSocket.send(packet);
					datagramSocket.close();*/
                    DatagramSocket socket = new DatagramSocket();
                    sendUDPMessageToController("!alive " + tcpPort + " " + nOperators, socket);
                    socket.close();
					
					Thread.sleep(nAlive);
				}
				
			} catch (InterruptedException e) {
				//let thread run out
			} catch (SocketException e) {
				//let thread run out
			} catch (UnknownHostException e) {
				//let thread run out
			} catch (IOException e) {
				//let thread run out
			}
		}
		
		public void exit(){
			this.exit = true;
		}
	}

    private void sendUDPMessageToController(String message, DatagramSocket socket) throws IOException {
        // open a new DatagramSocket

        byte[] buffer = message.getBytes();

        // create the datagram packet with all the necessary information
        // for sending the packet to the server
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(cHost), cUDPPort);
        socket.send(packet);
    }

    public synchronized void setShare(int share){
        this.share = share;
    }

    public int getRmin(){
        return rmin;
    }

}
