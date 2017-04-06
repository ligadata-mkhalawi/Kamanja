
.. _ingest-data-model:

Ingest data
===========

Data ingestion is the process of bringing data from external sources
into the Kamanja platform.  To do this:

- Ingest the data using one of the following methods:

  - Create a :ref:`smart file input adapter<smart-input-adapter-term>`
    that pulls new messages onto the Kamanja platform for processing.
    The input adapter watches the specified folders constantly;
    whenever it finds a new file in one of these folders,
    it reads the file and sends it to the Kamaja engine.
  - Call the :ref:`KVInit<kvinit-command-ref>` tool to ingest data
    into the specified Kafka :ref:`topic<topic-term>`

- Create a Kamanja input message
  that defines each record or table structure,
  with names and types for each field/column.
  Each message is defined in a JSON file.
  The data (including any data transformations and extensions
  that have been defined) is fed into Kamanja as input messages.


