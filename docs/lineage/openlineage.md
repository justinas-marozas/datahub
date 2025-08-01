# OpenLineage

DataHub, now supports [OpenLineage](https://openlineage.io/) integration. With this support, DataHub can ingest and display lineage information from various data processing frameworks, providing users with a comprehensive understanding of their data pipelines.

## Features

- **REST Endpoint Support**: DataHub now includes a REST endpoint that can understand OpenLineage events. This allows users to send lineage information directly to DataHub, enabling easy integration with various data processing frameworks.

- **[Spark Event Listener Plugin](https://docs.datahub.com/docs/metadata-integration/java/acryl-spark-lineage)**: DataHub provides a Spark Event Listener plugin that seamlessly integrates OpenLineage's Spark plugin. This plugin enhances DataHub's OpenLineage support by offering additional features such as PathSpec support, column-level lineage, patch support and more.

## OpenLineage Support with DataHub

### 1. REST Endpoint Support

DataHub's REST endpoint allows users to send OpenLineage events directly to DataHub. This enables easy integration with various data processing frameworks, providing users with a centralized location for viewing and managing data lineage information.

With Spark and Airflow we recommend using the Spark Lineage or DataHub's Airflow plugin for tighter integration with DataHub.

#### How to Use

To send OpenLineage messages to DataHub using the REST endpoint, simply make a POST request to the following endpoint:

```
POST GMS_SERVER_HOST:GMS_PORT/openapi/openlineage/api/v1/lineage
```

Include the OpenLineage message in the request body in JSON format.

Example:

```json
{
  "eventType": "START",
  "eventTime": "2020-12-28T19:52:00.001+10:00",
  "run": {
    "runId": "d46e465b-d358-4d32-83d4-df660ff614dd"
  },
  "job": {
    "namespace": "workshop",
    "name": "process_taxes"
  },
  "inputs": [
    {
      "namespace": "postgres://workshop-db:None",
      "name": "workshop.public.taxes",
      "facets": {
        "dataSource": {
          "_producer": "https://github.com/OpenLineage/OpenLineage/tree/0.10.0/integration/airflow",
          "_schemaURL": "https://raw.githubusercontent.com/OpenLineage/OpenLineage/main/spec/OpenLineage.json#/definitions/DataSourceDatasetFacet",
          "name": "postgres://workshop-db:None",
          "uri": "workshop-db"
        }
      }
    }
  ],
  "producer": "https://github.com/OpenLineage/OpenLineage/blob/v1-0-0/client"
}
```

##### How to set up Airflow

Follow the Airflow guide to setup the Airflow DAGs to send lineage information to DataHub. The guide can be found [here](https://airflow.apache.org/docs/apache-airflow-providers-openlineage/stable/guides/user.html).
The transport should look like this:

```json
{
  "type": "http",
  "url": "https://GMS_SERVER_HOST:GMS_PORT/openapi/openlineage/",
  "endpoint": "api/v1/lineage",
  "auth": {
    "type": "api_key",
    "api_key": "your-datahub-api-key"
  }
}
```

#### How to modify configurations

To modify the configurations for the OpenLineage REST endpoint, you can change it using environment variables. The following configurations are available:

##### DataHub OpenLineage Configuration

This document describes all available configuration options for the DataHub OpenLineage integration, including environment variables, application properties, and their usage.

##### Configuration Overview

The DataHub OpenLineage integration can be configured using environment variables, application properties files (`application.yml` or `application.properties`), or JVM system properties. All configuration options are prefixed with `datahub.openlineage`.

##### Environment Variables

| Environment Variable                                   | Property                                               | Type    | Default | Description                                                     |
| ------------------------------------------------------ | ------------------------------------------------------ | ------- | ------- | --------------------------------------------------------------- |
| `DATAHUB_OPENLINEAGE_PLATFORM_INSTANCE`                | `datahub.openlineage.platform-instance`                | String  | `null`  | Specific platform instance identifier                           |
| `DATAHUB_OPENLINEAGE_COMMON_DATASET_PLATFORM_INSTANCE` | `datahub.openlineage.common-dataset-platform-instance` | String  | `null`  | Common platform instance for datasets                           |
| `DATAHUB_OPENLINEAGE_MATERIALIZE_DATASET`              | `datahub.openlineage.materialize-dataset`              | Boolean | `true`  | Whether to materialize dataset entities                         |
| `DATAHUB_OPENLINEAGE_INCLUDE_SCHEMA_METADATA`          | `datahub.openlineage.include-schema-metadata`          | Boolean | `true`  | Whether to include schema metadata in lineage                   |
| `DATAHUB_OPENLINEAGE_CAPTURE_COLUMN_LEVEL_LINEAGE`     | `datahub.openlineage.capture-column-level-lineage`     | Boolean | `true`  | Whether to capture column-level lineage information             |
| `DATAHUB_OPENLINEAGE_FILE_PARTITION_REGEXP_PATTERN`    | `datahub.openlineage.file-partition-regexp-pattern`    | String  | `null`  | Regular expression pattern for file partition detection         |
| `DATAHUB_OPENLINEAGE_USE_PATCH`                        | `datahub.openlineage.use-patch`                        | Boolean | `false` | Whether to use patch operations for lineage/incremental lineage |

#### Known Limitations

With Spark and Airflow we recommend using the Spark Lineage or DataHub's Airflow plugin for tighter integration with DataHub.

- **[PathSpec](https://docs.datahub.com/docs/metadata-integration/java/acryl-spark-lineage/#configuring-hdfs-based-dataset-urns) Support**: While the REST endpoint supports OpenLineage messages, full [PathSpec](https://docs.datahub.com/docs/metadata-integration/java/acryl-spark-lineage/#configuring-hdfs-based-dataset-urns)) support is not yet available in the OpenLineage endpoint but it is available in the DataHub Cloud Spark Plugin.

etc...

### 2. Spark Event Listener Plugin

DataHub's Spark Event Listener plugin enhances OpenLineage support by providing additional features such as PathSpec support, column-level lineage, and more.

#### How to Use

Follow the guides of the Spark Lineage plugin page for more information on how to set up the Spark Lineage plugin. The guide can be found [here](https://docs.datahub.com/docs/metadata-integration/java/acryl-spark-lineage)

## References

- [OpenLineage](https://openlineage.io/)
- [DataHub OpenAPI Guide](../api/openapi/openapi-usage-guide.md)
- [DataHub Spark Lineage Plugin](https://docs.datahub.com/docs/metadata-integration/java/acryl-spark-lineage)
