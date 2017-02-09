
.. _smart-output-config-ref:

Smart File Output Adapter
=========================

Adapter definition
------------------

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


.. _smart-output-adapter-ref:

Smart File Output Adapter Parameters
------------------------------------
A smart output adapter parameter has a name
that includes the supported filesystem type
such as "HDFSAdapter" or "SftpAdapter_1".

The first few parameters are the standard ones
used for all adapters and described on the
:ref:`adapter-def-config-ref` page.
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

See also
--------

- :ref:`adapter-def-config-ref` gives details about adapter definitions
- :ref:`adapter-binding-config-ref` describes the structure
  used to link adapters to :ref:messages<messages-term>`
  and :ref:`serializers<serial-deserial-term>`

