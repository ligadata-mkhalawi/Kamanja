

.. _read-to-metadata-api:

Reading objects to the metadata
===============================

To read objects from the Kamanja metadata,
the key of the specific object to access needs to be known.
If not known, issue a call to get all keys of a given object type.

Note: For now, filtering within an object type is not supported;
models in the acmeInc namespace cannot be seen.

Seeing Messages
---------------

To see all the messages in a metadata server:

- Method: GET
- URL Structure: https//host/api/keys/message
- Parameters: None
- Returns: A JSON representation of APIResults.

To see the output message in a metadata server:

- Method: GET
- URL Structure: https//host/api/keys/outputmsg/
- Parameters: None
- Returns: A JSON representation of APIResults.

Once the object key is obtained,
retrieve the actual object from the metadata server.

Seeing Containers
-----------------

To see all the containers in a metadata server:

- Method: GET
- URL Structure: https//host/api/keys/container
- Parameters: None
- Returns: A JSON representation of APIResults.

Seeing Models
-------------

To see all the models in a metadata server:

- Method: GET
- URL Structure: https//host/api/keys/model
- Parameters: None
- Returns: A JSON representation of APIResults.

Seeing Functions
----------------

To see all the functions in a metadata server:

- Method: GET
- URL Structure: https//host/api/keys/function
- Parameters: None
- Returns: A JSON representation of APIResults.

Seeing Types
------------

To see all types in a metadata server:

- Method: GET
- URL Structure: https//host/api/keys/type
- Parameters: None
- Returns: A JSON representation of APIResults.

Seeing Concepts
---------------

To see all the concepts in a metadata server

- Method: GET
- URL Structure: https//host/api/keys/concept
- Parameters: None
- Returns: A JSON representation of APIResults.

Once the object key is obtained,
retrieve the actual object from the metadata server.

Retrieving Messages
-------------------

To retrieve a message from the metadata server:

- Method: GET
- URL Structure: https://host/api/message/
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Containers
---------------------

To retrieve a container from the metadata server:

- Method: GET
- URL Structure: https://host/api/container/
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Models
-----------------

To retrieve a model from the metadata server:

- Method: GET
- URL Structure: https://host/api/model/
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Functions
--------------------

To retrieve a function from the metadata server:

- Method: GET
- URL Structure: https://host/api/function/
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Types
----------------

To retrieve a type from the metadata server:

- Method: GET
- URL Structure: https://host/api/type/key>
- Parameters: None
- Returns: A JSON representation of APIResults.

Retrieving Concepts
-------------------

To retrieve a concept from the metadata server:

- Method: GET
- URL Structure: https://host/api/concept/key
- Parameters: None
- Returns: A JSON representation of APIResults.


