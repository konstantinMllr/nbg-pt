import numpy as np
from neo4j import GraphDatabase
from openai import OpenAI
import argparse

def get_embedding(client, text):
    response = client.embeddings.create(
        input=text,
        model="qwen3"
    )
    return response.data[0].embedding

def update_embeddings(driver, client):
    with driver.session() as session:
        query = """
        MATCH (n) 
        WHERE (n:Keyword OR n:Category) 
        AND n.label IS NOT NULL
        RETURN n.label AS text, elementId(n) AS id, labels(n)[0] AS type
        """
        result = session.run(query)
        items = [{"text": record["text"], "id": record["id"], "type": record["type"]} for record in result]

        if not items:
            print("Es wurden keine passenden Knoten gefunden.")
            return

        print(f"Verarbeite {len(items)} Einträge...")

        for item in items:
            mapped_type = "Kategorie" if item['type'] == "Category" else "Schlagwort"
            text_to_embed = f"{mapped_type}:{item['text']}"
            
            print(f"Vektorisierung ({item['type']}): {text_to_embed}")
            try:
                vector = get_embedding(client, text_to_embed)

                session.run(
                    "MATCH (n) WHERE elementId(n) = $id SET n.embedding = $vector",
                    {"id": item["id"], "vector": vector}
                )
            except Exception as e:
                print(f"Fehler bei '{text_to_embed}': {e}")
                continue

    print("Vektorisierung abgeschlossen!")
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate embeddings for Neo4j nodes")
    parser.add_argument("--neo4j-uri", default="bolt://localhost:7687", help="Neo4j URI")
    parser.add_argument("--neo4j-user", default="neo4j", help="Neo4j Username")
    parser.add_argument("--neo4j-password", required=True, help="Neo4j Password")
    parser.add_argument("--api-base-url", default="http://localhost:8082/v1", help="OpenAI Compatible API Base URL")
    args = parser.parse_args()

    client = OpenAI(
        base_url=args.api_base_url,
        api_key="sk-no-key-required"
    )

    driver = GraphDatabase.driver(args.neo4j_uri, auth=(args.neo4j_user, args.neo4j_password))
    try:
        update_embeddings(driver, client)
    finally:
        driver.close()
