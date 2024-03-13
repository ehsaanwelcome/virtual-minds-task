#!/bin/sh

curl -X "POST" "http://localhost:8083/connectors" \
             -H "Content-Type: application/json" \
             -d '{
          "name": "jdbc",
          "config": {
            "value.converter.schema.registry.url": "http://schema-registry:8081",
            "key.converter.schema.registry.url": "http://schema-registry:8081",
            "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
            "tasks.max": "2",
            "key.converter": "io.confluent.connect.avro.AvroConverter",
            "value.converter": "io.confluent.connect.avro.AvroConverter",
            "transforms": "TimestampConverter",
            "topics": "vm-stats",
            "transforms.TimestampConverter.type": "org.apache.kafka.connect.transforms.TimestampConverter$Key",
            "transforms.TimestampConverter.target.type": "Timestamp",
            "transforms.TimestampConverter.field": "time",
            "transforms.TimestampConverter.format": "dd-MM-yyyy HH:mm:ss",
            "connection.url": "jdbc:postgresql://postgres:5432/",
            "connection.user": "postgres",
            "connection.password": "test",
            "insert.mode": "upsert",
            "table.name.format": "hourly_stats",
            "pk.mode": "record_key",
            "auto.create": "false"
          }
        }'