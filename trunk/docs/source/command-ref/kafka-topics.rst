

.. _kafka-topics-command-ref:

kafka-topics.sh
===============

Create and delete :ref:`Kafka<kafka-term>` :ref:`topics<topic-term>`.

Syntax
------

::

  kafka-topics.sh —zookeeper server:port —topic <topicName>
     —partitions <#Partitions> —replication-factor <integer>

Options and arguments
---------------------

- **zookeeper** - The server name/IP address and port where
  :ref:`ZooKeeper<zookeeper-term>` for the cluster is running

- **topic** - Specify the name of the topic to be created or deleted

- **partitions** -- Specify the number of :ref:`partitions<partition-term>`
  to be allocated for this topic.

- **replication-factor** - 

Usage
-----

See also
--------

- :ref:`watchqueue-command-ref`

