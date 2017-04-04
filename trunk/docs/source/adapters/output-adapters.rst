
.. _adapters-output-guide:

Creating custom output adapters
===============================

An output adapter is a destination for results produced by model execution.
Examples are Kafka, MQ, and HDFS.

In order to understand output adapters,
be familiar with concepts such as Kafka topics, partitions, and offsets.
Read `<http://kafka.apache.org/documentation.html>`_ carefully.

Introduction to Output Adapters
-------------------------------

Implementing an output adapter is a relatively easy task,
at least compared to implementing a custom consumer.
The custom code needs to implement
the com.ligadata.KamanjaBase.OutputAdapter Scala trait.
The two send methods and the Shutdown method
in the following example must be overwritten by the customer implementation.

::

  trait OutputAdapter {
    val inputConfig: AdapterConfiguration // Configuration
    def send(message: String, partKey: String): Unit
    def send(message: Array[Byte], partKey: Array[Byte]): Unit
    def Shutdown: Unit
    def Category = "Output"

The send method needs to insert KeyedMessage
into a specified message destination object.
See Kafka documentation for instructions
on how to instantiate and use Kafka producer objects).

The constructor for this producer must have two arguments:

- AdapterConfiguration
- CountersAdapter

The AdapterConfiguration and the CountersAdapter objects
are described in the Kafka consumer section.

.. _oracle-output-adapter-api:

Oracle Adapter API
------------------

The :ref:`Oracle Output Adapter<oracle-output-adapter-ref>`
uses a :ref:`storage adapter<adapters-storage-guide>` component
that implements the following basic API.

::

  trait OutputAdapter{
    // Configuration
    val inputConfig: AdapterConfiguration
    override final def getAdapterName = inputConfig.Name
    def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit
    def Shutdown: Unit
  }

The OracleOutputAdapter uses an Oracle storage adapter component
that implements the following basic API.

::

  def createTable(containerName: String, columnNames: Array[String],
     columnTypes: Array[String], keyColumns: Array[String],
     apiType:String): Unit

::

  def put(containerName: String, columnNames:Array[String],
     rowColumnValues: Array[Array[(String,String)]]): Unit

::

  def get(containerName: String, selectList: Array[String],
     filterColumns:Array[(String,String)],
     callbackFunction: (String, Int, String, String) => Unit

**createTable**: create a table 

- **containerName**: Can be a className of the container object.
  However, the Oracle table name is restricted to 30 characters
  so the package name of the class name cannot be used.
  For example, if the container name is
  "com.ligadata.messages.ParameterContainer",
  we recommend passing "ParameterContainer" as the value of this parameter
  rather than a string such as
  com_ligadata_messages_ParameterContainer,
  which exceeds the 30-character limit..

- **columnNames**: Array of strings that represent column names;
  each column name must be less than 30 characters.

- **columnTypes**: Array of strings that represent column types.
  These are Oracle data types used in a create table statement
  such as "number", "varchar2(100)".

- **keyColumns**: Array of string that represents columns
  that are part of primary key (one or more columns)

- **apiType**: This parameter along with configuration paramter
  can prevent creation of tables by this API ( where table creation is considered as DBA function)

**put**: insert rows into an Oracle table

- **containerName**: Name of the table that was used
  in the API "createTable".

- **columnNames**: Array of columns into which we insert the data.
  A table must have been created before using this API.
  If the table does not already exist, this API throws an error.
  This array can be a subset of the all the table columns.
  Please note that, all the non-null columns should be included.

- **rowColumnValues**: An array of Arrays where the inner array
  represents a list of columnName/columnValue Tuples
  (which is equivalent to an array)
  and the Outer array represents an array of rows

**get**: get the data from the Oracle table

- **containerName**: Name of the table that was used
  in the API "createTable".

- **selectList**: Array of columns who values are being fetched.
  It can be null in which case all the columns are fetched.

- **filterColumns**: Array of tuples where each tuple
  is columnName/columnValue pair.
  These tuples are used to construct a where clause.
  At this point, we support only equality condition
  between columnName and columnValue (operator = )
  and only  "and" operator between the predicates.
  It can be null which results in a table scan.

- **callbackFunction**: The API invokes this call back function
  for every column value fetched by the get operation.
  Call back function has parameters:
  table_name, row_number, column_name, column_value


Example Output Adapter Configuration
------------------------------------

The following defines each field specified in the output adapter configuration.

Example:

::

  {
  "Name": "TestOut_1",
  "TypeString": "Output",
  "InputAdapterToVerify": "TestOut_In_1",
  "ClassName": "com.edifecs.saas.le.adapter.EdifecsOutputAdaptor$",
  "JarName": "le-adaptor-0.1.0.0-SNAPSHOT.jar",
  "DependencyJars": ["jopt-simple-3.2.jar",
    "kafka_2.10-0.8.1.1.jar",
    "metrics-core-2.2.0.jar",
    "zkclient-0.3.jar",
    "kamanjabase_2.10-1.0.jar"
  ],
  "AdapterSpecificCfg": "{"HostList": "localhost:9092",
    "TopicName": "testout_1" }"
  },

- **Name** – unique logical name given to this adapter by the developer.
- **TypeString** – either Input (used for the input adapter),
  Output (used for the output adapter),
  Status (used for the status adapter),
  or Validate (used for the validation adapter).
  The validation adapter is used once to validate
  and should point to the output adapter where the data is written.
- **InputAdapterToVerify** – points to the validation adapter.
  This is used once.
- **ClassName** – adapter class where the adapter is implemented.
  It should be a static class in Java and a singleton object in Scala.
- **JarName** – JAR where the adapter class is implemented.
  This is loaded before resolving the class name.
- **DependencyJars** – JARs required for the adapters
  and these are loaded before loading JarName.
- **AdapterSpecificCfg** – string passed to the adapter.
  It is the adapter’s responsibility to understand
  the given string and get whatever is required for it.

.. _smart-file-adapter:

Smart File Output Adapter
-------------------------

A smart file output adapter is a particular type of output adapter.

A smart file output adapter (also known as a smart file producer)
is an output adapter that can be used
to persist Kamanja results to HDFS or a local file system.
This adapter supports compression and can produce files
in gzip, bzip2, or xz compression format.
The results can be partitioned into different files/folders
using time partition data and partition keys.

The smart file output adapter is defined in ClusterConfig.json:

::

  {
  "Name": “HDFSProducer",
  "TypeString": "Output",
  "TenantId": "tenant1",
  "ClassName": "com.ligadata.OutputAdapters.SmartFileProducer$",
  "JarName": "KamanjaInternalDeps_2.10-1.5.0.jar",
  "DependencyJars": [],
  "AdapterSpecificCfg": {
      "Uri": "hdfs://nameservice/folder/to/save",
      "FileNamePrefix": "Data",
      "MessageSeparator": "\n",
      "Compression": "gz",
      "RolloverInterval": "60",
      "TimePartitionFormat": "${yyyy}/${MM}/${dd}",
      "PartitionBuckets": "10",
      "Kerberos": {
          "Principal": "user@domain.com",
          "Keytab": "/path/to/keytab/user.keytab"
      }
  }
  }

Attributes in AdapterSpecificCfg for the smart file producer
are explained below:

- **Uri** – the only mandatory attribute
  and specifies the folder to write files.
  Uri can specify an HDFS location (should start with hdfs://)
  or a local file system directory (should start with file://).
- **FileNamePrefix** – specifies a prefix for the generated filenames.
  If not provided, no prefix is used.
- **MessageSeparator** – if specified, is written after every message.
- **Compression** – specifies the compression codec
  to use when generating files.
  It can be gz, bzip2, or xz.
  If not specified, the data is not compressed.
- **RolloverInterval** – can be specified in minutes,
  if the files need to be rolled.
  The smart file producer creates a new file
  every RolloverInterval minutes.
- **TimePartitionFormat** – specifies the folders
  for time-partitioned data.
  It can be any string with SimpleDateFormat strings between ${…}.
  For example, a format string of ${yyyy}/${MM}/${dd}
  creates subfolders for year, month, and day such as ../2016/05/15/.. and a format string of year=${yyyy}/month=${MM}/day=${dd}
  will create Hive-friendly partition directories
  such as ../year=2016/month=05/day=15/...
- **PartitionBuckets** – can be used to distribute data
  into different files based on partition key.
- **Kerberos** – provides security credentials for HDFS.
  When specified, Principal and Keytab are required.


