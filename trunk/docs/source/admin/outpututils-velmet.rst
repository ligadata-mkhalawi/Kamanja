

.. _outpututils-velmet:

Implementing Velocity Metrics for OutputUtils
=============================================

:ref:`velocity-metrics-term` is supported for the
hdfs and jdbc OutputUtils beginning with Release 1.6.2 of Kamanja.


JDBC OutputUtil
---------------

To implement Velocity Metrics for the JDBC OutputUtil:

- Modify the 
- Run the following command:

::

  export currentKamanjaVersion=1.6.2

  export KAMANJA_HOME=/home/flare/Binaries/VelocityMetricsIntegration/
      Kamanja-1.6.2_2.11

  export INSTALL_HOME=/home/flare/repos/VelocityMetricsIntegration/Kamanja/
      trunk/Utils/OutputUtils/target/scala-2.11

  export HADOOP_HOME=/data/hadoop

 

  java -Dlog4j.configurationFile=file:$KAMANJA_HOME/config/log4j2.xml
      -cp /etc/hadoop/conf:$HADOOP_HOME/lib/*:$HADOOP_HOME/.//*:
          $HADOOP_HOME/lib/*:$HADOOP_HOME/.//*:/usr/lib/hadoop-yarn/lib/*:
          /usr/lib/hadoop-yarn/.//*:$HADOOP_HOME/lib/*:$HADOOP_HOME/.//*:
          $INSTALL_HOME/KamanjaEndpointServices_2.11-1.6.2.jar:
          $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:
          $KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:
          $l/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar
      com.ligadata.adapters.KafkaAdapter config/copsjdbc.properties

Sample properties file for JDBC
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  # zookeeper parameters
  zookeeper.connect=localhost:2181
  #zookeeper.session.timeout.ms=400
  #zookeeper.sync.time.ms=200
   
  # kafka parameters
  kafka.group.id=copsoutput 
  kafka.topic=cops_output
  
  bootstrap.servers=localhost:9092
  
  # These parameters will be passed to kafka consumer config.  
  # offset storage can be kafka or zookeeper. default is zookeeper
  #kafka.offsets.storage=kafka
  # behavior if consumer offsets for above group.id are present. 
  # can be smallest meaning read from beginning or largest meaning read only new messages
  kafka.auto.offset.reset=earliest

  # number of parallel kafka consumers to run
  consumer.threads=1

  # implementation class to process messages
  adapter.message.processor=com.ligadata.adapters.jdbc.BufferedJDBCSink

  # SimpleDateFormat format string used to parse date in input message
  input.date.format=yyyy-MM-dd'T'HH:mm:ss.SSS

  # jdbc connection
  jdbc.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
  jdbc.url=jdbc:sqlserver://10.0.0.83:1433;databaseName=hworld;
  jdbc.user=sa
  jdbc.password=pass123
  #
  #jdbc.url=jdbc:sqlserver://ec2-54-160-245-36.compute-1.amazonaws.com:1433;databaseName=admin;
  #jdbc.user=admin
  #jdbc.password=pass123
  jdbc.insert.statement=INSERT INTO [SUBSCRIBER] ( [MSISDN], [ACC_NUM], [PLAN_NAME],
     [ACTIVE_DATE]) VALUES (  {$msisdn}, {$acc_num}, {$plan_name}, {$active_date})

  # parameters to control message batching
  # messages will be written every "count" messages or every "interval" seconds
  sync.messages.count=10
  sync.interval.seconds=10

  component.name=JDBCSink
  node.id.prefix=1
  adapter.weight=10

  velocitymetrics.kafka.topic=velocitymetrics
  velocitymetrics.category=outpututil
  velocitymetrics.component.name=hwjdbc
  velocitymetrics.config={"VelocityMetrics":[{"MetricsByMsgKeys":
     {"Keys":["msisdn"],"TimeIntervalInSecs":5}},{"MetricsByMsgFixedString":{"KeyString":["name"],"TimeIntervalInSecs":1}}]}
  velocitymetrics.nodeid=1


HDFS OutputUtil
---------------

To implement Velocity Metrics for the HDFS OutputUtil:

- Modify the .properties file as shown below.
- Run the following command:

::

  export currentKamanjaVersion=1.6.2

  export KAMANJA_HOME=/home/cloudera/Binaries/VelocityMetricsIntegration/
     Kamanja-1.6.2_2.11

  export INSTALL_HOME=/home/cloudera/repos/VelocityMetricsIntegration/
     Kamanja/trunk/Utils/OutputUtils/target/scala-2.11

  export HADOOP_HOME=/opt/cloudera/parcels/CDH

 

  java -Dlog4j.configurationFile=file:
     $KAMANJA_HOME/config/log4j2.xml -cp /etc/hadoop/conf:
     $HADOOP_HOME/lib/*:$HADOOP_HOME/.//*:
     $HADOOP_HOME/lib/*:$HADOOP_HOME/.//*:/usr/lib/hadoop-yarn/lib/*:
     /usr/lib/hadoop-yarn/.//*:$HADOOP_HOME/lib/*:
     $HADOOP_HOME/.//*:$INSTALL_HOME/KamanjaEndpointServices_2.11-1.6.2.jar:
     $KAMANJA_HOME/lib/system/ExtDependencyLibs2_2.11-${currentKamanjaVersion}.jar:
     $KAMANJA_HOME/lib/system/ExtDependencyLibs_2.11-${currentKamanjaVersion}.jar:
     $l/lib/system/KamanjaInternalDeps_2.11-${currentKamanjaVersion}.jar
         com.ligadata.adapters.KafkaAdapter config/hdfs.properties

 

Sample properties file for HDFS
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

::

  # zookeeper parameters
  zookeeper.connect=quickstart.cloudera:2181
  #zookeeper.session.timeout.ms=400
  #zookeeper.sync.time.ms=200
 
  # kafka parameters
  kafka.group.id=hdfssink
  kafka.topic=cops_output

  bootstrap.servers=quickstart.cloudera:9092

  # These parameters will be passed to kafka consumer config.  
  # offset storage can be kafka or zookeeper. default is zookeeper
  #kafka.offsets.storage=kafka
  # behavior if consumer offsets for above group.id are present. 
  # can be smallest meaning read from beginning or largest meaning read only new messages
  kafka.auto.offset.reset=earliest
  
  # number of parallel kafka consumers to run
  consumer.threads=1
  
  # implementation class to process messages
  adapter.message.processor=com.ligadata.adapters.hdfs.BufferedPartitionedAvroSink

  # uri to create files under
  #hdfs.uri=file:/tmp/data/subscriber
  hdfs.uri=hdfs://quickstart.cloudera:8020/home/cloudera/bofa/outputAdapter
  #hdfs.kerberos.keytabfile=
  #hdfs.kerberos.principal=
  #hdfs.resource.file=

  # prefix to name all files created under above uri
  file.prefix=AppLog

  # can be deflate, snappy, bzip2, xz
  # if not given, no compression is used
  file.compression=bzip2

  # append or new 
  file.mode=append

  # SimpleDateFormat format string used to parse date in input message
  input.date.format=yyyy-MM-dd

  # partition messages using the of attributes in the format string
  # attribute names should match schema definition.
  # optional SimpleDateFormat format string can be used after ":" for date attributes 
  file.partition.strategy=year=${timestamp:yyyy}/month=${timestamp:MM}/day=${timestamp:dd}

  # Avro schema file location
  schema.file=/home/cloudera/Binaries/VelocityMetricsIntegration/
      Kamanja-1.6.2_2.11/config/subscriber.avsc
  #schema.file=/opt/Kamanja-1.1.8/services/KafkaOutputAdapter/InstrumentationLog.avsc

  # parameters to control message batching
  # messages will be written every "count" messages or every "interval" seconds
  sync.messages.count=10
  sync.interval.seconds=10

  component.name=HDFSsink
  node.id.prefix=1
  adapter.weight=10

  velocitymetrics.kafka.topic=velocitymetrics
  velocitymetrics.category=outpututil
  velocitymetrics.component.name=hdfs
  velocitymetrics.config={"VelocityMetrics":[{"MetricsByMsgKeys":
     {"TimeIntervalInSecs":1}},{"MetricsByMsgFixedString":
     {"KeyString": ["name"],"TimeIntervalInSecs":1}}]}
  velocitymetrics.nodeid=1


