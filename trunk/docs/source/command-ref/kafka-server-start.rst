

.. _kafka-server-start-command-ref:

kafka-server-start.sh
=====================

Initialize the :ref:`Kafka<kafka-term>` server.

Syntax
------

::

  $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties

Options and arguments
---------------------

Usage
-----

Do the following before running this command to start
the Kafka server:

- Run :ref:`SetPaths.sh<setpaths-command-ref>`
  to perform string replacement on items in the template files
  and create new configuration files.

- Run :ref:`zkServer.sh`<zkserver-command-ref>`
  to start :ref:`Zookeeper<zookeeper-term>`.

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

- :ref:`start-node-install-guide`


