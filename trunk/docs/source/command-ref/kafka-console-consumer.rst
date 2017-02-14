

.. _kafka-console-command-ref:

kafka-console-consumer.sh
=========================

Read data from a specific Kafka :ref:`topic<topic-term>` in the cluster.
This is a Kafka command that has been ported to Kamanja.

Syntax
------

::

  kafka-console-consumer.sh [--blacklist <blacklist>]
   --bootstrap-server <server-to-connect-to> --consumer.config <config file>
   [--csv-reporter-enabled] [--delete-consumer-offsets] [--enable-systest-events]
   [--formatter <class>] [--from-beginning]
   [--key-deserializer <deserializer-for-key>
   [--max-messages <Integer: num_messages>]
   [--metrics-dir <metrics directory>] [--new-consumer] --property <prop>
   [--skip-message-on-error]
   [--timeout-ms <Integer: timeout_ms>]
   --topic <topic>
   --value-deserializer <deserializer-for-values>
   [--whitelist <whitelist>]
   --zookeeper <server:port>


Options and arguments
---------------------

- **blacklist** - Blacklist of topics to exclude from consumption.
- **bootstrap-server** - Server to which to connect
- **consumer.config** - Consumer configuration properties file.
- **csv-reporter-enabled** - If set, the CSV metrics reporter is enabled.
- **delete-consumer-offsets** - If set, the ZooKeeper consumer path
  is deleted during start-up.
- **enable-systest-events** - If set, log consumed lifecycle events
  as well as consumed messages.
  This is used for testing.
- **formatter** - Classname to use to format Kafka messages for delay.
  Default value id *kafka.tools.DefaultMessageFormatter*
- **from-beginning** - Start with the earliest message present in the log
  rather than the latest messsage;
  this works only if the consumer does not already have
  an established offset from which to consume.
- **key-deserializer** - Deserializer for key.
- **max-messages** - Maximum number of messages to consume before exiting.
  If not set, consumption is continual.
- **metrics-dir** - Directory to which to output the CSV metrics;
  this option requires that the **csv-reporter-enabled** option is set.
- **new-consumer** - Use the new consumer implementation.
- **property <prop>** - Properties to initialize the message formatter.
- **skip-message-on-error** - Skip an error when processing a message
  and continue processing rather than halt when an error occurs.
- **timeout-ms <Integer: timeout_ms>** - Timeout value, in microseconds.
  If no message is available for consuption for this interval,
  the command exits.
- **topic <topic>** - Topic ID to consume.
- **value-deserializer** - Deserializer to use for values.
- **whitelist <whitelist>** - Whitelist of topics to include for consumption.
- **zookeeper** - Connection string for :ref:`ZooKeeper<zookeeper-term>`,
  expressed as <host:port>.
  Multiple URLs can be configured to allow for failover.


Usage
-----

Files
-----

$KAFKA_HOME/bin/kafka-console-consumer.sh

See also
--------


