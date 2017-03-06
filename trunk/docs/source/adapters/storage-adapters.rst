
.. _adapters-storage-guide:

Creating custom storage adapters
================================

A storage adapter is an internal interface to the data store for Kamanja.
Examples are :ref:`HBase<hbase-term>`, :ref:`Cassandra<cassandra-term>`,
and the Microsoft :ref:`SQL<sql-term>` server (JDBC). 

It is also possible to add a storage adapter to Kamanja
where it can save information such as :ref:`messages<messages-term>`,
:ref:`containers<container-term>`,
:ref:`model<model-term>` results, and some information saved from models.
Multiple storage adapters means the same information
can be saved to different databases.
Here is a sample :ref`clusterconfig-config-ref>
with differet storage adapters,
to support HBase, Cassandra,
and Microsoft SQL Server.

::

  {
  "Clusters": [
      {
      "ClusterId": "ligadata1",
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

When adding a new storage adapter,
both com.ligadata.keyvaluestore.Transaction
and com.ligadata.keyvaluestore.DataStore need to be implemented.

The following case classes are used to define
Key, Value, and TimeRange objects
that are used to pass the object information to the storage interface
(see below for interface definition).

::

  case class Key(timePartition: Long, bucketKey: Array[String], transactionId: Long, rowId: Int)

  case class Value(serializerType: String, serializedInfo: Array[Byte])

  case class TimeRange(beginTime: Long, endTime: Long)

where:

- **Key** – abstracts the primary key for the underlying table.
- **timePartition** – epoch time for the record (in milliseconds).
- **bucketKey** – multi-valued name of the object
  (such as namespace, name, version).
- **transactionId** – integer value that represents the transaction
  within which this record is added/modified.
- **rowId** – version of the row if all the versions of the same row are kept.
- **Value** – abstracts the actual record that is serialized
  and stored as a blob.
- **serializerType** – name given to the type of serializer
  (such as kryo, avro, etc.).
- **serializedInfo** – serialized record as an array of bytes.
- **TimeRange** – abstracts the range for timePartition value
  on which some of the queries are performed.
  TimeRange values are passed as a parameter in the storage interface API.


.. _kvs-datastore-guide:

Extending com.ligadata.keyvaluestore.DataStore
----------------------------------------------

com.ligadata.keyvaluestore.DataStore is one of the interfaces
that needs to be extended for the storage type.
It has the following methods to override.

Update Operations
~~~~~~~~~~~~~~~~~

::

  def put(containerName: String, key: Key, value: Value): Unit

- **put** – equivalent to the UPSERT(UPDATE or INSERT) operation.
  In other words, update the record if it already exists
  or insert the record if it doesn’t exist.

::

  def put(data_list: Array[(String, Array[(Key, Value)])]): Unit

Bulk put for multiple records at the same time.
data_list has a list of container names
and each container can have a list of key-value pairs.

Delete Operations
~~~~~~~~~~~~~~~~~

::

  def del(containerName: String, keys: Array[Key]): Unit

- **del** – for the given list of keys, delete the rows.


::

  def del(containerName: String, time: TimeRange, keys: Array[Array[String]]): Unit

- **del** – for the given multiple bucket key strings and a TimeRange,
  delete the rows.

Get Operations
~~~~~~~~~~~~~~

All of the following get operations
call the callbackFunction on each record they fetch from the database.

::

  def get(containerName: String, callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container
  (a container translates to a table in most databases).

::

  def get(containerName: String, keys: Array[Key], callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container and a list of keys.

::

  def get(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key, Value) =&gt; Unit): Unit

get – fetch all the records from a given container and a list of TimeRanges.

::

  def get(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container
  and a list of TimeRanges and a list of bucketKey values
  (each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey).

::

  def get(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container
  and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.

The getKeys operations are similar to the get operations,
but only get key values.

::

  def getKeys(containerName: String, callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  (a container translates to a table in most databases).

::

  def getKeys(containerName: String, keys: Array[Key], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container and a list of keys.
  This operation returns only keys that still exist.

::

  def getKeys(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  and a list of TimeRanges.

::

  def getKeys(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  and a list of TimeRanges and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.

::

  def getKeys(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.

Transactional Operations
~~~~~~~~~~~~~~~~~~~~~~~~

::

  def beginTx(): Transaction

- **beginTx** – begin transaction on the data store.

::

  def commitTx(tx: Transaction): Unit

- **commitTx** – commit the given transaction.

::

  def endTx(tx: Transaction): Unit

- **endTx** – same as CommitTx, not a rollback.

::

  def rollbackTx(tx: Transaction): Unit

- **rollbackTx** – roll back a given transaction.

Clean-up Operations
~~~~~~~~~~~~~~~~~~~

::

  def Shutdown(): Unit

- **Shutdown** – shut down all operations working
  on this data store (database).

::

  def TruncateContainer(containerNames: Array[String]): Unit

- **TruncateContainer** – truncate the given container, making it empty.

::

  def DropContainer(containerNames: Array[String]): Unit

- **DropContainer** – drop a given list of containers.
  Drops the related tables from the database.

::

  def CreateContainer(containerNames: Array[String]): Unit

- **CreateContainer** – create a table for each container
  of a given list of containers.

  This operation can be used to create necessary indices
  as well as partitioning structures.

.. _kvs-transaction-guide:

Extending com.ligadata.keyvaluestore.Transaction
------------------------------------------------

com.ligadata.keyvaluestore.Transaction also needs be extended
for a new storage type.
This interface has a member that is nothing
but a pointer to the DataStore interface.
Typically, each of the following API functions
calls the identical function within the DataStore interface,
but within a transactional context (beginTx, commitTx).
It has the following methods to override.

Update Operations
~~~~~~~~~~~~~~~~~

::

  def put(containerName: String, key: Key, value: Value): Unit

- **put** – equivalent to UPSERT(UPDATE or INSERT) operation.
  In other words, update the record if it already exists
  or insert the record if it doesn’t exist.

::

  def put(data_list: Array[(String, Array[(Key, Value)])]): Unit

- bulk **put** for multiple records at the same time.
  data_list has a list of container names
  and each container can have a list of key-value pairs.

Delete Operations
~~~~~~~~~~~~~~~~~

::

  def del(containerName: String, keys: Array[Key]): Unit

- **del** – for the given list of keys, delete the rows.

::

  def del(containerName: String, time: TimeRange,
     keys: Array[Array[String]]): Unit

- **del** – for the specified multiple bucket key strings and a TimeRange,
  delete the rows.

Get Operations
~~~~~~~~~~~~~~

All of the following get operations call the callbackFunction
on each record they fetch from the database.

::

  def get(containerName: String, callbackFunction:
     (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container.
  A container translates to a table in most databases.

::

  def get(containerName: String, keys: Array[Key],
     callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container and a list of keys.

::

  def get(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container
  and a list of TimeRanges.

::

  def get(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container
  and a list of TimeRanges and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.


::

  def get(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) =&gt; Unit): Unit

- **get** – fetch all the records from a given container
  and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.

- **getKeys** – similar to get, but only get key values.

::

  def getKeys(containerName: String, callbackFunction: (Key) =&gt; Unit): Unit

- **getKey** – fetch all the keys from a given container
  (a container translates to a table in most databases).

::

  def getKeys(containerName: String, keys: Array[Key], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container and a list of keys.
  This operation returns only keys that still exist.

::

  def getKeys(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  and a list of TimeRanges.

::

  def getKeys(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  and a list of TimeRanges and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.

::

  def getKeys(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key) =&gt; Unit): Unit

- **getKeys** – fetch all the keys from a given container
  and a list of bucketKey values.
  Each bucketKey can be multi-valued
  and Array[String] represents a single instance of bucketKey.

.. _sql-storage-guide:

Microsoft SQL Server Adapter Implementation
-------------------------------------------

The Microsoft SQL Server is one of the most successful relational databases
in today’s market.
Kamanja is packaged with several storage adapters
including Cassandra, Hbase, and Microsoft SQL server.

The following steps are required
to enable Kamanja to store metadata and/or other container output data
in the Microsoft SqlServer.

.. _metadata-sql:

Changes to MetadataAPIConfig.properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Modifications are required to
the :ref:`metadataapiconfig-config-ref` file.
The following discussion is also applicable to Engine1Config.properties.

The MetadataDataStore property is a JSON string
that should contain the following elements for using the sqlserver adapter:

- **StoreType** – set to sqlserver.
- **hostname** – host name (or IP address) of sqlserver host.
- **instance name** – name of specific SQL server instance (optional).
- **port number** – port on which sqlserver is listening for requests.
  This parameter is optional; it defaults to 1433.
- **database** – SQL server database name.
- **user** – SQL server login user.
- **password** – SQL server login password.
- **jarpaths** – specfic directory that contains the sqlserver JDBC JAR.
- **jdbcJar** – sqlserver JDBC driver file.
- **maxActiveConnections** – maximum number of active connections
  to the SQL server, used by connection pooling.
  This parameter is optional; it defaults to 20.
- **maxIdleConnections** – maximum number of idle connections
  to the SQL server, used by connection pooling.
  This parameter is optional; it defaults to 10.
- **initialSize** – number of connections created
  when the client sets up a connection for the first time
  using the connection pool object, used by connection pooling
  This parameter is optional; it defaults to 10.

Example: (without specifying instance name and port)

::

  MetadataDataStore={"StoreType": "sqlserver","hostname": "192.168.56.1",
    "database": "bank","user":"bank","password":"bankuser",
    "jarpaths":"/media/home2/java_examples/sqljdbc_4.0/enu",
    "jdbcJar":"sqljdbc4.jar"}

Example: (specifying instance name and port)

::
  MetadataDataStore={"StoreType": "sqlserver","hostname": "192.168.56.1",
    "instancename":"kamanja","portnumber":"1433","database": "bank",
    "user":"bank","password":"bankuser",
    "jarpaths":"/media/home2/java_examples/sqljdbc_4.0/enu","jdbcJar":"sqljdbc4.jar"}

Example: (specifying connection pooling properties as well)

::

  MetadataDataStore={"StoreType": "sqlserver","hostname": "192.168.56.1",
    "instancename":"kamanja","portnumber":"1433","database": "bank","user":"bank",
    "password":"bankuser",
    "jarpaths":"/media/home2/java_examples/sqljdbc_4.0/enu","jdbcJar":"sqljdbc4.jar","maxActiveConnections":"20","maxIdleConnections":"10","initialSize":"10"}

SQL Server JDBC Driver Download Link
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You must download and install the SQL server JDBC driver:

#. Download the JDBC driver from
   `<https://www.microsoft.com/en-us/download/details.aspx?displaylang=en&id=11774>`_

   Version 4.2 is recommended.
   The self-extracting JAR file is named *sqljdbc_4.2.6420.100_enu.exe*.

#. Copy the sqljdbc4-2.0.jar driver JAR to *$KAMANJA_HOME/lib/system*.


