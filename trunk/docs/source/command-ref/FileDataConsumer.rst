
.. _filedataconsumer-command-ref:

FileDataConsumer
================

Populate Kafka message queues from files.

Syntax
------

::

  java -Xms2g -Xmx2g \
    -Dlog4j.configuration=file:$KAMANJA_HOME/config/fileProcessor_log4j.properties \
    -jar $KAMANJA_HOME/bin/FileDataConsumer-0.1.0
    $KAMANJA_HOME/config/fileProcessor.conf

Options and arguments
---------------------


Configuration file properties
-----------------------------

::

  dirToWatch=/tmp/watch
  moveToDir=/data/cops_processed
  metadataConfigFile=/tmp/Kamanja/config/MetadataAPIConfig.properties
  messageName=com.bofa.messages.inputmessage
  msgFormat=kv
  kafkaBroker=localhost:9092
  topic=TestIn_1
  messageSeparator=10
  fieldSeparator=,
  kvSeparator=-
  fileConsumers=1
  readyMessageMask=gzip
  workerdegree=3
  workerbuffersize=4
  maxAllowedMemory=500
  statusTopic=statusTest
  errorTopic=messageErrors
  throttle_ms=1000
  delayWarningThreshold
  fileQueueFullValue

To implement Velocity Metrics,
add the following properties to the configuration file:

::

  vm.rotationtimeinsecs=3000
  vm.emittimeinsecs=5
  vm.category=input
  vm.componentname=fileconsumer
  vm.kafka.topic=velocitymetrics
  velocitymetricsinfo={"VelocityMetrics":[{"MetricsByFileName":
     {"TimeIntervalInSecs":5}},{"MetricsByMsgType":{"ValidMsgTypes":
     ["com.ligadata.kamanja.samples.messages.msg1"],"TimeIntervalInSecs":
     5,"MetricsTime":{"MetricsTimeType":"LocalTime"}}},{"MetricsByMsgKeys":
     {"ValidMsgTypes":["com.ligadata.kamanja.samples.messages.msg1"],"Keys":
     ["id"],"TimeIntervalInSecs":5}},{"MetricsByMsgFixedString":
     {"KeyString":["name"],"TimeIntervalInSecs":5}}]}
  
Properties
----------

- **dirToWatch** - Directory to watch (required parameter).
  This directory is monitored for new incoming files.
- **moveToDir** - Directory where the processed files are moved
  after the utility is finished with them.
  If nothing is specified, then the file is renamed
  inside the *dirToWatch* directory by appending a d_COMPLETE to the file.
- **metadataConfigFile** - Full path of the
  :ref:`metadataapiconfig-config-ref` file.
  This is required because the utility must access metadata storage
  to retrieve MessageDef for the message being processed. (required parameter)
- **messageName** - Full name of a message definition (namespace.name)
  of a message being processed. (required parameter).
- **msgFormat** - Message format (required parameter).
  KV is the only format currently supported.
- **kafkaBroker** - Kafka broker to connect to (required parameter).
- **topic=TestIn_1** - Topic name where the messages are to be inserted
  (required parameter).
- **messageSeparator** - Character to be used to determine the end of a message.
- **fieldSeparator** - Character that indicates where fields in the message
  are separated. Defaults to x01.
- **kvSeparator** - Character that indicates where KV values in the message
  are separated. Defaults to x01.
- **fileConsumers** - Number of file consumers,
  which defines the number of concurrent files that can be processed.
  Default value is 1.
- **readyMessageMask** - The extension of the file to process.
  Default value is gzip.
- **workerdegree** - Number of internal parallel threads
  within each file consumer.
  The internal threads are used to parse and validate messages in the files.
  Default value is 1.
- **workerbuffersize** - Size of a buffer to be used for internal storage.
  A file being processed is split up into chunks of this size.
  Default value is 4MB.
- **maxAllowedMemory** - Amount of heap storage, in MB
  to be used to perform file processing operations. Default value is 512MB.
- **statusTopic** - Name of the Kafka queue
  where utility status messages are written.
  If not specified, no status messages are written.
- **errorTopic=messageErrors** - Name of the Kafka queue
  where messages that cannot be parsed are written.
  If not specified, no error messages are written.
- **throttle_ms** - Time, in milliseconds,
  to wait before retrying to acquire more memory
  if the utility detects that it is using too much memory.
  Default value is 250ms.
- **delayWarningThreshold** - Time, in milliseconds,
  to wait for a call to an eternal component
  (such as Zookeeper, Kafka, NAS, etc)
  before sending a WARNING message to the log.
  Default value is 5000ms.
- **fileQueueFullValue** - Soft queue limit for actively processed files.
  If the number of files exceeds this value,
  no new files are read until the total number of files drops below this number.  The number of files on the internal queue can be greater than this
  if a spike of input files exceeds the value
  and is processed in the same time interval.
  The default value is 300.

The Velocity Metrics properties are:

- **vm.rotationtimeinsecs** -
- **vm.emittimeinsecs** -
- **vm.category** -
- **vm.componentname** -
- **vm.kafka.topic=velocitymetrics** -
- **velocitymetricsinfo** -


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
FileDataConsumer-X.X.X fat JAR located in the */bin* directory.

See also
--------

:ref:`adapters-input-guide`


