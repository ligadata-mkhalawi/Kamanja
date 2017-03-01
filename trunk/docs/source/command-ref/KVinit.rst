

.. _kvinit-command-ref:

KVInit
======

Ingest data into the specified Kafka :ref:`topic<topic-term>`

Syntax
------

::

  java -jar /tmp/KamanjaInstall/KVInit-<version> \
     --typename <container-or-message \
     --config /tmp/KamanjaInstall/EngineConfig.cfg \
     --datafiles <datasource> \
     --keyfieldname <Id> [--onlyappend]

Options and arguments
---------------------

- **version** - version of the command to use.
  Currently, only 1.0 is recognized
- **typename** - fully qualified path to the :ref:`container<container-term>`
  or :ref:`message<messages-term>`
- **config** - full pathname of the configuration file
  that defines the jarpaths, metadata store information,
  and data store information.
- **datafiles** - full path of the input file
- **keyfieldname** - Specify the key field name to use.
  This must be one of the fields in the
  first line (column headers) of the file specified for **--datafiles**.
  Failure to find this name causes the command to terminate
  without creating the KV store.
- **onlyappend** - If specified, data can be appended to the data store
  without verifying that this data is not already in the data store.
  The existing conainer must have a primary key
  in order to use this option.
  Note that using this option may create duplicate rows in the container
  if data that is already in the container is added with this option.

Usage
-----

KVInit ingests data into a specified Kafka topic
from which Kamanja can read the data as an input message.

The datastore being input must be a CSV file
whose first row is a header row that gives the column names.
JSON, XML, Avro, and other file formats are not currently supported.

Returns
-------

KVInit creates a "hash mapdb" kv store
based on the information that is provided.
The name of the kv store is "classname" without the full path.


Examples
--------

::

  java -jar /tmp/KamanjaInstall/KVInit-1.0 --typename System.TestContainer \
     --config /tmp/KamanjaInstall/EngineConfig.cfg \
     --datafiles /tmp/KamanjaInstall/sampledata/TestContainer.csv \
     --keyfieldname Id

See also
--------

:ref:`ingest-data-model`


