package entity;

import java.util.LinkedList;
import java.util.Random;

import application.Operation;

public class Graph {

	private int numberOfNodes;
	private LinkedList<Long> nodeIdList;

	public Graph(int numberOfNodes, Operation op) {
		this.numberOfNodes = numberOfNodes;
		this.nodeIdList = new LinkedList<>();
		this.createNodes(op);
		// this.createRelationships();
	}

	private void createNodes(Operation op) {
		String[] properties = { "name", "number", "type" };
		for (int i = 0; i < this.numberOfNodes; i++) {
			Object[] values = { "node " + i, i, "test" };

			Long nodeId = op.addNode("NODE", properties, values);

			this.nodeIdList.add(nodeId);
		}
	}

	public void createRandomRelationships(Operation op) {
		this.nodeIdList.forEach(nodeID -> {
			Random r = new Random();

			Long id1 = nodeID;
			Long id2;
			do {
				int num = r.nextInt(this.numberOfNodes);

				id2 = this.nodeIdList.get(num);

			} while (id1 == id2);

			op.addOneWayRelation(id1, id2, RelationshipType.CONNECTED_TO);
		});
	}

	public void addRootNode(Operation op) {
		Long rootId = op.createRoot(op);
		op.connectRootToAll(op, rootId);
	}
}
