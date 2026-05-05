package nbgpt.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
public class GraphService {

    private final Neo4jClient neo4jClient;

    public GraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public String searchContext(List<Double> queryVector, String userQuestion) {
        List<String> allCategoryNames = List.of(
            "Regionen und Städte",
            "Umwelt",
            "Bevölkerung und Gesellschaft",
            "Bildung, Kultur und Sport",
            "Regierung und öffentlicher Sektor",
            "Wirtschaft und Finanzen",
            "Verkehr",
            "Wissenschaft und Technologie",
            "Energie"
        );

        String cypher = """
                CALL {
                    CALL db.index.vector.queryNodes('keyword_index', 10, $queryVector) YIELD node, score
                    RETURN node, score
                    UNION
                    CALL db.index.vector.queryNodes('category_index', 5, $queryVector) YIELD node, score
                    RETURN node, score
                }
                WITH node, score ORDER BY score DESC LIMIT 10
                MATCH (d:Dataset)
                WHERE (d)-[:HAS_KEYWORD]->(node) OR (d)-[:HAS_CATEGORY]->(node)
                WITH d, collect(DISTINCT node.label) AS matchedTerms
                OPTIONAL MATCH (d)-[:HAS_CATEGORY]->(c:Category)
                WITH d, matchedTerms, collect(DISTINCT c.label) AS datasetCategories
                ORDER BY size(matchedTerms) DESC
                LIMIT 5
                RETURN d.title AS datasetName, 
                       datasetCategories, 
                       matchedTerms AS matchedKeywords, 
                       size(matchedTerms) AS keywordMatchCount
                """;

        Collection<Map<String, Object>> datasetResults = neo4jClient.query(cypher)
                .bind(queryVector).to("queryVector")
                .fetch().all();

        Collection<Map<String, Object>> debugKeywords = neo4jClient.query(
                "CALL { " +
                "  CALL db.index.vector.queryNodes('keyword_index', 10, $queryVector) YIELD node, score RETURN node, score " +
                "  UNION " +
                "  CALL db.index.vector.queryNodes('category_index', 5, $queryVector) YIELD node, score RETURN node, score " +
                "} " +
                "WITH node, score ORDER BY score DESC LIMIT 10 " +
                "RETURN node.label AS keyword, score")
                .bind(queryVector).to("queryVector")
                .fetch().all();
        
        System.out.println("--- DEBUG: TOP 10 GEFUNDENE KEYWORDS & KATEGORIEN ZUR ANFRAGE ---");
        int count = 1;
        for (Map<String, Object> kw : debugKeywords) {
            System.out.println(count++ + ". " + kw.get("keyword") + " (Score: " + kw.get("score") + ")");
        }
        System.out.println("----------------------------------------------------");

        return buildContextString(allCategoryNames, datasetResults);
    }

    private String buildContextString(List<String> allCategoryNames, Collection<Map<String, Object>> results) {
        StringBuilder contextBuilder = new StringBuilder();
        
        contextBuilder.append("Verfügbare Kategorien im Open Data Portal Nürnberg: ")
                      .append(String.join(" | ", allCategoryNames)).append("\n\n");

        if (results.isEmpty()) {
            contextBuilder.append("Keine relevanten Datensätze für diese Suchanfrage gefunden.\n");
        } else {
            contextBuilder.append("Nutze die folgenden Datensätze der Stadt Nürnberg, um die Frage zu beantworten.\n\n");

            for (Map<String, Object> row : results) {
                String name = (String) row.get("datasetName");
                
                @SuppressWarnings("unchecked")
                List<String> categories = (List<String>) row.get("datasetCategories");
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) row.get("matchedKeywords");
                
                Number matchCountNum = (Number) row.get("keywordMatchCount");
                long keywordMatchCount = matchCountNum != null ? matchCountNum.longValue() : 0;

                contextBuilder.append("### Datenset: ").append(name).append("\n");
                contextBuilder.append("Kategorien: ").append(String.join(" | ", categories)).append("\n");
                contextBuilder.append("Anzahl passender Suchbegriffe: ").append(keywordMatchCount).append(" von 10\n");
                contextBuilder.append("Relevante Keywords: ").append(String.join(", ", keywords)).append("\n\n");
            }
        }

        System.out.println("===== GEFUNDENER KONTEXT =====");
        System.out.println(contextBuilder.toString());
        System.out.println("==============================");

        return contextBuilder.toString();
    }
}