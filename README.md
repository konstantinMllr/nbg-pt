# Einmalimport JSON-LD -> Neo4j

Dieses Skript importiert Datensatz-Metadaten von
`https://nuernberg.bydata.de/api/hub/repo/datasets` nach Neo4j.

## Enthalten

- `import_jsonld_to_neo4j.py`: Einmaliger Importer
- `requirements.txt`: Python-Abhaengigkeiten

## Installation

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Dry-Run (ohne Neo4j Schreibzugriff)

```bash
python3 import_jsonld_to_neo4j.py --dry-run
```

## Import nach Neo4j

```bash
python3 import_jsonld_to_neo4j.py \
  --neo4j-uri bolt://localhost:7687 \
  --neo4j-user neo4j \
  --neo4j-password schwanzer \
  --distribution-mode node
```

`--distribution-mode` Optionen:

- `list`: speichert `access_urls` als Liste auf `Dataset`
- `node`: erstellt `(:Distribution {accessURL})` und `(:Dataset)-[:HAS_DISTRIBUTION]->(:Distribution)`
- `both`: macht beides

