

.. _watchqueue-command-ref:

WatchInputQueue.sh, WatchOutputQueue, WatchStatusQueue
======================================================

These functions monitor the default :ref:`Kafka<kafka-term>`, 
:ref:`topics<topic-term>` that are created
when Kamanja is first initialized;
they assume that :ref:`ZooKeeper<zookeeper-term>`
is located on localhost:2181 and
the **kafka** server is localhost:9092.

- **WatchInputQueue.sh** watches the default 'testin_1' topic
- **WatchOutputQueue.sh** watches the default 'testout_1' topic
- **WatchStatusQueue.sh** watches the default 'teststatus_1' topic

Usage
-----

If the :ref:`SetPaths.sh<setpaths-command-ref>`
does not set the $KAFKA_HOME variable,
**WatchOutputQueue.sh** and **WatchStatusQueue.sh**
do not run correctly.

See also
--------

- :ref:`kafka-topics-command-ref`


