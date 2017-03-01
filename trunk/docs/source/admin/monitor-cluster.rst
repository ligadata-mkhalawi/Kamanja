
.. _monitor-cluster-admin:

Monitor the Kamanja cluster
===========================

Tools are provided for monitoring the Kamanja cluster:

.. list-table::
   ::widths:: 25 75
   :header-rows: 1

   * - Command
     - Purpose

   * - :ref:`statuskamanjacluster-command-ref`
     - Report health of each node in the cluster
   * - :ref:`GetHealthCheck<gethealthcheck-admin>`
     - 
   * - :ref:`zookeeper-query-admin`
     - 
   * - :ref:`message-tracking-admin`
     - view message-level events such as which message was executed,
       when it was executed, and which models were executed.
       This includes information about model events
       and the exception message Kamanja produces
       when it encounters an error during execution.
   * - :ref:`RecentLogErrorsFromKamanjaCluster.sh<recentlogerrorsfromkamanjacluster-admin>`,
       :ref:`LogErrorsFromKamanjaCluster.sh<logerrorsfromkamanjacluster-admin>`
     - Find ERROR messages in system logs

.. _gethealthcheck-admin:

Using GetHealthCheck
--------------------

Several getHealthCheck methods are provided for monitoring Kamanja.
The MetadataAPI trait defines the following methods:

getHealthCheck
~~~~~~~~~~~~~~

Each node (Kamanja engine and Kamanja metadata)
externalizes some statistic information to ZooKeeper Znodes
on a heartbeat interval (currently set to every 5 seconds).

If an empty array is provided to getHealthCheck,
the heartbeat of all the nodes can be retrieved.

The MetadataAPI trait defines a method:

::

  getHealthCheck

This method does not take any parameters and returns an APIResult.
The resulting data property of this APIResult JSON
is an array of all the Kamanja engines and Kamanja metadata managers
that are known to Kamanja.
Get this API result back via a POST REST call to the following URL:

::

  https://hostname:port/api/heartbeat

- The BODY of the POST should contain a JSON array of node names.
- If the array is empty, then all the known nodes are returned.
- An empty body is an ERROR.

For example:

::

  #POST request to https://localhost:8081/api/heartbeat

The BODY must return all known information.

Here is an example of the APIResult that is returned:

::

  {
  "APIResults": {
  "Status Code": 0,
  "Function Name": "GetHeartbeat",
  "Result Description": “Heartbeatqueryexecutedsuccessfully ",
  "Result Data": [{
  "Name": "Node3",
  "Version": "1.3.0.0",
  "UniqueId": 19,
  "LastSeen": "2015-08-0418: 26: 12",
  "StartTime": "2015-08-0418: 24: 42",
  "Components": []
  }, {
  "Name": "Node5",
  "Version": "1.3.0.0",
  "UniqueId": 31,
  "LastSeen": "2015-08-0418: 24: 17",
  "StartTime": "2015-08-0418: 21: 47",
  "Components": []
  }]
  }
  }

 

getHealthCheckNodesOnly
~~~~~~~~~~~~~~~~~~~~~~~

This method takes a list of node IDs as a parameter
and returns an APIResult.
The resulting data property of this APIResult JSON
is a subset of the monitoring information,
showing only data related directly to nodes,
excluding the components under the nodes.

Get this API result back via a POST REST call to the following URL:

::

  https://hostname:port/api/heartbeat/nodesonly

- The BODY of the POST should contain a JSON array of node names.
- If the array is empty, then all the known nodes are returned.
- An empty body is an ERROR.

Here is an example of the result:
 
::

  {
  "APIResults": {
  "Status Code": 0,
  "Function Name": "GetHeartbeat",
  "Result Description": “Heartbeatqueryexecutedsuccessfully ",
  "Result Data": [{
  "Name": "Node1",
  "LastSeen": "2015-08-0418: 26: 12",
  "UniqueId": 19,
  "Version": "1.3.0.0",
  "StartTime": "2015-08-0418: 24: 42"
  }, {
  "Name": "Node2",
  "LastSeen": "2016-03-0418: 26: 12",
  "UniqueId": 20,
  "Version": "1.3.0.0",
  "StartTime": "2016-03-0418: 24: 42"
  }]
  }
  }

 
getHealthCheckComponentNames
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This method takes a list of node IDs as parameters
and returns an APIResult.
The result data property of this APIResult JSON
is a subset of the monitoring information
showing data related directly to nodes,
while showing only name and type for components under the nodes.

Get this API result back via a POST REST call to the following URL:

::

  https://hostname:port/api/heartbeat/componentnames

- The BODY of the POST should contain a JSON array of node names.
- If the array is empty, then all the known nodes are returned.
- An empty body is an ERROR.

Here is an example of the result:
 
::

  [{
  "Name": "Node1",
  "LastSeen": "2015-08-0418: 26: 12",
  "UniqueId": 19,
  "Version": "1.3.0.0",
  "StartTime": "2015-08-0418: 24: 42",
  "Components": [{
  "Name": "testin_1",
  "Type": "Input"
  }]
  }, {
  "Name": "Node2",
  "LastSeen": "2016-03-0418: 26: 12",
  "UniqueId": 20,
  "Version": "1.3.0.0",
  "StartTime": "2016-03-0418: 24: 42",
  "Components": [{
  "Name": "testout_1",
  "Type": "Output"
  }, {
  "Name": "testin_1",
  "Type": "Input"
  }]
  }]

 
getHealthCheckComponentDetailsByNames
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This method takes a list of component names as parameters
and returns an APIResult.
The result data property of this APIResult JSON
is similar to the result of the getHealthCheck method
except it shows data for only components
corresponding to the names passed as parameters.

Get this API result back via a POST REST call to the following URL:

::

  https://hostname:port/api/heartbeat/specificcomponents

- The BODY of the POST should contain a JSON array of node names.
- An empty body is an ERROR.

Here is an example of the result
as a response to a request with parameter ["testout_1"]:

::

  [{
  "Name": "Node1",
  "Components": [],
  "LastSeen": "2015-08-0418: 26: 12",
  "UniqueId": 19,
  "Version": "1.3.0.0",
  "StartTime": "2015-08-0418: 24: 42"
  }, {
  "Name": "Node2",
  "Components": [{
  "Name": "testout_1",
  "LastSeen": "2015-08-0418: 26: 12",
  "Description": "kafka output",
  "StartTime": "2015-08-0418: 24: 42",
  "Type": "Output",
  "Metrics": []
  }],
  "LastSeen": "2016-03-0418: 26: 12",
  "UniqueId": 20,
  "Version": "1.3.0.0",
  "StartTime": "2016-03-0418: 24: 42"
  }]


.. _zookeeper-query-admin:

Querying ZooKeeper for general statistics
-----------------------------------------

Another way to monitor Kamanja is by directly querying ZooKeeper.
See `ZooKeeper Getting Started Guide
<http://zookeeper.apache.org/doc/r3.1.2/zookeeperStarted.html>`_
for more information

The NodeID is specified in the metadata configuration.
It must be a unique ID.

Metadata status ZNodes:

::

  <znodeBase>/monitor/metadata/<NodeId>

Kamanja manager (also known as the engine) status Znodes:

::

  <znodeBase>/monitor/engine/<NodeId>

Each engine/metadata Znode data structure:

::

  { "Name": "", "UniqueId": "", "Version": "", "LastSeen": "", "StartTime": "", "Components": []}

The Components array is made up of:

::

  { "Type": "", "Name": "", "Description": "", "LastSeen": "", "StartTime": "", "Metrics":[]}

Each component is responsible for collecting and externalizing data
(the MONITORABLE trait in the com.ligadata.heartbeat package enforces it).

::

  def getComponentStatusAndMetrics: MonitorComponentInfo
  case class MonitorComponentInfo(typ: String, name: String, description: String, startTime: String, lastSeen: String, metricsJsonString: String)

In other words, if writing an adapter,
the user is responsible for defining the metricsJsonString
and outputting the metrics.

The heartbeat interval is set to 5 seconds.
These are the values that are externalized for Kamanja-implemented code.
(More numbers will be given in the future).

Engine:

- **Name** - name as shown in the NODE_ID of the relevant configuration file.
- **Version** - version of this engine (this is hard-coded for now).
- **UniqueId** - an ever-increasing number.
  Can be used for debugging.
  These are increasing so the order of externalization can be determined.
- **Metrics** - Java memory statistics such as UsedMemory,
  FreeMemory, TotalMemory, and MaxMemory (new in v1.5)See below:

  ::

    {
     "Name": "1",
     "Version": "1.5.0.0",
     "UniqueId": 5,
     "Metrics": "{"UsedMemory":"98 MB","FreeMemory":"127 MB","TotalMemory":"225 MB","MaxMemory":"2585 MB"}",",
     ...
    }

- LastSeen - heartbeat for the engine itself (updated on each heartbeat).
- StartTime - when the engine was last started.
- Components - array of all the input/output/storage components.


Metadata Service (Web Service):

- **Name** - name as shown in the NODE_ID of the relevant configuration file.
- **Version** - version of this engine (for now we are just hard coding this).
- **UniqueId** - an ever-increasing number. Can be used for debugging. These are increasing so the order of externalization can be determined.
- **LastSeen** - heartbeat for the engine itself (updated on each heartbeat).
- **StartTime** - when the engine was last started.
- **Components** - always an empty array for now.

The Components field in the engine has an array
of all the input/output/storage components
that are registered in the cluster configuration file for this engine.

They are defined as:

Input Adapter:

- **Type** - tells whether the engine is Kamanja.
- **Name** - name as defined in the cluster configuration file.
- **Description** - provided by the author of the adapter implementation to give any relevant information a user may want.
- **LastSeen** - each component maintains its own heartbeat!
- **StartTime** - when the component was instantiated.
- **Metrics**

  -  **Exception Summary**
  -  **Last_Failure**

     -  **Last_Recovery** - for each partition,
        the last time this adapter detected a failure,
        and the last time this adapter recovered.
        There may not be a Last_Recovery in the input adapter field.
        When Kafka is killed, retries are scaled back to 60 seconds,
        so successful retry is not marked until waking up after the sleep.
        That value is not populated for a while.

   - **Partition Counts** - number of individual messages processed
     for each partition. This could have old inactive partitions.
     Any messages that are not valid are also counted here.
   - **Partition Depths** - on each heartbeat,
     the Kafka input adapter asks the last offset in each partition
     and subtracts the current offset.
     The maximum value is kept between the newly computed one
     and the existing one.
     This only happens on each heartbeat in v1.3.
     It can give an idea if there are some large numbers here.


Output Adapter:

- **Type** - tells whether the engine is Kamanja.
- **Name** - name as defined in the cluster configuration file.
- **Description** - a meaningful description of the adapter implementation
  to give any relevant information a user may want.
- **LastSeen** - each component maintains its own heartbeat!
- **StartTime** - when the component has been instantiated.
- **Metrics**

  - ** Last_Failure**

       - ** Messages Sent** - individual messages sent to the output topic.
       - ** Send Call Count** - number of calls to the Kafka producer
         (multiple messages can be externalized per call).


Storage Adapter:

- **Type** - Kamanja interface to the storage.
- **Name** - name.
- **Description** - version.
- **LastSeen** - heartbeat.
- **StartTime** - time started.
- **Metrics** - for now it just READS from the datastore and WRITES to the datastore.

Here is an example to see whether ZooKeeper can be queried.

Step 1: Start Zookeeper and Kafka.
Step 2: Add the messages to the metadata.
Step 3: Create the queues.
Step 4: Run the InitKV scripts.
Step 5: Start the engine.
Step 6: Push the messages to the queue.

Run the Zookeeper shell to check the metrics
(run the following two commands to check the metrics),
1 in the second command is the NODEID.
Check the NODEID in ClusterCfgMetadataAPIConfig.properties
and make sure it is 1.

::

    bash $KAFKA_HOME/bin/zookeeper-shell.sh localhost:2181
    get /kamanja/monitor/engine/1

Expected Result

::

  {
   "Name": "1",
   "Version": "1.3.0.0",
   "UniqueId": 31,
   "LastSeen": "2016-01-20 11:18:58",
   "StartTime": "2016-01-20 11:16:15",
   "Components": [{
   "Type": "Input_Adapter",
   "Name": "testin_1",
   "Description": "Kafka 8.2.2 Client",
   "LastSeen": "2016-01-20 11:18:12",
   "StartTime": "2016-01-20 11:16:19",
   "Metrics": "{"
   Exception Summary ":{"
   2 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   5 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   7 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   1 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   4 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   6 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   0 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   3 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "}},"
   Partition Counts ":{"
   2 ":18,"
   5 ":18,"
   7 ":0,"
   1 ":0,"
   4 ":0,"
   6 ":0,"
   0 ":0,"
   3 ":0},"
   Partition Depths ":{"
   2 ":0,"
   5 ":0,"
   7 ":0,"
   1 ":0,"
   4 ":0,"
   6 ":0,"
   0 ":0,"
   3 ":0}}"
   }, {
   "Type": "Output_Adapter",
   "Name": "testout_1",
   "Description": "Kafka 8.1.1 Client",
   "LastSeen": "2016-01-20 11:18:57",
   "StartTime": "2016-01-20 11:16:06",
   "Metrics": "{"
   Last_Failure ":"
   n / a ","
   Messages Sent ":15,"
   Last_Recovery ":"
   n / a ","
   Send Call Count ":15}"
   }, {
   "Type": "Output_Adapter",
   "Name": "teststatus_1",
   "Description": "Kafka 8.1.1 Client",
   "LastSeen": "2016-01-20 11:18:55",
   "StartTime": "2016-01-20 11:16:06",
   "Metrics": "{"
   Last_Failure ":"
   n / a ","
   Messages Sent ":163,"
   Last_Recovery ":"
   n / a ","
   Send Call Count ":163}"
   }, {
   "Type": "Output_Adapter",
   "Name": "testfailedevents_1",
   "Description": "Kafka 8.1.1 Client",
   "LastSeen": "n/a",
   "StartTime": "2016-01-20 11:16:06",
   "Metrics": "{"
   Last_Failure ":"
   n / a ","
   Messages Sent ":0,"
   Last_Recovery ":"
   n / a ","
   Send Call Count ":0}"
   }, {
   "Type": "Input_Adapter",
   "Name": "testout_in_1",
   "Description": "Kafka 8.2.2 Client",
   "LastSeen": "2016-01-20 11:16:17",
   "StartTime": "2016-01-20 11:18:58",
   "Metrics": "{"
   Exception Summary ":{"
   2 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   5 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   7 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   1 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   4 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   6 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   0 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "},"
   3 ":{"
   Last_Failure ":"
   n / a ","
   Last_Recovery ":"
   n / a "}},"
   Partition Counts ":{"
   2 ":0,"

   5 ":0,"
   7 ":0,"
   1 ":0,"
   4 ":0,"
   6 ":0,"
   0 ":0,"
   3 ":0},"
   Partition Depths ":{"
   2 ":0,"
   5 ":0,"
   7 ":0,"
   1 ":0,"
   4 ":0,"
   6 ":0,"
   0 ":0,"
   3 ":0}}"
   }, {
   "Type": "STORAGE_ADAPTER",
   "Name": "SimpleEnvContext",
   "Description": "v1.3",
   "LastSeen": "2016-01-20 11:18:54",
   "StartTime": "2016-01-20 11:16:03",
   "Metrics": "{"
   READS ":252,"
   WRITES ":38}"
   }]
  }


.. _message-tracking-admin:

Message-level tracking
---------------------- 

Event-level information can be traced to one of the destinations
that is specified in the cluster configuration file.
The information is in JSON, kBinary, or CSV format.
It describes message-level events such as which message was executed,
when it was executed, and which models were executed.

Kamanja has a :ref:`kamanjamessageevent-msg-ref` internal message
that is created when a message comes into the Kamanja engine.
This includes these other messages:

- :ref:`kamanjamodelevent-msg-ref` tracks each model event
- :ref:`kamanjaexceptionevent-msg-ref` provides information
  about any error Kamanja encounters while processing the message
- :ref:`kamanjaexcecutionfailureevent-msg-ref` provides information
  about any errors encountered while trying to process a message.


.. _status-message-admin:

Kamanja status messages
-----------------------

Status messages are actually output adapters. For example:

::

  Status Messages

  {
   "Name": "TestStatus_1",
   "TypeString": "Output",
   "TenantId": "System",
   "ClassName": "com.ligadata.OutputAdapters.KafkaProducer$",
   "JarName": "kafkasimpleinputoutputadapters_2.10-1.0.jar",
   "DependencyJars": [
   "jopt-simple-3.2.jar",
   "kafka_2.10-0.8.2.2.jar",
   "kafka-clients-0.8.2.2.jar",
   "metrics-core-2.2.0.jar",
   "zkclient-0.3.jar",
   "kamanjabase_2.10-1.0.jar",
   "kvbase_2.10-0.1.0.jar"
   ],
   "AdapterSpecificCfg": {
   "HostList": "localhost:9092",
   "TopicName": "teststatus_1"
   }
  }


.. monitor-logs-admin:

Monitor Kamanja logs
--------------------

System logs are found on each node in a given cluster.
There are two log scraper tools.
Both look for ERROR messages in the logs on a given cluster.

.. _logerrorsfromkamanjacluster-admin:

LogErrorsFromKamanjaCluster.sh
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


LogErrorsFromKamanjaCluster.sh searches an entire log.
The syntax is:

::

    LogErrorsFromKamanjaCluster.sh
    --ClusterId < cluster name identifer >
    --MetadataAPIConfig < metadataAPICfgPath >
    --KamanjaLogPath < Kamanja system log path >
   [--ErrLogPath < where errors are collected > ]


NOTES: Logs for the cluster specified by the cluster identifier parameter
found in the metadata api configuration.
The default error log path is "/tmp/errorLog.log"
errors collected in this file.


To roll logs every hour, use this script.
The error log path receives the error lines found in the log.
Because the system log can be moved about with log4j configuration options,
the script requires the location of the Kamanja logs.
As written, only the current log is searched.
When scheduling a job that runs every five minutes,
the script nominally runs 12 times before log rollover.
The errors are repeatedly emitted for each of the runs during the hour.
However, this is satisfactory behavior
for simple console dashboard applications.
Note that errors from all nodes
are logged to the error log on the administration machine
that has issued the script.
The output currently looks similar to this:

::

    Node 1 (Errors detected at 2015-04-17 21:16:47) :
       file /tmp/drdigital/logs/testlog.log
       not found No ERRORs found for this period
    Node 2 (Errors detected at 2015-04-17    21:16:47) :
       2015-04-17 21:12:44,467 - com.ligadata.MetadataAPI.MetadataAPIImpl$    -
       ERROR - Closing datastore failed 2015-04-17 23:22:41,484 -
       com.ligadata.MetadataAPI.MetadataAPIImpl$    -
       ERROR - metdatastore is corrupt 2015-04-17 24:02:14,493 -
       com.ligadata.MetadataAPI.MetadataAPIImpl$    -
       ERROR - transStore died 2015-04-17 24:12:34,500 -
       com.ligadata.MetadataAPI.MetadataAPIImpl$    -
       ERROR - jarStore has no beans 2015-04-17 24:22:54,508 -
       com.ligadata.MetadataAPI.MetadataAPIImpl$    -
       ERROR - configStore hammered


In this example, there is no log found for Node 1.
Node 2 has logs for five different errors.

.. _recentlogerrorsfromkamanjacluster-admin:

RecentLogErrorsFromKamanjaCluster.sh
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The RecentLogErrorsFromKamanjaCluster.sh script
produces the same sort of output as the other.
It, however, is designed not to read the entire log.
Instead the script invocation can be configured
to only examine log records written to the log in the last InLast units,

Use this script instead of LogErrorsFromKamanjaCluster
in the following cases:

- The Kamanja clusters are heavily used with many transactions
  both in terms of metadata and model processing traffic.
- The high volume dictates more frequent log queries
  for the administration screen updates.
- The logrolling is dictated by logs reaching
  a substantial size before rolling;
  this would either make log scanning prohibitively expensive
  or cause too much output to be provided to the admin screen
  to be manageable to monitor (if not both).

The syntax is

::

    RecentLogErrorsFromKamanjaCluster.sh--ClusterId <cluster-name-identifer>
    --MetadataAPIConfig < metadataAPICfgPath >
    --InLast < unit count >
    --KamanjaLogPath < Kamanja system log path >
    [--ErrLogPath <where-errors-are-collected>]
    [--Unit < time unit...any of {
         minute,
         second,
         hour,
         day
         } > ]


Start the cluster specified by the cluster identifier parameter.
Use the metadata api configuration to locate the appropriate
metadata store.Default time unit is "minute".
Default error log path is "/tmp/errorLog.log"..errors
collected in this file




