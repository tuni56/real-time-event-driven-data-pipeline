# Schema Registry Feature

## Overview
Implements Confluent Schema Registry for Avro schema management and evolution.

## Features Added
- Schema Registry service in Docker Compose
- Avro schema definitions
- Schema evolution support
- Type-safe serialization/deserialization

## Configuration
- Schema Registry URL: http://localhost:8082
- Avro schemas stored in `/src/main/avro/`
- Backward compatibility enabled

## Usage
```bash
# Register schema
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data @event.avsc http://localhost:8082/subjects/events-value/versions

# List schemas
curl http://localhost:8082/subjects
```
