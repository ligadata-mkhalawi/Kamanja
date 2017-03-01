
.. _write-to-metadata-api:

Writing objects to the metadata
===============================

To upload Kamanja objects into Kamanja metadata,
use the following APIs:

Uploading and Updating Messages
-------------------------------

To upload a new message object to the metadata server:

- Method: POST
- URL Structure: https://host/api/message
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To upload a new output message object to the metadata server:

- Method: POST
- URL Structure: https://host/api/outputmsg
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing message object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/message
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing output message object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/outputmsg
- Parameters: The content of the file must be in the body of the PUT request.
- Returns: A JSON representation of APIResults.

Uploading and Updating Containers
---------------------------------

To upload a new container object to the metadata server:

- Method: POST
- URL Structure: https://host/api/container
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing container object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/container
- Parameters: The content of the file must be in the body of the PUT request.
- Returns: A JSON representation of APIResults.

Uploading and Updating Models
-----------------------------

To upload a new model object to the metadata server:

- Method: POST
- URL Structure: https://host/api/model
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing model object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/model
- Parameters: The content of the file must be in the body of the PUT request.
- Returns: A JSON representation of APIResults.

To upload a new Java model object to the metadata server:

- Method: POST
- URL Structure: https://host/api/model/java
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing Java model object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/model/java
- Parameters: The content of the file must be in the body of the PUT request.
- Returns: A JSON representation of APIResults.

To upload a new Scala model object to the metadata server:

- Method: POST
- URL Structure: https://host/api/model/scala
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing Scala model object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/model/scala
- Parameters: The content of the file must be in the body of the PUT request.
- Returns: A JSON representation of APIResults.

Use the REST API to activate/deactivate a model.

Activating Models
-----------------

To activate an existing model object in the metadata server:

- Method: PUT
- URL Structure: https://host/api/activate/model/
- Parameters: None
- Returns: A JSON representation of APIResults.

Deactivating Models
-------------------

To deactivate an existing model object in the metadata server:

- Method: PUT
- URL Structure: https://host/api/deactivate/model/
- Parameters: None
- Returns: A JSON representation of APIResults.

Note: Currently, only models can be activated or deactivated.

Uploading and Updating Functions
--------------------------------

To upload a new function object to the metadata server:

- Method: POST
- URL Structure: https://host/api/function
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing function object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/function
- Parameters: The content of the file must be in the body of the PUT request.
- Returns: A JSON representation of APIResults.

Uploading and Updating Types
----------------------------

To upload a new type object to the metadata server:

- Method: POST
- URL Structure: https://host/api/type
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing type object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/type
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

Uploading and Updating Concepts
-------------------------------

To upload a new concept object to the metadata server:

- Method: POST
- URL Structure: https://host/api/concept
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.

To update an existing concept object to the metadata server:

- Method: PUT
- URL Structure: https://host/api/concept
- Parameters: The content of the file must be in the body of the POST request.
- Returns: A JSON representation of APIResults.


