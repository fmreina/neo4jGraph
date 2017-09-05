package application;

import java.time.Duration;
import java.time.Instant;

import entity.Graph;

public class Main {

	public static void main(String... args) {
		System.out.println("Start running");
		Instant startRunnig = Instant.now();

		Instant start, end;
		Duration nodesDuration = Duration.ZERO;
		Duration relationshipDuration = Duration.ZERO;
		Duration rootDuration = Duration.ZERO;
		Duration readDuration = Duration.ZERO;
		Duration allHashDuration = Duration.ZERO;
		Operation op = new Operation();

		if (Params.CREATE_NEW_GRAPH) {
			op.clearDB();

			start = Instant.now();
			Graph g = new Graph(Params.NUMBER_OF_NODES, op);
			end = Instant.now();
			nodesDuration = Duration.between(start, end);
			System.out.println("\nTime to create nodes : " + nodesDuration);

			start = Instant.now();
			g.createRandomRelationships(op);
			end = Instant.now();
			relationshipDuration = Duration.between(start, end);
			System.out.println("\nTime to create random relationships : " + relationshipDuration);

			start = Instant.now();
			g.addRootNode(op);
			end = Instant.now();
			rootDuration = Duration.between(start, end);
			System.out.println("\nTime to add root node : " + rootDuration);
		}

		if (Params.UPDATE_ALL_HASH_CHASH) {
			start = Instant.now();
			op.updateAllCHashes();
			end = Instant.now();
			allHashDuration = Duration.between(start, end);
			System.out.println("\nTime to update all hashes : " + allHashDuration);
		}

		Instant endRunnig = Instant.now();
		System.out.println("\nEnd running : " + Duration.between(startRunnig, endRunnig));

		//@formatter:off
		System.out.println(
				"\nNumber of Nodes : "+Params.NUMBER_OF_NODES+
				"\nHash Algorithm : "+Params.HASH_ALGORITHM+
				"\nTime to read the graph: "+readDuration+
				"\nTime to create nodes : " + nodesDuration +
				"\nTime to create random relationships : " + relationshipDuration+
				"\nTime to add root node : " + rootDuration+
				"\nTime to update all hashes : " + allHashDuration +
				"\nTotal Execution Time : "
				+ nodesDuration.plus(relationshipDuration).plus(rootDuration).plus(allHashDuration)
				);
		//@formatter:on
	}
}
