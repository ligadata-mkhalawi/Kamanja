
.. _filedataconsumer-command-ref:

FileDataConsumer
================

Populate Kafka message queues from files

Syntax
------

::

  java -Xms2g -Xmx2g \
    -Dlog4j.configuration=file:$KAMANJA_HOME/config/fileProcessor_log4j.properties \
    -jar $KAMANJA_HOME/bin/FileDataConsumer-0.1.0
    $KAMANJA_HOME/config/fileProcessor.conf

Configuration file
------------------

::

  # Directory to watch (required parameter). This directory is monitored for new incoming files.
  dirToWatch=/tmp/watch
 
  # Directory where the processed files are moved after the utility is finished with them. If nothing is specified,
  # then the file is renamed inside the dirToWatch directory by appending a d_COMPLETE to the
  # file.
  moveToDir=/data/cops_processed
 
  # Path to the metadata configuration properties file. This is needed because the utility needs to access metadata
  # storage to retrieve MessageDef for the message being processed. (required parameter)
  metadataConfigFile=/tmp/Kamanja/config/MetadataAPIConfig.properties
 
  # Full name of a message definition (namespace.name) of a message being processed. (required parameter).
  messageName=com.bofa.messages.inputmessage
 
  # Message format (required parameter). KV is the only format supported now.
  msgFormat=kv
 
  # Kafka broker to connect to (required parameter).
  kafkaBroker=localhost:9092
 
  # Topic name where the messages are to be inserted (required parameter).
  topic=TestIn_1
 
  # Character to be used to determine the end of a message.
  messageSeparator=10
 
  # Character that indicates where fields in the message are separated. Defaults to x01.
  fieldSeparator=,
 
  # Character that indicates where KV values in the message are separated. Defaults to x01.
  kvSeparator=-
 
  # Number of file consumers - number of concurrent files that can be processed. Defaults to 1.
  fileConsumers=1
 
  # The extension of the file to process. Defaults to gzip.
  readyMessageMask=gzip
 
  # Number of internal parallel threads within each file consumer - the internal threads used to parse/validate messages
  # in the files. Defaults to 1.
  workerdegree=3
 
  # Size of a buffer to be used for internal storage. A file being processed is split up into chunks of this size.
  # Defaults to 4MB.
  workerbuffersize=4
 
  # MB of heap storage to be used to perform file processing operations. Defaults to 512MB.
  maxAllowedMemory=500
 
  # Name of the Kafka queue where utility status messages are written. If not specified, no status messages are
  # written.
  statusTopic=statusTest
 
  # Name of the Kafka queue where messages that unable to be parsed are written. If not specified, no error
  # messages are written.
  errorTopic=messageErrors
 
  # If the utility detects that it is using too much memory, wait for this many milliseconds before retrying
  # to acquire more memory. Defaults to 250ms.
  throttle_ms=1000
 
  # The utility reports when a call to an external component (such as Zookeeper, Kafka, NAS, etc) takes too long.
  # If any call takes longer then this threshold, then a WARNING message is sent to the log. This value is in
  # milliseconds and defaults to 5000ms (5 sec) (new in v1.5.2).
  delayWarningThreshold
 
  # The utility has a soft queue limit for actively processed files. If the number of files exceeds this value,
  # no new files are read until the total number of files drops below this number. The number of files on the
  # internal queue can be greater than this if a spike of input files exceeds the value and
  # is processed in the same time interval. The default value is 300 (new in v1.5.2).
  fileQueueFullValue


Options and arguments
---------------------

Usage
-----

This tool works as follows:

- An administrator sets up a directory
  that is accessible to the tool
  (the tool must have read/write permissions for this new directory).
  The file data consumer monitors this directory
  and detects when a new file appears.
  If that file provides specific criteria
  (as specified in the configuration file),
  the utility tool picks up that file
  and processes the messages contained within.
  The messages in the file are inserted into
  a specified Kafka queue that is processed by the Kamanja engine.

Files
-----

This tool is included in a separate stand-alone
FileDataConsumer-X.X.X fat JAR located in the /bin directory.

See also
--------

:ref:`adapters-input`


