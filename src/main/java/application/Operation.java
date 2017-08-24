package application;

import java.util.List;

import lombok.NoArgsConstructor;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

import cryptography.Hash;
import cryptography.HashGeneretor;
import cryptography.HashType;
import entity.Node;
import entity.RelationshipType;

@NoArgsConstructor
public class Operation extends BaseApplication {

	public List<Record> runExpression(String expression) {

		try (Session session = this.driver.session()) {

			return session.writeTransaction(new TransactionWork<List<Record>>() {

				@Override
				public List<Record> execute(Transaction transaction) {
					StatementResult result = transaction.run(expression);
					return result.list();
				}
			});
		}
	}

	public void createNode(Node node) {
		String attributesForHashing = node.getId() + node.getName();
		String hash = this.getHash(attributesForHashing);
		String cHash = hash;

		// @formatter:off
		String expression = "CREATE (a:Node {"
								  + "id: " + node.getId() + ", "
								  + "name: \"" + node.getName() + "\", "
								  + "hash: \"" + hash + "\", "
								  + "chash: \"" + cHash + "\""
								  + "})"
						  + "RETURN id(a), a.name";;
		// formatter:on
		List<Record> result = this.runExpression(expression);
	}

	public void addOneWayRelation(String nameA, String labelA, String nameB, String labelB, RelationshipType relation) {
		//@formatter:off;
		String expression = "MATCH (a:"+labelA+") , (b:"+labelB+") "
				+ "WHERE a.name = \""+nameA+"\" AND b.name = \""+nameB+"\" "
				+ "CREATE (a)-[r:"+relation.getLabel()+"{name: \""+relation.getName()+"\"}]->(b) "
				+ "RETURN a.name, r.name, b.name";
		//@formatter:on

		List<Record> result = this.runExpression(expression);
		// result.list().forEach(r -> System.out.println(r));

		this.updateCHash(nameA, true);
	}

	public void addTwoWayRelation(String nameA, String labelA, String nameB, String labelB, RelationshipType relation) {
		//@formatter:off;
		String expression = "MATCH (a:"+labelA+") , (b:"+labelB+") "
				+ "WHERE a.name = \""+nameA+"\" AND b.name = \""+nameB+"\" "
				+ "CREATE (a)-[r1:"+relation.getLabel()+"{name: \""+relation.getName()+"\"}]->(b), "
				+ "(b)-[r2:"+relation.getLabel()+"{name: \""+relation.getName()+"\"}]->(a)"
				+ "RETURN a.name, r1.name, b.name, r2.name";
		//@formatter:on

		List<Record> result = this.runExpression(expression);
		// result.list().forEach(r -> System.out.println(r));

		this.updateCHash(nameA, true);
	}

	public List<Record> getNeighborsOf(String name) {
		// formatter:off
		String expression = "MATCH (a{name:\"" + name + "\"}) " + "OPTIONAL MATCH (a)<-[r]->(b) " + "RETURN a.name, a.hash, b.name, b.hash " + "ORDER BY id(b)";
		// formatter:on
		List<Record> result = this.runExpression(expression);
		return result;
	}

	public void updateCHash(String name, boolean checkNeighbors) {
		// System.out.println("Discover all neighbors");
		List<Record> list = this.getNeighborsOf(name);

		// System.out.println("Collect their hashes");
		if (!list.isEmpty()) {
			String aHash = list.get(0).get("a.hash", "");
			String temp = "";
			// String order = ""; // used only for testing
			for (Record item : list) {
				temp += item.get("b.hash", "");
				// order += item.get("b.name", ""); // used only for testing
			}

			// System.out.println("Calculate CHash");
			String cHash = this.getHash(aHash + temp);

			// System.out.println("Update CHash...");
			this.runExpression("MATCH (a {name:\"" + name + "\"}) SET a.chash = \"" + cHash + "\"");

			// order = list.get(0).get("a.name", "") + order; // used only for testing
			// System.out.println("cHash updated : Node " + name + " : order " + order + " : cHash " + cHash);

			// System.out.println("Repeat for each neighbor");
			if (checkNeighbors) {
				list.forEach(item -> this.updateCHash(item.get("b.name", ""), false));
			}
		}
	}

	public void updateAllCHashes() {
		List<Record> nodes = this.runExpression("MATCH (a) RETURN a.name ORDER BY id(a)");

		nodes.forEach(node -> {
			String name = node.get("a.name", "");
			this.updateCHash(name, true);
		});
	}

	public void clearDB() {
		String expression = "MATCH (a) OPTIONAL MATCH (a)-[r]->() DELETE a, r";

		List<Record> result = this.runExpression(expression);
	}

	public void createRoot(Operation op) {
		String name = "Root";
		String attributesForHashing = name;

		String hash = op.getHash(attributesForHashing);
		String chash = op.getHash(hash);

		//@formatter:off
		String expression = "CREATE (a:ROOT{name: \"" + name + "\", "
								  + "hash: \"" + hash + "\", "
								  + "chash: \"" + chash + "\""
								  + "}) "
						  + "RETURN id(a), a.name";
		//@formatter:on
		List<Record> result = op.runExpression(expression);
		// result.forEach(record -> System.out.println("Node id(" + record.get("id(a)", 0) + ") name: " + record.get("a.name", "")));
	}

	public void connectRootToAll(Operation op) {
		List<Record> nodes = op.runExpression("MATCH (a) RETURN a.name");
		nodes.forEach(name -> {
			List<Record> result = op.runExpression("match (a:ROOT), (b) with a, b where a<>b create unique (a)-[r:HAS_NODE]->(b)");
			// result.forEach(record -> System.out.println(record));
		});
	}

	public String getHash(String msg) {
		switch (Params.HASH_ALGORITHM) {
		case BCrypt:
			return HashGeneretor.hashPassword(msg);
		case MD5:
			return Hash.generateHash(msg, HashType.MD5);
		case SHA1:
			return Hash.generateHash(msg, HashType.SHA1);
		case SHA256:
			return Hash.generateHash(msg, HashType.SHA256);
		case NONE:
		default:
			return msg;
		}
	}
}
