

.. _command-ref-top:

=======================
Command Reference Pages
=======================


.. list-table::
   :class: ld-wrap-fixed-table
   :widths: 30 50
   :header-rows: 1

   * - Command
     - Description
   * - :ref:`clusterinstallerdriver-command-ref`
     - Install a fresh Kamanja release
       or upgrade from an earlier release.
   * - :ref:`containerutility-command-ref`
     - Select, delete, and truncate data
       from a :ref:`container<container-term>`.
   * - :ref:`createqueues-command-ref`
     - Create default :ref:`topics<topic-term>`
   * - :ref:`easyinstallkamanja-command-ref`
     - Build a Java or Scala model.
   * - :ref:`filedataconsumer-command-ref`
     - Populate Kafka message queues from files
   * - :ref:`generatekeys-command-ref`
     - Generate a sample key pair and encrypt a password
   * - :ref:`generatemessage-command-ref`
     - Generate a :ref:`message definition<message-def-config-ref>`
       from a file header or from :ref:`PMML<pmml-term>`.
   * - :ref:`jdbcdatacollector-command-ref`
     - Extract data from an external relational database
       into a Kamanja :ref:`container<container-term>`.
   * - :ref:`jsonchecker-command-ref`
     - Check the sanity of a JSON file.
   * - :ref:`jsonchecker-command-ref`
     - Ingest data into a Kafka :ref:`topic<topic-term>`
   * - :ref:`kafka-console-command-ref`
     - :ref:`View and monitor Kafka :ref:`topic<topic-term>`
       in the cluster.
   * - :ref:`kafka-server-start-command-ref`
     - Initialize the :ref:`Kafka<kafka-term>` server.
   * - :ref:`kafka-topics-command-ref`
     - Create and delete Kafka topics.
   * - :ref:`kamanja-command-ref`
     - Start/stop Kamanja engine, general operations for the cluster,
       and manage metadata.
   * - :ref:`setpaths-command-ref`
     - Perform string replacement on items in the template files
       and create new configuration files in the config folder.
   * - :ref:`simplekafkaproducer-command-ref`
     - 
   * - :ref:`simplepmltesttool-command-ref`
     - Test a generated PMML model.
   * - :ref:`startkamanjacluster-command-ref`
     - Start the Kamanja cluster
   * - :ref:`statuskamanjacluster-command-ref`
     - Report health of each node in the cluster.
   * - :ref:`watchqueue-command-ref`
     - Monitor default :ref:`topics<topic-term>`.
   * - :ref:`zkcli.sh-command-ref`
     - Verify that ZooKeeper is running.
   * - :ref:`zkserver-command-ref`
     - Start Zokeeper.
   * - :ref:`zookeeper-command-ref`
     - ZooKeeper shell.



.. toctree::
   :titlesonly:
   :maxdepth: 1

   command-ref/ContainerUtility
   command-ref/CreateQueues
   command-ref/ClusterInstallerDriver
   command-ref/easyInstallKamanja
   command-ref/FileDataConsumer
   command-ref/GenerateKeys
   command-ref/GenerateMessage
   command-ref/JdbcDataCollector
   command-ref/JsonChecker
   command-ref/kafka-console-consumer
   command-ref/kafka-server-start
   command-ref/kafka-topics
   command-ref/kamanja
   command-ref/KVInit
   command-ref/SetPaths
   command-ref/SimpleKafkaProducer
   command-ref/SimplePmlTestTool
   command-ref/StartKamanjaCluster
   command-ref/StatusKamanjaCluster
   command-ref/WatchQueue
   command-ref/zkCLI
   command-ref/zkServer
   command-ref/Zookeeper


