package controller.rmi;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.ComputationRequestInfo;
import util.Config;
import util.Keys;
import admin.INotificationCallback;
import cli.Base64Channel;
import cli.Channel;
import cli.HmacChannel;
import cli.TcpChannel;
import controller.IAdminConsole;
import controller.node.NodeInfo;
import controller.node.NodeManager;
import controller.user.UserInfo;
import controller.user.UserManager;

public class AdminService implements IAdminConsole, Serializable{
	
	private static final long serialVersionUID = 3929133918781894335L;
	private UserManager userManager;
	private NodeManager nodeManager;
	private Key secret_hmac_key;
	
	public AdminService(UserManager userManager, NodeManager nodeManager){
		this.userManager = userManager;
		this.nodeManager = nodeManager;
		Config config = new Config("controller");
		try {
			this.secret_hmac_key = Keys.readSecretKey(new File(config.getString("hmac.key")));
		} catch (IOException e) {
			this.secret_hmac_key = null;
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean subscribe(String username, int credits, INotificationCallback callback) throws RemoteException {
		
		UserInfo user = userManager.getRegisteredUser(username);
		
		if(user != null){
			user.addCreditsNotification(callback, credits);
			return true;
		}else{
			return false;
		}
		
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		
		List<ComputationRequestInfo> logs = new ArrayList<ComputationRequestInfo>();
		
		NodeInfo[] nodes = nodeManager.getNodes();
		Socket nodeSocket = null;
		
		for(NodeInfo n : nodes){
			if(n.isActive()){
				try{
					nodeSocket = new Socket(n.getHostname(), n.getPort());
					
					Channel channel = new HmacChannel(new Base64Channel(new TcpChannel(nodeSocket)), this.secret_hmac_key);
					
					// create a writer to send messages to the node
//					PrintWriter nodeWriter = new PrintWriter(nodeSocket.getOutputStream(), true);
//					nodeWriter.println("!getLogs");
					channel.sendMessageLine("!getLogs");
					
					// create a reader to retrieve messages send by the node
					ObjectInputStream nodeReader = new ObjectInputStream(nodeSocket.getInputStream());
					List<ComputationRequestInfo> result = (List<ComputationRequestInfo>)nodeReader.readObject();
					
					if(result == null)
						throw new SocketException("Result from Socket is null");

					logs.addAll(result);

				} catch (IOException | ClassNotFoundException ex) {
					//It was not possible to properly connect to the node
//					nodeManager.deactivate(node);
					//try next node
					continue;
				} finally {
					if(nodeSocket != null && !nodeSocket.isClosed()){
						try {
							nodeSocket.close();
						} catch (IOException e) {
							//nothing to do
						}
					}
				}
			}
		}
		
		Collections.sort(logs);
		
		return logs;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		Map<Character, Long> opOccurrences = nodeManager.getOperatorOccurrences();
		LinkedHashMap<Character, Long> sortedOpOccurrences = new LinkedHashMap<Character, Long>();
		
		Set<Entry<Character, Long>> entries = new HashSet<Entry<Character, Long>>(opOccurrences.entrySet());
		Entry<Character, Long> maxEntry = null;
		while(!entries.isEmpty()){
			for(Entry<Character, Long> e : entries){
				if(maxEntry == null || e.getValue() > maxEntry.getValue())
					maxEntry = e;
			}
			sortedOpOccurrences.put(new Character(maxEntry.getKey()), new Long(maxEntry.getValue()));
			entries.remove(maxEntry);
			maxEntry = null;
		}
		
		return sortedOpOccurrences;
	}
	
}
