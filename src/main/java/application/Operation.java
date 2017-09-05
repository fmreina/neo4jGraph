package application;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.NoArgsConstructor;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;

import cryptography.Hash;
import cryptography.HashGeneretor;
import cryptography.HashType;
import entity.RelationshipType;

@NoArgsConstructor
public class Operation extends BaseApplication {

	public List<Record> runExpression(String expression, Value params) {

		try (Session session = this.driver.session()) {

			return session.writeTransaction(new TransactionWork<List<Record>>() {

				@Override
				public List<Record> execute(Transaction transaction) {
					StatementResult result;
					if (params == null) {
						result = transaction.run(expression);
					} else {
						result = transaction.run(expression, params);
					}
					return result.list();
				}
			});
		}
	}

	public Long addNode(String label, String[] properties, Object[] values) {
		if (label.equals("ROOT")) {
			System.out.println("Cannot create ROOT node");
			return -1L;
		}

		//@formatter:off
		String propsFormat = IntStream.range(0, properties.length)
					.mapToObj(i -> properties[i] + ": "
								+ (values[i].getClass().getSimpleName().equals("String") ?
										"'" + values[i] + "'" : values[i])
								).sequential().collect(Collectors.joining(", "));

		//@formatter:on

		String expression = "CREATE (a:" + label + "{" + propsFormat + "}) RETURN id(a)";
		List<Record> result = this.runExpression(expression, null);

		Long nodeId = result.get(0).get("id(a)").asLong();
		// this.updateHash(nodeId);

		return nodeId;
	}

	public void UpdateNode() {
		// TODO: complete method
	}

	public Long deleteNode(Long id) {
		// TODO: complete method
		return id;
	}

	public Long removeRelationship(Long id) {
		// TODO: complete methodcreateNodes
		return id;
	}

	public Long addOneWayRelation(Long id1, Long id2, RelationshipType relation) {
		//@formatter:off;
		String expression = "MATCH (a), (b) WHERE id(a) = $id1 AND id(b) = $id2 "
							+ "CREATE (a)-[r:"+relation.getLabel()+"{name: '$name'}]->(b) "
							+ "RETURN id(r)";
		//@formatter:on
		Value params = parameters("id1", id1, "id2", id2, "name", relation.name());
		List<Record> result = this.runExpression(expression, params);

		// this.updateCHash(id1);
		// this.updateCHash(id2);

		return result.get(0).get("id(r)").asLong();
	}

	public Long[] addTwoWayRelation(Long id1, Long id2, RelationshipType relation) {
		//@formatter:off;
		String expression = "MATCH (a), (b) WHERE id(a) = $id1 AND id(b) = $id2"
							+ "CREATE (a)-[r1:"+relation.getLabel()+"{name: '$name'}]->(b) "
							+ "CREATE (b)-[r2:"+relation.getLabel()+"{name: '$name'}]->(a) "
							+ "RETURN id(r1), id(r2)";
		//@formatter:on
		Value params = parameters("id1", id1, "id2", id2, "name", relation.name());
		List<Record> result = this.runExpression(expression, params);

		this.updateCHash(id1);
		this.updateCHash(id2);

		Long[] relations = { result.get(0).get("id(r1)").asLong(), result.get(0).get("id(r2)").asLong() };
		return relations;
	}

	public List<Record> getNeighborsOf(Long id) {
		//@formatter:off
		String expression = "MATCH (a) WHERE id(a) = $id "
							+ "OPTIONAL MATCH (a)<-[r]->(b) "
							+ "RETURN a.hash, b.hash, id(r), id(b) "
							+ "ORDER BY id(b)";
		//@formatter:on
		return this.runExpression(expression, parameters("id", id));
	}

	public void updateHash(Long id) {
		//@formatter:off
		String expression = "MATCH (a) WHERE id(a) = $id "
							+ "WITH DISTINCT KEYS(a) AS properties "
							+ "UNWIND properties AS keyslisting "
							+ "WITH DISTINCT keyslisting AS allfields "
							+ "RETURN allfields";
		//@formatter:on

		List<Record> properties = this.runExpression(expression, parameters("id", id));

		String concatenation = id.toString();
		for (Record property : properties) {
			String propertyName = property.get(0).asString();
			if (propertyName.equalsIgnoreCase("chash") || propertyName.equalsIgnoreCase("hash")) {
				// do nothing. doesn't need to collect hash and cHash
			} else {
				// FIXME: it access the DB for each property, so it can be very expensive. Properties must contain the whole object not only the properties
				//@formatter:off
				expression = "MATCH (a) WHERE id(a) = $id "
							+ "RETURN a." + propertyName;
				//@formatter:on

				concatenation += this.runExpression(expression, parameters("id", id)).get(0).get("a." + propertyName);
			}
		}

		String hash = this.getHash(concatenation);

		expression = "MATCH (a) WHERE id(a) = $id SET a.hash = $hash";
		this.runExpression(expression, parameters("id", id, "hash", hash));
	}

	public void updateAllHashes() {
		String expression = "MATCH (a) RETURN id(a) ORDER BY id(a)";
		List<Record> nodes = this.runExpression(expression, null);

		nodes.forEach(node -> {
			long id = node.get("id(a)", 0);
			this.updateHash(id);
		});
	}

	public void updateCHash(Long id) {
		// All nodes need to have hash property
		List<Record> list = this.getNeighborsOf(id);

		String temp = list.get(0).get("a.hash", "");
		for (Record item : list) {
			temp += item.get("id(r)", 0) + item.get("b.hash", "");
		}

		String cHash = this.getHash(temp);

		String expression = "MATCH (a) WHERE id(a) = $id SET a.chash = $chash";
		this.runExpression(expression, parameters("id", id, "chash", cHash));
	}

	public void updateAllCHashes() {
		String expression = "MATCH (a) RETURN id(a) ORDER BY id(a)";
		List<Record> nodes = this.runExpression(expression, null);

		nodes.forEach(node -> {
			long id = node.get("id(a)", 0);
			this.updateCHash(id);
		});
	}

	public void clearDB() {
		String expression = "MATCH (a) OPTIONAL MATCH (a)-[r]->() DELETE a, r";
		this.runExpression(expression, null);
	}

	public Long rootId() {
		String expression = "match (a:ROOT) return id(a)";

		return this.runExpression(expression, null).isEmpty() ? null : this.runExpression(expression, null).get(0).get("id(a)").asLong();
	}

	public Long createRoot(Operation op) {
		if (this.rootId() == null) {
			String expression = "CREATE (a:ROOT{name:'ROOT'}) RETURN id(a)";
			Long rootId = this.runExpression(expression, null).get(0).get("id(a)").asLong();
			// this.updateHash(rootId);
			return rootId;
		} else {
			System.out.println("There already exists a ROOT node.");
			return -1L;
		}
	}

	public void connectRootToAll(Operation op, Long rootId) {
		String expression = "MATCH (a) WHERE id(a) <> $rootId RETURN id(a)";
		List<Record> nodes = op.runExpression(expression, parameters("rootId", rootId));

		nodes.forEach(id -> {
			String expr = "MATCH (a), (b) where id(a)=$id1 and id(b)=$id2 CREATE (a)-[r:HAS_NODE]->(b)";
			Long id2 = id.get("id(a)").asLong();
			Value params = parameters("id1", rootId, "id2", id2);
			op.runExpression(expr, params);
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
