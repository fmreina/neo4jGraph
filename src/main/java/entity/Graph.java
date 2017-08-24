package entity;

import java.util.LinkedList;
import java.util.Random;

import application.Operation;

public class Graph {

	private int numberOfNodes;
	private LinkedList<Node> nodes;

	public Graph(int numberOfNodes, Operation op) {
		this.numberOfNodes = numberOfNodes;
		this.nodes = new LinkedList<>();
		this.createNodes(op);
		// this.createRelationships();
	}

	private void createNodes(Operation op) {
		for (int i = 0; i < this.numberOfNodes; i++) {
			Node node = new Node(i, "Nodo " + i);
			op.createNode(node);
			this.nodes.add(node);
		}
	}

	public void createRandomRelationships(Operation op) {
		this.nodes.forEach(node -> {
			Random r = new Random();

			String nodeA = node.getName();
			String nodeB;
			do {
				int num = r.nextInt(this.numberOfNodes + 1);

				nodeB = "Nodo " + num;
			} while (nodeA.equals(nodeB));

			//@formatter:off
			String expression = "MATCH (a), (b) WITH a, b "
							  + "WHERE a<>b AND a.name=\"" + nodeA + "\" "
							  + "AND b.name=\"" + nodeB + "\" "
							  + "CREATE UNIQUE (a)-[r:KNOWS]->(b)";
			//@formatter:on

			// List<Record> result =
			op.runExpression(expression);

			// this.op.updateCHash(nodeA, true);
		});
	}

	public void addRootNode(Operation op) {
		op.createRoot(op);
		op.connectRootToAll(op);
	}
}
