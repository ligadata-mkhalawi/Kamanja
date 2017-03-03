
.. _smart-input-adapter-term:

Smart file input adapter
------------------------

A smart file adapter is a custom input :ref:`adapter<adapter-term>`
that can be used to get messages onto the Kamanja server
in order to process them.
It watches some configured folders constantly.
Whenever it finds a new file in any of these folders,
it starts reading from this file and sends the new data to the Kamanja engine.

The smart file input adapter currently supports
the following file system types:

- DAS/NAS files (for example Linux file system files)
- SFTP
- HDFS

The smart file input adapter currently supports
the following types of files:

. plain text
. gzip
. lzo
. bz2

To use an input adapter,
it should be configured in the
:ref:`adapter definition section<adapter-def-config-ref>`
of the :ref:`ClusterConfig.json<clusterconfig-config-ref>` file.
This configuration specifies the file system type to use,
how to connect to the file system, what folders to watch,
and some other information.

For more information:

- :ref:`smart-input-adapter-ref` gives details about the
  parameters and attributes that control a smart file input adapter.


