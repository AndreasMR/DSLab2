package controller.node;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;


public class NodeInfo {
	private int port = 0;
	private String hostname = "";
	private String ip = "";
	private Set<Character> supportedOperations = new HashSet<Character>();
	private long useage = 0;
	private long lastAlive = System.currentTimeMillis(); //last TimeStamp in milliseconds when the last isAlive Message was received by this node
	private boolean active = true;
	
	public NodeInfo(int port, InetAddress address, String operations){
		this.port = port;
		this.hostname = address.getHostName();
		this.ip = address.getHostAddress();
		for(int i = 0; i < operations.length(); i++){
			supportedOperations.add(operations.charAt(i));
		}
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getHostname(){
		return this.hostname;
	}
	
	public String getIP(){
		return this.ip;
	}
	
	public boolean hasSupportedOperation(Character op){
		return this.supportedOperations.contains(op);
	}
	
	public void addUseage(long useage){
		synchronized(this){
			this.useage += useage;
		}
	}
	
	public void setUsage(long useage){
		this.useage = useage;
	}
	
	public long getUseage(){
		return useage;
	}
	
	public void updateAlive(){
		this.lastAlive = System.currentTimeMillis();
	}
	
	public long getLastAlive(){
		return lastAlive;
	}
	
	public void setActive(boolean active){
		this.active = active;
	}
	
	public boolean isActive(){
		return active;
	}
	
}
