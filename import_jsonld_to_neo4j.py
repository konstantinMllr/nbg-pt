#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Bayern Open Data → Neo4j Metadaten-Import
Kombiniert Search-API (Liste) + Dataset-API (Details pro Datensatz)
"""
import requests
import logging
from neo4j import GraphDatabase

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger(__name__)

SEARCH_URL  = "https://opendata.bayern/api/hub/search/search"
DATASET_URL = "https://opendata.bayern/api/hub/search/datasets/{id}"

def fetch_search_results(query="Nürnberg", limit=10):
    resp = requests.get(
        SEARCH_URL,
        params={"q": query, "filters": "dataset", "limit": limit},
        headers={"Accept": "application/json"},
        timeout=15,
    )
    resp.raise_for_status()
    items = resp.json().get("result", {}).get("results", [])
    log.info(f"Search: {len(items)} Treffer für '{query}'")
    return items

def fetch_dataset_detail(dataset_id: str) -> dict:
    resp = requests.get(
        DATASET_URL.format(id=dataset_id),
        headers={"Accept": "application/json"},
        timeout=15,
    )
    resp.raise_for_status()
    return resp.json().get("result", {})


def lang(mapping, *keys):
    if isinstance(mapping, str):
        return mapping
    for k in keys:
        if mapping.get(k):
            return mapping[k]
    return ""


CONSTRAINTS = [
    "CREATE CONSTRAINT IF NOT EXISTS FOR (d:Dataset)      REQUIRE d.id IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (p:Publisher)    REQUIRE p.name IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (c:Catalog)      REQUIRE c.id IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (cat:Category)   REQUIRE cat.id IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (k:Keyword)      REQUIRE k.id IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (f:Format)       REQUIRE f.id IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (l:License)      REQUIRE l.resource IS UNIQUE",
    "CREATE CONSTRAINT IF NOT EXISTS FOR (d:Distribution) REQUIRE d.id IS UNIQUE",
]

def setup_constraints(session):
    for cql in CONSTRAINTS:
        session.run(cql)
    log.info("Constraints gesetzt.")


def ingest_dataset(tx, d: dict):
    dataset_id = d.get("id")
    if not dataset_id:
        return

    tx.run("""
        MERGE (d:Dataset {id: $id})
        SET d.title       = $title,
            d.description = $desc,
            d.modified    = $modified,
            d.issued      = $issued
    """,
           id=dataset_id,
           title=lang(d.get("title", {}), "de", "en"),
           desc=lang(d.get("description", {}), "de", "en")[:500],
           modified=d.get("catalog_record", {}).get("modified", ""),
           issued=d.get("catalog_record", {}).get("issued", ""),
           )

    pub = d.get("publisher", {}).get("name", "")
    if pub:
        tx.run("""
            MERGE (p:Publisher {name: $name})
            WITH p MATCH (d:Dataset {id: $did})
            MERGE (d)-[:PUBLISHED_BY]->(p)
        """, name=pub, did=dataset_id)

    cat = d.get("catalog", {})
    if cat.get("id"):
        tx.run("""
            MERGE (c:Catalog {id: $id})
            SET c.title = $title
            WITH c MATCH (d:Dataset {id: $did})
            MERGE (d)-[:PART_OF]->(c)
        """,
               id=cat["id"],
               title=lang(cat.get("title", {}), "de", "en"),
               did=dataset_id,
               )

    for entry in d.get("categories", []):
        tx.run("""
            MERGE (cat:Category {id: $id})
            SET cat.label = $label
            WITH cat MATCH (d:Dataset {id: $did})
            MERGE (d)-[:HAS_CATEGORY]->(cat)
        """,
               id=entry["id"],
               label=lang(entry.get("label", {}), "de", "en"),
               did=dataset_id,
               )

    for kw in d.get("keywords", []):
        tx.run("""
            MERGE (k:Keyword {id: $id})
            SET k.label = $label
            WITH k MATCH (d:Dataset {id: $did})
            MERGE (d)-[:HAS_KEYWORD]->(k)
        """,
               id=kw.get("id", kw.get("label", "")),
               label=kw.get("label", ""),
               did=dataset_id,
               )

    for dist in d.get("distributions", []):
        dist_id = dist.get("id")
        if not dist_id:
            continue

        tx.run("""
            MERGE (dist:Distribution {id: $id})
            SET dist.title      = $title,
                dist.modified   = $modified,
                dist.access_url = $url
            WITH dist MATCH (d:Dataset {id: $did})
            MERGE (d)-[:HAS_DISTRIBUTION]->(dist)
        """,
               id=dist_id,
               title=lang(dist.get("title", {}), "de", "en"),
               modified=dist.get("modified", ""),
               url=(dist.get("access_url") or [""])[0],
               did=dataset_id,
               )

        fmt = dist.get("format", {})
        if fmt.get("id"):
            tx.run("""
                MERGE (f:Format {id: $id})
                SET f.label = $label
                WITH f MATCH (dist:Distribution {id: $did})
                MERGE (dist)-[:IN_FORMAT]->(f)
            """, id=fmt["id"], label=fmt.get("label", ""), did=dist_id)

        lic = dist.get("license", {})
        if lic.get("resource"):
            tx.run("""
                MERGE (l:License {resource: $res})
                SET l.label    = $label,
                    l.homepage = $hp
                WITH l MATCH (dist:Distribution {id: $did})
                MERGE (dist)-[:HAS_LICENSE]->(l)
            """,
                   res=lic["resource"],
                   label=lic.get("label", ""),
                   hp=lic.get("homepage", ""),
                   did=dist_id,
                   )


def main():
    import argparse
    parser = argparse.ArgumentParser(description="Bayern Open Data → Neo4j Metadaten-Import")
    parser.add_argument("--neo4j-uri", default="bolt://0.0.0.0:7687", help="Neo4j Bolt URI")
    parser.add_argument("--neo4j-user", default="neo4j", help="Neo4j Username")
    parser.add_argument("--neo4j-password", required=True, help="Neo4j Password")
    parser.add_argument("--dry-run", action="store_true", help="Ohne Schreibzugriff")
    parser.add_argument("--distribution-mode", default="node", choices=["list", "node", "both"], help="Art der Distrubtionen")
    args = parser.parse_args()

    # Dry-Run Dummy für Kompatibilität mit README (ohne Write, nur Read/Verbinden)
    driver = GraphDatabase.driver(args.neo4j_uri, auth=(args.neo4j_user, args.neo4j_password))

    with driver.session() as session:
        if not args.dry_run:
            setup_constraints(session)

        search_results = fetch_search_results(query="Nürnberg", limit=500)

        ok, fail = 0, 0
        for item in search_results:
            dataset_id = item.get("id")
            if not dataset_id:
                continue
            try:
                detail = fetch_dataset_detail(dataset_id)
                if not args.dry_run:
                    session.execute_write(ingest_dataset, detail)
                ok += 1
                log.info(f"  ✓ {dataset_id}")
            except Exception as e:
                fail += 1
                log.warning(f"  ✗ {dataset_id}: {e}")

    driver.close()
    log.info(f"Fertig: {ok} OK, {fail} Fehler")

if __name__ == "__main__":
    main()