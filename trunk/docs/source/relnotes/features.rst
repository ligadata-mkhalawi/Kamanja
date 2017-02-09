
New features in Kamanja 1.6.2
=============================

Kamanja 1.6.2 includes a number of bug fixes
plus the following new features:

- Velocity metrics, which can be configured into your application
  to collect statistics about :ref:`message<messages-term>` processing.
  See :ref:`velocity-metrics-term` for more information.
- ElasticSearch adapter
- <Parquet updates>

Kamanja 1.6.1 was a limited release that includes
the following new features,
included in 1.6.2 for the first time in a public release:

- :ref:`Consolidated logging<logging-consolidated>` -
  creates a centralized queue for all errors and warnings
  from all components and all nodes in the cluster.
  These are implemented as a Kafka topic that the a log server can read.
- :ref:`Encrypted and encoded passwords<password-encrypt-term>` – 
  Kamanja now supports encrypted and encoded passwords
  as well as plain text passwords;
  only the RSA algorithm is supported at this time.
  In Release 1.6.2, you can use encrypted passwords
  with MetadataAPIServices or SmartFileAdapter.
  You can also add this functionality to any
  :ref:`adapters<adapter-term>` you create.
- :ref:`Failover support<failover-nodes-term>` for nodes
  in a cluster running the JDBC, HDFS, or FileDataConsumer services.

Click on the embedded links above
for more details about using each of these new features.


