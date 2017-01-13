

.. _adapter-def-config-ref:

Adapter definition
==================

All :ref:`adapter<adapter-term>` objects
used in the cluster are defined in the
:ref:`ClusterConfig.json<clusterconfig-config-ref>` JSON file.
Each input, output, or storage adapter used in the cluster
has its own "Adapter" section,
identified by a unique "Name".

To implement a custom adapter:

- Code the adapter
- Create a definition for the adapter
- Submit the adapter to the metadata using the
  :ref:`kamanja<kamanja-command-ref>` command.

Adapter definitions include some core parameters
that are used for all adapter types
plus some parameters that are specific to each adapter type.

File structure
--------------

::

 "Adapters": [
          ... {some config}
          {
            "Name": "NEW_ADAPTER_NAME",
            "TypeString": "Input",
            "TenantId": "tenant1",
            "ClassName": "com.ligadata.InputAdapters.KafkaSimpleConsumer$",
            "JarName": "KamanjaInternalDeps_2.11-1.4.0.jar",
            "DependencyJars": [
              "ExtDependencyLibs_2.11-1.4.0.jar",
              "ExtDependencyLibs2_2.11-1.4.0.jar"
            ],
            "AdapterSpecificCfg": {
              "HostList": "localhost:9092",
              "TopicName": "testin_1"
            }
          },
          {
            "Name": "TestFailedEvents_1",
            "TypeString": "Output",
            "TenantId": "tenant1",
            "ClassName": "com.ligadata.kafkaInputOutputAdapters_v9.KafkaProducer$",
            "JarName": "kamanjakafkaadapters_0_9_2.11-1.5.3.jar",
            "DependencyJars": [
              "kafka-clients-0.9.0.1.jar",
              "KamanjaInternalDeps_2.11-1.5.3.jar",
              "ExtDependencyLibs_2.11-1.5.3.jar",
              "ExtDependencyLibs2_2.11-1.5.3.jar"
            ],
            "AdapterSpecificCfg": {
              "HostList": "localhost:9092",
              "TopicName": "testfailedevents_1"
            },
            "MessageNames": [
              "com.ligadata.kamanja.samples.messages.COPDOutputMessage"
            ],
            "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
            "Options": {
            "emitSystemColumns": "true"
            }
            ]
          {
            "Name": "Storage_1",
            "TypeString": "Storage",
            "TenantId": "tenant1",
            "StoreType": "h2db",
            "connectionMode": "embedded",
            "SchemaName": "testdata",
            "Location": "/home/flare/Binaries/Kamanja911/Kamanja-1.5.3_2.11/storage/tenant1_storage_1",
            "portnumber": "9100",
            "user": "test",
            "password": "test"
          },
          },
        ],



Parameters
----------

All adapter definitions include the following Core parameters:

- **Name** – name of the adapter.

- **TypeString** - (Required) Type of this adapter.
   Valid values are **input**, **Output**, and **Storage**.

- **TenantId** - ID of the :ref:`tenant<tenancy-term>` used for this adapter;
  see :ref:`tenant-def-config-ref`

Input and output adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Input and output adapters use the Core parameters
plus the following:

- **ClassName** - class that contains the logic for the adapter.
  It should be the full package.className. In example,
  KafkaConsumer$ is for reading from Kafka
  and KafkaProducer$ is for writing to Kafka.

- **JarName** – name of the JAR in which the aforementioned ClassName exists.

- **DependencyJars** - list of JARs on which the adapters JarName jar depends.

- **AdapterSPecificCfg** - configuration that is specific to this
  Input or Output adapter.

  - **HostList** - list of server:ports of Kafka brokers to use

  - **TopicName** - name of the topic or queue from which to read
    or to which to write.


Input adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~

Input adapters use the Core parameters,
the Input and output adapter parameters,
plus the following:

- **DataFormat** -- format used for data passed to the adapter.
  Valid formats are CSV or JSON.


Output adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~

Output adapter definitions use the Core parameters,
the Input and output adapter parameters,
plus the following:

- **NameSpace** – namespace of the output adapter.
- **Name** – name of the output adapter.
- **InputAdapterToVerify** - location the adapter reads
  to verify the completion of outputting alerts and messages.

The following parameters define how :ref:`serialization<serial-deserial-term>`
is implemented for this Output adapter.

- **MessageNames** -- messages affected by these serialization settings
- **Serializer** -- serializer to use.  Valid values are:

  - com.ligadata.kamanja.serializer.jsonserdeser

- **emitSystemColumns** -

  - if set to "false" (default),
    internal system columns are not included in the serialized output.
    This is appropriate if the serialized output will be consumed
    by external systems with no knowledge of internal columns
  - if set to "true",
    internal header columns are included in the serialized output.
    For output that is used in the Kamanja platform,
    this is necessary to restore data properly.

    See :ref:`serial-internal-cols-guide` for more details.

Note that only the JSON serializer supports
including/excluding system columns.


Storage adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~

Storage adapter definitions use the Core parameters
plus the following:

- **StoreType** -
- **connectionMode** -
- **SchemaName** -
- **Location** -
- **portnumber** -
- **user** -
- **password** -


Usage
-----

To add a new adapter object to the cluster:

- add a new ADAPTER object to the ClusterConfig.json configuration file
- submit it to the metadata using
  the :ref:`kamanja<kamanja-command-ref>` upload cluster config command.
  For example:

  ::

      kamanja upload cluster config /tmp/kamanjaInstall/cong/ClusterConfig.json

To update an existing object, update an existing property;
if the adapter object already exists in the system,
then uploading a cluster configuration results in an update operation.

To remove an object (in this case an input adapter),
upload the file with the desired object using
the :ref:`kamanja<kamanja-command-ref>` remove engine config command.
For example:

::

    kamanja remove engine config /tmp/kamanjaInstall/cong/objectsToRemove.json


Any objects present in the JSON dcoument are removed.

If the input adapter definition contains an AssociatedMessage, 
it is called tagged. 
So if the input adapters contain tagged messages, 
add new messages and/or JTMs as appropriate. 
Refer to the JTMs for more information.


Input adapters
~~~~~~~~~~~~~~

Output adapters
~~~~~~~~~~~~~~~

Storage adapters
~~~~~~~~~~~~~~~~


Examples
--------



See also
--------

- :ref:`adapters-input-guide`
- :ref:`adapters-output-guide`
- :ref:`adapters-storage-guide`



