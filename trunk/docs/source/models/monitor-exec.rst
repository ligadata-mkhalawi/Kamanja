
.. _monitor-exec:

Monitoring model execution
==========================

Information about message and model execution appears
in standard Kamanja schemes that record various types of events.
These include:

- Kamanja uses Apache :ref:`ZooKeeper<zookeeper-term>`
  to monitor the health of a cluster and its nodes.
- To debug a model, put break points in your code,
  then start Kamanja in debug/verbose mode by running kamanja start -v .
- :ref:`log4j2-term` implements logging for the Kamanja engines
  as well as any model running in the cluster.
  You can configure log4j2 to customize the logging done for your models.
- Use :ref:`message-level tracking<message-tracking-admin>`
  to implement event-level tracking
  for any destination specified in the
  :ref:`ClusterConfig.json<clusterconfig-config-ref>` file.
- Implement :ref:`velocity-metrics-term` to collect statistics
  that can be used to understand the execution performance
  of message processing in your application.
- Kamanja does not currently provide tools
  for analyzing the execution speed of a model or an application,
  nor for identifying I/O bottlenecks in the application execution.
  You can implement some rough analysis
  by setting breakpoints in your code and analyzing the system logs
  to ensure that you do not have a disproportionate amount of activity
  on some nodes while other nodes are quiet.
- Kamanja does not currently provide facilities
  to limit the resources (number of CPU cores, memory)
  consumed by a single model or application.


