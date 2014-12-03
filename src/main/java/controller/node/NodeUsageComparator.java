package controller.node;

import java.util.Comparator;

public class NodeUsageComparator implements Comparator<NodeInfo>{

	@Override
	public int compare(NodeInfo o1, NodeInfo o2) {
		long u1 = o1.getUseage();
		long u2 = o2.getUseage();
		return u1 < u2 ? -1 : u1 > u2 ? 1 : 0;
	}

}
