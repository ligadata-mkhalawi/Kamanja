

.. _adapter-def-config-ref:

Adapter definition
==================

All :ref:`adapter<adapter-term>` objects
used in the cluster are defined in the
:ref:`ClusterConfig.json<clusterconfig-config-ref>` JSON file.
Each input, output, or storage adapter used in the cluster
has its own "Adapter" section,
identified by a unique "Name".

Application pipelines define 
:ref:`adapter message bindings<adapter-binding-config-ref>`
that define how to process messages in the pipeline.
Each adapter message binding defines the adapter it is using.

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

File structure -- Smart file adapters
-------------------------------------

::

  {
    "Name": "file_input_1",
    "TypeString": "Input",
    "TenantId": "tenant1",
    "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
    "JarName": "KamanjaInternalDeps_2.11-1.5.3.jar",
    "DependencyJars": [
      "ExtDependencyLibs_2.11-1.5.3.jar",
      "ExtDependencyLibs2_2.11-1.5.3.jar"
    ],
    "AdapterSpecificCfg": {
      "Type": "HDFS",
      "ConnectionConfig": {
          "HostLists": "hdfs://host1:port1,hdfs://host2:port2",
          "Authentication": "kerberos",
          "principal": "user@GROUP.LOCAL",
          "keytab": "/home/user/user.keytab",
          "Encrypted.Encoded.Password": "HlC3OVDz5gC+HbDnmN8BUJ41MO9+ofHIlvm0sgFmmG4hKw+xB5hvrHpJ9vMQKOVECwTephZB222OH/VqoldeaT47e2TGskhSTkWfYn1GMhiM5T93ldUyuwWjb5U1HvG20sZkZhMNxnad3QXtf+ERtvtlCpQJ/ViVjEddEfTjwkw=",
          "PrivateKeyFile": "/home/kamanja/programs/kamanja/config/private.key"
          "hadoopConfig":{
             "dfs.nameservices":"NameService",
             "dfs.ha.namenodes.NameService": "namenode1,namenode2",
             "dfs.namenode.rpc-address.NameService-namenode1":"node1:8020",
             "dfs.namenode.rpc-address.NameService-ns.namenode2": "node2:8020",
             "hadoop.rpc.protection":"privacy",
             "dfs.client.failover.proxy.provider.NameService":"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"
            },
      "MonitoringConfig": {
        "MaxTimeWait": "10000",
        "MessageSeparator":"10",
        "WorkerBufferSize":"4",
        "ConsumersCount":"4",
        "MonitoringThreadsCount" : "2",
        "TargetMoveDir" : "/data/processed",
        "DetailedLocations":[
           { 
             "srcDir": "/data/input/msg0",
             "targetDir": "/data/processed0",
             "MsgTags": ["Msg0"],
             "TagDelimiter":"$$",
             "OrderBy" : "$File_Name"
           },
           {
              "srcDir": "/data/input/msg1",
              "targetDir": "/data/processed1",
              "FileComponents": {
                  "Regex": "^([0-9]{8})_([0-9]+)\\.([a-z]+)$", 
                  "Components": ["date", "serial", "extension"],
                  "Paddings": {
                      "serial": ["left", 5, "0"]
                  }
              },
              "OrderBy":["serial", "$FILE_MOD_TIME"],
              "TagDelimiter": "$$",
              "MsgTags": ["Msg1", "$File_Name", "$Line_Number"],
              "MessageSeparator": "10"
          },
          { 
             "srcDir": "/data/input/msg2",
             "targetDir": "/data/processed2"
           },
           { 
             "srcDir": "/data/input/msg3"
           }
         ]
      }
    }
  } 
  
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



Parameters
----------

All adapter definitions include the following Core parameters:

- **Name** – name of the adapter.
  This is the name that :ref:`adapter bindings<adapter-binding-config-ref>`
  use to identify the adapter they are using.

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



.. _smart-input-adapter-ref:

Smart input adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A smart input adapter parameter should be given a name
that includes the supported filesystem type
such as "HDFSAdapter" or "SftpAdapter_1".
In addition, the **AdapterSpecificCfg** parameter
has the following attributes:


- **Type** – type of file system to ingest.
  Valid values are HDFS, SFTP, DAS/NAS.
- **ConnectionConfig** – information necessary to connect to the file system:

  - **HostLists** – comma-separated list of (server:port) to connect to. It is not required for DAS/NAS.
  - **UserId**, **Password** – user name and password
    used to connect to the file system;
    used when Kerberos is not enabled.
  - **Principal**, **Keytab** – used instead of UserId/Password
    when Kerberos is enabled
  - **Passphrase**, **KeyFile** – required if server uses
    public key authentication.
    In this case, the adapter uses **UserId** but ignores **Password**.
  - **Encrypted.Encoded.Password** - Password generated by
    :ref:`generatekeys-command-ref` to implement
    :ref:`encrypted and encoded passwords<password-encrypt-term>`.
    To implement this feature in your application,
    you must populate this parameter and the next one
    for the smart input adapter you are using.
  - **PrivateKeyFile** - private.key file to use for your application.
    You can use :ref:`generatekeys-command-ref` to create this file
    for testing.

- **hadoopConfig** - controls how the adapter connects to a Hadoop cluster:

  - **dfs.nameservices** -
  - **dfs.ha.namenodes.EXAMPLENAMESERVICE** -
  - **dfs.namenode.rpc-address** -
  - **hadoop.rpc.protection** -
  - **dfs.client.failover.proxy.provider** -


In addition, the following parameters are defined
for all smart input adapters:

- **DirCheckThreshold** – if greater than zero, listing watched folders stops
  when the count of the files waiting to be processed is above the threshold.
  This means that, if five files are waiting to be processed
  and the threshold is three, the monitor stops checking
  the input directories until the number of waiting files gets to three.
  This helps when processing the listing directory’s files
  is a relatively costly operation.

- **MonitoringConfig** – controls how the adapter monitors directories
  and reads messages from files:

  - **Locations** – A comma separated list of directories to monitor.
  - **TargetMoveDir** - The directory to which processed files are moved.
  - **MaxTimeWait** – maximum time, in milliseconds, for the adapter to wait
    each time it checks for new files; default value is 1000 milliseconds.
  - **WorkerBufferSize** – size, in MB, of the buffer
    used for internal storage.
    A file being processed is split into chunks of this size; default is 4 MB.
  - **ConsumersCount** – number of file consumers,
    which is the maximum number of concurrent files
    that can be processed; default value is 1.
  - **MessageSeparator** – character used for determine the end of a message.
    If this is an unprintable characters, an ASCII value is supplied.
    The default value is 10, which is the ASCII value for new line.
  - **MonitoringThreadsCount** – size of the thread pool
    to check the input directories.
  - **DetailedLocations** – describes directories to monitor
    with detailed attributes that differ from directory to directory.
    This is an array.
    The file may have multiple **DetailedLocations** blocks
    to define different behavior for different **srcDir** locations.
    These attributes can also exist directly under **MonitoringConfig**,
    in which case they are public and are applied to any location
    (any input directorsy) that has no value assigned.
    Each **DetailedLocation** block has the following structure
    (only **srcDir** is mandatory):

    - **srcDir** – directory to monitor.
    - **targetDir** – directory to move files to after processing.
    - **MsgTags** – in case the user wants the input adapter
      to send other information with the messages it reads.
      There are two types of tags – fixed and predefined.
      Fixed tags mean to add the string as is.
      Predefined tags mean to add the value of the attribute.
      Currently, the supported predefined tags are:
      $Dir_Name, $File_Name, $File_Full_Path, $File_Full_Path, and $Line_Number.
    - **TagDelimiter** – delimiter between tags.  For example:

      ::

        ("TagDelimiter" : "$$", "MsgTags" :["Msg1", "$File_Name", "$Line_Number"])

      and assuming the input adapter reads the message (1,hello,5)
      at line number (50) from the file (file1.txt),
      the final message sent by the input adapter looks like this:

      ::

        (Msg1$$file1.txt$$50$$1,hello,5).

  - **MessageSeparator** – same as (MessageSeparator) in the upper level
    (that is, MessageSeparator under MonitoringConfig directly).
    If no value is defined here,
    the value of (MessageSeparator) from the upper level is used.
  - **FileComponents** – the section used to define the file name format.
    It has the following sections:

    - **Regex** – regular expression describing the format.
    - **Components** – array of strings where each value is matching
      a part of the above regular expression
      and used as the name to that part.  For example: "Regex":

      ::

        "^([0-9]{8})_([0-9]+)\\.([a-z]+)$", "Components": ["date", "serial", "extension"].

      This means the filename should look like (20160101_123.txt).
      Also, it means that for such a file,
      the values of the components are
      (date=20160101, serial=123, extension=txt).
    - **Paddings** – used to add pads to any of the components
      defined in the attribute (Components).
      This is a map with component name as key.
      For example: "Paddings": { "serial": ["left", 5, "0"] }.
      This means that when comparing files (for ordering),
      the value of the component (serial) is padded
      from left by zeros until five digits.
      The whole (FileComponents) section is optional but, when provided,
      files that do not follow the provided regex are ignored.

- **OrderBy** – defines the order in which files are processed.
  This is an array so ordering by multiple attributes is supported.
  Component names (defined in Components) can be used.
  Also, predefined values can be used.
  Supported predefined values are
  ($File_Name, $File_Full_Path, $FILE_MOD_TIME).
  For example, "OrderBy":["serial", "$FILE_MOD_TIME"]
  means that files are ordered based on the serial part
  (which is extracted from the filename).
  By file modification time, when not provided,
  the value of the same attibute from the upper level is used.
  If that is also not provided,
  the default value is ($FILE_MOD_TIME),
  meaning files are ordered by modification time.



.. _smart-output-adapter-ref:

Smart output adapter parameters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A smart output adapter parameter should be given a name
that includes the supported filesystem type
such as "HDFSAdapter" or "SftpAdapter_1".
In addition, the **AdapterSpecificCfg** parameter
has the following attributes:


- **Uri** – specifies the folder to which files are written.
  This can specify an HDFS location (should start with hdfs://)
  or a local file system directory (should start with file://).
- **FileNamePrefix** – specifies a prefix for the generated filenames.
  If not specified, no prefix is used.
- **MessageSeparator** – if specified, is written after every message.
- **Compression** – specifies the compression codec to use
  when generating files. It can be gz, bzip2, or xz.
  If not specified, the data is not compressed.
- **RolloverInterval** – interview, in minutes, at which files can be rolled.
  The smart file producer creates a new file every RolloverInterval minutes,
  if the files need to be rolled.
- **TimePartitionFormat** – specifies the folders for time-partitioned data.
  It can be any string with SimpleDateFormat strings between ${…}.
  For example, a format string of ${yyyy}/${MM}/${dd} creates subfolders
  for year, month, and day such as ../2016/05/15/..
  and a format string of year=${yyyy}/month=${MM}/day=${dd}
  creates Hive-friendly partition directories
  such as ../year=2016/month=05/day=15/...
- **PartitionBuckets** – can be used to distribute data
  into different files based on partition key.
- **Kerberos** – provides security credentials for HDFS.
  When specified, Principal and Keytab are required.


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



