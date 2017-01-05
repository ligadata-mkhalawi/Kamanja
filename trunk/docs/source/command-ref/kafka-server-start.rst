

.. _kafka-server-start-command-ref:

kafka-server-start.sh
=====================

Syntax
------

::

  $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties

Options and arguments
---------------------

Usage
-----

Zookeeper must be running before you start Kafka.

Use the following command to run Kafka in the background
or to start another terminal from which to run the samples
with the following command:

::

  $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties >/dev/null &

Output
------

Expected output:

::

  [2015-12-03 14:32:05,498] INFO Verifying properties (kafka.utils.VerifiableProperties)
  [2015-12-03 14:32:05,532] INFO Property broker.id is overridden to 0 (kafka.utils.VerifiableProperties)
  ...
  [2015-12-03 14:32:06,099] INFO [Kafka Server 0], started (kafka.server.KafkaServer)
  [2015-12-03 14:32:06,145] INFO New leader is 0 (kafka.server.ZookeeperLeaderElector$LeaderChangeListener)


See also
--------


