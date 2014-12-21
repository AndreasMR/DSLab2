package controller.node;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeManager {
	
	//Port-NodeInfo mapping
	private Map <Integer, NodeInfo> nodes = Collections.synchronizedMap(new HashMap<Integer, NodeInfo>());
	
	private List <NodeInfo> nodesAdd = Collections.synchronizedList(new ArrayList<NodeInfo>());
	private List <NodeInfo> nodesSub = Collections.synchronizedList(new ArrayList<NodeInfo>());
	private List <NodeInfo> nodesMul = Collections.synchronizedList(new ArrayList<NodeInfo>());
	private List <NodeInfo> nodesDiv = Collections.synchronizedList(new ArrayList<NodeInfo>());

    private int rmax;

    public NodeManager(int rmax){
        this.rmax = rmax;
    }
	
	public void add(NodeInfo node){
		nodes.put(node.getPort(), node);
		activate(node);
	}
	
	public void activate(NodeInfo node){
		node.setActive(true);
		if(node.hasSupportedOperation('+'))
			nodesAdd.add(node);
		if(node.hasSupportedOperation('-'))
			nodesSub.add(node);
		if(node.hasSupportedOperation('*'))
			nodesMul.add(node);
		if(node.hasSupportedOperation('/'))
			nodesDiv.add(node);
	}
	
	public void deactivate(NodeInfo node){
		node.setActive(false);
		nodesAdd.remove(node);
		nodesSub.remove(node);
		nodesMul.remove(node);
		nodesDiv.remove(node);
	}
	
	public void updateAlive(int port, InetAddress address, String operators){
		NodeInfo node = nodes.get(port);
		if(node == null){
			this.add(new NodeInfo(port, address, operators));
		}else{
			node.updateAlive();
			if(!node.isActive()){
				this.activate(node);
			}
		}
	}
	
	public NodeInfo[] getNodes(){
		NodeInfo[] nodesArray;
		synchronized(nodes){
			nodesArray = nodes.values().toArray(new NodeInfo[nodes.values().size()]);
		}
		return nodesArray;
	}
	
	public NodeInfo getNode(char operator){
		List <NodeInfo> opNodes = null;
		switch(operator){
			case '+': opNodes = nodesAdd; break;
			case '-': opNodes = nodesSub; break;
			case '*': opNodes = nodesMul; break;
			case '/': opNodes = nodesDiv; break;
			default: return null;
		}
		
		if(opNodes.size() == 0)
			return null;
		
		Collections.sort(opNodes, new NodeUsageComparator());
		
		//The node will get picked until it processed the request and its usage value is changed
		NodeInfo ret = opNodes.get(0);
		return ret;
	}

    public int getRmax(){
        return rmax;
    }
}
