package controller.node;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeManager implements Serializable{
	
	private static final long serialVersionUID = 2794134793557429473L;

	//Port-NodeInfo mapping
	private Map <Integer, NodeInfo> nodes = Collections.synchronizedMap(new HashMap<Integer, NodeInfo>());
	
	private List <NodeInfo> nodesAdd = Collections.synchronizedList(new ArrayList<NodeInfo>());
	private List <NodeInfo> nodesSub = Collections.synchronizedList(new ArrayList<NodeInfo>());
	private List <NodeInfo> nodesMul = Collections.synchronizedList(new ArrayList<NodeInfo>());
	private List <NodeInfo> nodesDiv = Collections.synchronizedList(new ArrayList<NodeInfo>());

    private int rmax;
    
    private Map<Character, Long> opOccurrences;

    public NodeManager(int rmax){
        this.rmax = rmax;
        opOccurrences = Collections.synchronizedMap(new HashMap<Character, Long>());
        opOccurrences.put('+', 0L);
        opOccurrences.put('-', 0L);
        opOccurrences.put('*', 0L);
        opOccurrences.put('/', 0L);
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
	
	public void addOperatorStatistics(String[]terms){
		for(int i = 3; i < terms.length; i+=2){
			char op = terms[i-1].charAt(0);
			if(op == '+' || op == '-' || op == '*' || op == '/'){
//				System.out.println("op: " + op + " : " + (opOccurrences.get(op) + 1));
				synchronized(this){
					opOccurrences.put(op, opOccurrences.get(op) + 1);
				}
			}
		}
	}
	
	public Map<Character, Long> getOperatorOccurrences(){
		return opOccurrences;
	}

    public int getRmax(){
        return rmax;
    }
}
