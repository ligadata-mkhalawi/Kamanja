
.. _smart-input-config-ref:

Smart File Input Adapter
========================

The smart file adapter is a custom input adapter
that can be used to ingest streaming messages
into a Kafka :ref:`topic<topic-term>` automatically
rather than using :ref:`kvinit-command-ref` to ingest data periodically.
It watches some configured folders constantly;
whenever it finds a new file in any of these folders,
it starts reading from this file, and sends it to the Kamanja engine.

This adapter supports different file system architectures.
Currently supported types are:

- DAS/NAS files (for example, Linux file system files)
- SFTP
- HDFS

The adapter supports the following types of files:

- plain text
- gzip
- lzo
- bz2


Smart File Input Adapter definition
-----------------------------------
::

  {
    "Name": "file_input_1",
    "TypeString": "Input",
    "TenantId": "tenant1",
    "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
    "JarName": "KamanjaInternalDeps_2.11-1.6.2.jar",
    "DependencyJars": [
      "ExtDependencyLibs_2.11-1.6.2.jar",
      "ExtDependencyLibs2_2.11-1.6.2.jar"
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



.. _smart-input-adapter-ref:

Smart File Input Adapter Parameters
-----------------------------------

A smart input adapter parameter should be given a name
that includes the supported filesystem type
such as "HDFSAdapter" or "SftpAdapter_1".

The first few parameters are the standard ones
used for all adapters and described on the
:ref:`adapter-def-config-ref` page.
The Smart Input File functionality is implemented
with the following parameters set as shown:

::

  "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
  "JarName": "smartfileinputoutputadapters_2.10-1.0.jar",
  

In addition, the **AdapterSpecificCfg** parameter
has the following attributes
which must be populated when configuring the adapter:


- **Type** – type of file system to ingest.
  Valid values are HDFS, SFTP, DAS/NAS.
- **ConnectionConfig** – information necessary to connect to the file system:

  - **HostLists** – comma-separated list of (server:port)
    of the server hosting the data source;
    this is not required for DAS/NAS file systems.
  - **UserId**, **Password** – user name and password
    used to connect to the file system
    when Kerberos is not enabled.
  - **Principal**, **Keytab** – used instead of **UserId/Password**
    when Kerberos is enabled
  - **Passphrase**, **KeyFile** – required if the server
    uses hosting the data source uses public key authentication.
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

The following parameters must be set in the *ClusterConfig.json* file
when connecting to a Hadoop cluster:

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
    When this parameter is set, the **MessageConfig**
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
    to check the input directories
    for input directory scaling.
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

  - **rcDir** – directory to monitor.

    - **targetDir** – directory to move files to after processing.
      If no value is specified for this parameter,
      the value of the **TargetMoveDir** is used.
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


Usage
-----

To configure a smart file adapter,
add the definition to the "Adapters" section
of the :ref:`clusterconfig-config-ref` file
and set the parameters to values appropriate
for your installation.

You can specify the directories from which to read input data
using either the **Locations** or the **DetailedLocations** parameter.
The difference is how the **MessageSeparator**, **OrderBy**,
**TagDelimiter**, and **MsgTags** parameters are treated:

- If the **Location** parameter is used,
  these settings apply to all input directories
- If the **DetailedLocations** parameter is used,
  these configuration properties are set independently
  for each input location and apply only to that location.

These parameters can also be specified directly under
the **MonitoringConfig** parameter,
meaning they are public and are applied to any location
(the input directory) that does not explicitly set
another value for that parameter.



Examples
--------

SFTP input with public key authentication
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
	  "Name": "SftpAdapter_1",
	  "TypeString": "Input",
	  "TenantId": "tenant1",
	  "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
	  "JarName": "smartfileinputoutputadapters_2.10-1.0.jar",
	  "DependencyJars": [],
	  "AdapterSpecificCfg": {
		  "Type": "SFTP",
		  "ConnectionConfig": {
			  "HostLists": "sftp@c.com:22",
			  "UserId": "user",
			  "Passphrase": "",
			  "KeyFile": "/tmp/key.pem"
		  },
		  "MonitoringConfig": {
			  "Locations": "/data/input,/tmp/input",
			  "TargetMoveDir": "/data/processed",
			  "MaxTimeWait": "3000",
			  "WorkerBufferSize": "4",
			  "MessageSeparator": "10"
		  }
  	}
  }

Local (DAS) file system input
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
	  "Name": "DasAdapter_1",
	  "TypeString": "Input",
	  "TenantId": "tenant1",
	  "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
	  "JarName": "KamanjaInternalDeps_2.11-1.6.2.jar",
	  "DependencyJars": [
		  "ExtDependencyLibs_2.11-1.6.2.jar",
		  "ExtDependencyLibs2_2.11-1.6.2.jar"
	  ],
	  "AdapterSpecificCfg": {
		  "Type": "DAS/NAS",
		  "ConnectionConfig": {},
		  "MonitoringConfig": {
			  "Locations": "/data/input",
			  "TargetMoveDir": "/data/processed",
			  "MaxTimeWait": "10000",
			  "MessageSeparator": "10",
			  "WorkerBufferSize": "4",
			  "ConsumersCount": "3"
		  }
	  }
  }


HDFS input
~~~~~~~~~~

::

  {
  	  "Name": "HdfsAdapter_1",
	    "TypeString": "Input",
	  "TenantId": "tenant1",
	  "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
	  "JarName": "KamanjaInternalDeps_2.11-1.6.2.jar",
	  "DependencyJars": [
		  "ExtDependencyLibs_2.11-1.6.2.jar",
		  "ExtDependencyLibs2_2.11-1.6.2.jar"
	  ],
	  "AdapterSpecificCfg": {
		  "Type": "hdfs",
		  "ConnectionConfig": {
			  "HostLists": "node1:9000,node2:9000"
		  },
		  "MonitoringConfig": {
			  "Locations": "/user/data/input",
			  "TargetMoveDir": "/user/data/processed",
			  "MaxTimeWait": "10000",
			  "MessageSeparator": "10",
			  "WorkerBufferSize": "4",
			  "ConsumersCount": "2"
		  }
	  }
  }


Here is an example of the **ConnectionConfig** block
that is required when connecting to a Hadoop cluster:

::

  "ConnectionConfig": {
         "HostLists": "hdfs://myNameService",
         "Authentication":"kerberos",
         "principal": "ligadata@INTRANET.LIGADATA.COM",
         "keytab": "/home/kamanja/kamanja.keytab",
         "hadoopConfig":{
            "dfs.nameservices":"NameService",
            "dfs.ha.namenodes.NameService": "namenode1,namenode2",
            "dfs.namenode.rpc-address.NameService-namenode1":"node1:8020",
            "dfs.namenode.rpc-address.NameService-ns.namenode2": "node2:8020",
            "hadoop.rpc.protection":"privacy",
            "dfs.client.failover.proxy.provider.NameService":"org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider"
                        }
                  }



HDFS input, specifying DetailedLocation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  {
    "Name": "file_input_1",
    "TypeString": "Input",
    "TenantId": "tenant1",
    "ClassName": "com.ligadata.InputAdapters.SmartFileConsumer$",
    "JarName": "KamanjaInternalDeps_2.11-1.6.2.jar",
    "DependencyJars": [
      "ExtDependencyLibs_2.11-1.6.2.jar",
      "ExtDependencyLibs2_2.11-1.6.2.jar"
    ],
    "AdapterSpecificCfg": {
      "Type": "HDFS",
      "ConnectionConfig": {
          "HostLists": "hdfs://host1:port1,hdfs://host2:port2",
          "Authentication": "kerberos",
          "principal": "user@GROUP.LOCAL",
          "keytab": "/home/user/user.keytab",
          "hadoopConfig": {
              "hadoop.rpc.protection": "privacy",
              "dfs.nameservices": "host1",
              "dfs.ha.namenodes.host1-ns": "namenode1,namenode2"
          }
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


See also
--------

- :ref:`adapter-def-config-ref` gives details about adapter definitions
- :ref:`adapter-binding-config-ref` describes the structure
  used to link adapters to :ref:messages<messages-term>`
  and :ref:`serializers<serial-deserial-term>`

