package nbgpt.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private final Neo4jClient neo4jClient;

    public GraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public String searchContext(List<Double> queryVector) {
        String cypherQuery =
                "CALL db.index.vector.queryNodes('category_index', 9, $queryVector) YIELD node AS n, score " +
                "WHERE score > 0.6 " +
                "RETURN labels(n)[0] AS type, n.label AS label, score " +
                "UNION " +
                "CALL db.index.vector.queryNodes('keyword_index', 9, $queryVector) YIELD node AS n, score " +
                "WHERE score > 0.6 " +
                "RETURN labels(n)[0] AS type, n.label AS label, score " +
                "ORDER BY score DESC LIMIT 9";

        var results = neo4jClient.query(cypherQuery)
                .bind(queryVector).to("queryVector")
                .fetch()
                .all();

        if (results.isEmpty()) {
            return "Keine passenden Datensätze gefunden.";
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (Map<String, Object> row : results) {
            contextBuilder.append("Typ: ").append(row.get("type")).append("\n");
            contextBuilder.append("Name: ").append(row.get("label")).append("\n\n");
        }

        System.out.println("===== GEFUNDENER KONTEXT =====");
        System.out.println(contextBuilder.toString());
        System.out.println("==============================");

        return contextBuilder.toString();
    }
}
