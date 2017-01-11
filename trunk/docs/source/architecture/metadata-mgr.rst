
.. _metadata-mgr-arch:

Metadata manager
================

The metadata manager handles offline, compile-time, processing.
The metadata maintained and managed within Kamanja
consists of all the information that the engine needs
to process incoming messages, including the models,
information about message types, and configuration information.

The metadata manager performs a number of functions:

- Allows CRUD operations on object definitions
  that are instantiated and used during runtime by the Kamanja engine.
  An example is adding a message definition,
  so that an instance of this message object
  can be instantiated and used during by the runtime system.
  These are the Kamanja objects:

  - :ref`messages<messages-term>`

    - input messages  - data flowing into Kamanja engine
    - output messages - data flowing out of models

  - :ref`containers<containers-term>`  - “facts” about Kamanja models
  - :ref`models<model-term>` - executable logic (rules, TreeSets, etc)
  - :ref`functions<functions-term>` - UDFs
  - :ref`types<types-term>` - datatypes
  - :ref`concepts<concepts-term>`

- Allows uploading of JAR files that are used during Kamanja execution.
  For example, if a UDF is defined,
  the JAR file that contains the code needs to be uploaded,
  and then a function definition about that code needs to be added.
  At runtime, Scala reflections are used to instantiate and call the executable.
- Allows uploading of various configurations.
  For example, Kamanja runtime cluster configuration,
  which is required to run the Kamanja engine, Kafka input/output adapters,
  or model configurations,
  which are required to compile custom Java/Scala models.

There are 2 ways of interacting with the Kamanja metadata.

- :ref:`Metadata API<metadataapi-term>` - this service processes REST requests
- :ref:`CLI commands<kamanja-command-ref>` - Kamanja utilities

Use the Kamanja utilities to start the metadata REST service.

