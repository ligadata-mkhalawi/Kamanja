
.. _kafka-term:

Kafka
-----

Kafka is a high-throughput, distributed messaging queue system
that supports partitions.
Krtmanja uses Kafka or to stream transactions
into and out of Kamanja in real time.

Logical partitions are not supported in Release 1.6.*
but are under development for future releases.

Kafka 1.6.x uses Kafka 0.10 by default;
this means that, when you run any **PushDataToKafka_*.sh** scripts,
they are run against Kafka 0.10 unless you use the
**-kafkaversion** option to specify the Kafka version in your command line;
for example:

::

  Bash PushDataToKafka_hello_world.sh -kafkaversion “[0.8|0.9]”

The version of Kafka running on your system is defined in the Adapters section
of the :ref:`ClusterConfig.json<clusterconfig-config-ref>` file.
Kamanja cannot properly connect and will die
if you attempt to push information with the wrong version of Kafka.

[Or maybe the version is defined by the ClusterConfig.json file
you run to add cluster configuration.
The config directory includes files like ClusterConfig_kafka_v10.json.
To start the cluster, run:

::

  Kamanja add cluster config <path-to-config-with-proper-kafka-version>

]

For more information:

- See :ref:`kafka-install` for information about installing Kafka
  before installing the Kamanja software.

For more information:

- :ref:`java-guide-kafka`

See also:

- `Kafka home page <https://kafka.apache.org/>`_


