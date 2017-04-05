

.. _storage-adapter-ref:

Storage Adapter
===============

A storage adapter is an internal interface to the data store for Kamanja.
Examples are :ref:`HBase<hbase-term>`, :ref:`Cassandra<cassandra-term>`,
and the Microsoft :ref:`SQL<sql-term>` server (JDBC).
In Kamanja 1.6.3 and later,
the Hbase Fail over feature can be implemented;
this allows the Kamanja Engine to process the messages
even if the HBase is down.

It is also possible to add a storage adapter to Kamanja
where it can save information such as :ref:`messages<messages-term>`,
:ref:`containers<container-term>`,
:ref:`model<model-term>` results, and some information saved from models.
Multiple storage adapters means the same information
is saved to different databases.

HBase failover can be implemented in the storage adapter
to prevent against data loss.

Storage adapters are configured in
the "Adapters" section of the :ref:`clusterconfig-config-ref` file.

Storage Adapter structure
-------------------------

Here is a sample Storage Adapter definition,
providing different storage adapters
to support HBase, Cassandra, and Microsoft SQL Server.
HBase failover is implemented.

::

  {
    "Clusters": [
        {
        "ClusterId": "ligadata1",
        ...
        "CommitOffsetsMsgCnt": 10000,
        "CommitOffsetsTimeInterval": 5000,
        "EnableEachTransactionCommit": false,
        "PostAdapterInfoTime": 10000,
        "WriteAdapterInfoTime": 10000,
        "AdapterInfoWriteLocation":"/data/node",
    "Adapters": [
        {
            "Name": "Storage_1",
            "TypeString": "Storage",
            "TenantId": "tenant1",
            "StoreType": "hashmap",
            "SchemaName": "testdata1",
            "Location": "{InstallDirectory}/storage/tenant1_storage_1"
        },
        {
            "Name": "Storage_2",
            "TypeString": "Storage",
            "TenantId": "tenant1",
            "StoreType": "hbase",
            "SchemaName": "testdata2",
            "Location": "localhost"
        },
        {
            "Name": "Storage_3",
            "TypeString": "Storage",
            "TenantId": "tenant1",
            "StoreType": "hbase",
            "SchemaName": "testdata3",
            "Location": "localhost"
        },
        {
            "Name": "Storage_4",
            "TypeString": "Storage",
            "TenantId": "tenant1",
            "StoreType": "cassandra",
            "SchemaName": "testdata4",
            "Location": "localhost"
        }
        ]
    }
    ]
    }



Parameters
----------

.. _hbase-failover-parameters:

HBase failover parameters
~~~~~~~~~~~~~~~~~~~~~~~~~

- **CommitOffsetsMsgCnt** - number of transactions to queue
  and then write to local disk.
  When this number of transactions are queued,
  they are committed even if the **CommitOffsetsTimeInterval** interval
  has not expired.
- **CommitOffsetsTimeInterval** - interval (in milliseconds)
  between writing the queued transactions to disk.
  All transactions that are queued are committed at this interval,
  whether or not the **CommitOffsetsMsgCnt** limit has been reached.
- **EnableEachTransactionCommit** - If set to true,
  each individual transaction is committed rather than being
  queued for a batch commit.
  default value is false.
- **PostAdapterInfoTime** - interval, in milliseconds,
  for posting the updated partition information.
- **WriteAdapterInfoTime** - interval, in milliseconds,
  for writing the partition information to the local drive
- **AdapterInfoWriteLocation** - directory where the *nodename* JSON file
  that contains the adapter partition distribution information
  as well as the five most recent JSON files.


Usage
-----

The HBase failover feature is used in conjunction
with the :ref:`failover-nodes-term` feature
to prevent data loss while a standby node is activated.

.. _hbase-failover-description:

How HBase failover works
~~~~~~~~~~~~~~~~~~~~~~~~

HBase failover is implemented by creating a JSON file called *nodename*
in the directory specified to the **AdapterInfoWriteLocation** parameter.
This file  contains the adapter partition distribution information
(key, key value, node id, uuid, nodestartime and unique counter).
One record looks like the following:

::

  "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\
      "TopicName\":\"helloworldinput\",\"PartitionId\":3}",
          "keyvalue": "{\"Version\":1,\"Offset\":14}",
          "nodeid": "2",
          "uuid": "aa075745-a267-4253-bb46-32934556f89e",
          "nodestarttime": 1491209137977,
          "uniquecounter": 64

It also contains a backup of the most five most recent JSON files processed.

This partition distribution information is written to local drive
for **WriteAdapterInfoTime** milliseconds,
then posted for every **PostAdapterInfoTime** millisecs.
If a node fails,
the Kamanja engine can process these messages after it is restarted
by picking up the key and key values from this locally stored file.

Example nodename file
---------------------

::

  {
    "keyvalues": [
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":3}",
        "keyvalue": "{\"Version\":1,\"Offset\":14}",
        "nodeid": "2",
        "uuid": "aa075745-a267-4253-bb46-32934556f89e",
        "nodestarttime": 1491209137977,
        "uniquecounter": 64
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":6}",
        "keyvalue": "{\"Version\":1,\"Offset\":20}",
        "nodeid": "1",
        "uuid": "06383c9a-7d9d-4507-aa36-77caa3f3ee23",
        "nodestarttime": 1491208422285,
        "uniquecounter": 5
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":0}",
        "keyvalue": "{\"Version\":1,\"Offset\":23}",
        "nodeid": "2",
        "uuid": "aa075745-a267-4253-bb46-32934556f89e",
        "nodestarttime": 1491209137977,
        "uniquecounter": 64
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":5}",
        "keyvalue": "{\"Version\":1,\"Offset\":17}",
        "nodeid": "2",
        "uuid": "aa075745-a267-4253-bb46-32934556f89e",
        "nodestarttime": 1491209137977,
        "uniquecounter": 64
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":2}",
        "keyvalue": "{\"Version\":1,\"Offset\":17}",
        "nodeid": "1",
        "uuid": "06383c9a-7d9d-4507-aa36-77caa3f3ee23",
        "nodestarttime": 1491208422285,
        "uniquecounter": 5
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":7}",
        "keyvalue": "{\"Version\":1,\"Offset\":20}",
        "nodeid": "2",
        "uuid": "aa075745-a267-4253-bb46-32934556f89e",
        "nodestarttime": 1491209137977,
        "uniquecounter": 64
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":4}",
        "keyvalue": "{\"Version\":1,\"Offset\":17}",
        "nodeid": "1",
        "uuid": "06383c9a-7d9d-4507-aa36-77caa3f3ee23",
        "nodestarttime": 1491208422285,
        "uniquecounter": 5
      },
      {
        "key": "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",
              \"TopicName\":\"helloworldinput\",\"PartitionId\":1}",
        "keyvalue": "{\"Version\":1,\"Offset\":17}",
        "nodeid": "1",
        "uuid": "06383c9a-7d9d-4507-aa36-77caa3f3ee23",
        "nodestarttime": 1491208422285,
        "uniquecounter": 5
      }
    ]
  }


Differences between versions
----------------------------

HBase failover is supported only for Kamanja 1.6.3 and later releases.

See also
--------

- :ref:`adapter-binding-config-ref`
- :ref:`adapter-def-config-ref`

