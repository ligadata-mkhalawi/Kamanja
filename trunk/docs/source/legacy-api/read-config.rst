
.. _read-config-api:

Read configuration from metadata
================================

The Kamanja metadata server is also used to store engine configuration.
To retrieve it, use the following APIs.

Retrieving engineConfig
-----------------------

To get the entire engineConfig from the metadata server:

- Method: GET
- URL Structure: https://host/api/config/all
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Adapters
-------------------

To get the adapters defined in the engineConfig from the metadata server:

- Method: GET
- URL Structure: https://host/api/config/adapter
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Nodes
----------------

To get the nodes defined in engineConfig from the metadata server:

- Method: GET
- URL Structure: https://host/api/config/node
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Clusters
-------------------

To get the clusters defined in the engineConfig from the metadata server:

- Method: GET
- URL Structure: https://host/api/config/cluster
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving clustercfg
---------------------

To get the clustercfg defined in the engineConfig from the metadata server:

- Method: GET
- URL Structure: https://host/api/config/clustercfg
- Parameters: None
- Returns: A JSON representation of APIResults.


