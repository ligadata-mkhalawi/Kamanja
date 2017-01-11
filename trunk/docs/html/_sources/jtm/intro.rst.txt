

.. _jtm-intro:

Introduction
============

JTM (JSON Transformation Model) is a modeling language
that is based on JSON map and list (or array) expressions.
JTM is particularly useful for preparing data for analysis by other models.

Like all other Kamanja models,
JTMs accept one or more input messages and create one or more output messages.
Current capabilities include:

- transform an incoming field to another type. Examples:

  - A timestamp represented as a Long integer in the input can be formatted
    as a string using a formatting function
    from one of the date packages available for Scala and/or Java.
   - A comma-delimited string from the input can be split
     to form an array of strings for an output message.
   - An incoming field can serve as a key to a table or map lookup
     using the value found in an output message.

- perform an operation or series of operations on one or more input fields
  to create a new field for an output message.
  Virtually any Java or Scala library can be used
  to perform arbitrary transformations.
- project a subset of the incoming message fields to an output message.
- join an incoming message with fields from another message,
  using one or more field values as keys.
- doing time- and key-based aggregations
  where the {count, sum, avg, mean, mode} field values
  run over a rolling time window.

How JTM Works
-------------

The JTMs:

- compiles strings of JSON found in a file
- translates them to Scala source
- compiles the Scala source
- creates a model definition that is cataloged in the Kamanja metadata store
-  generates a JAR file that contains the executable model

After successfully ingesting the model,
The Kamanja engine processes a JTM the same way it handles
any other model:

- When a new model that is added, the Kamanja engine is notified
  and loads it.
- At boot time, the Kamanja engine consults the metadata store
  and loads all the models found there, including this one. 

Use the following commands to add, update, and remove a model to the cluster:

::

  kamanja <metadata-api-properties-file> add model jtm <jtm-spec-file> TENANTID
  kamanja <metadata-api-properties-file> update model jtm <jtm-spec-file> TENANTID
  kamanja <metadata-api-properties-file> remove model <namespace.name.version>



