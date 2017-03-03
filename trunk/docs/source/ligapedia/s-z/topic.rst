
.. _topic-term:

Topic
-----

A :ref:`Kafka<kafka-term>` topic stores a stream of records in categories.
Each record consists of a key, a value, and a time stamp.

Kamanja utilizes the Kafka topic concept.

For more information:

- Use the :ref:`kafka-topics-command-ref` command
  to create and delete topics.
- Use the :ref:`kafka-console-command-ref` command
  to read data from a topic.
- When Kamanja is first installed,
  the :ref:`createqueues-command-ref` creates some default topics.
  These topics can be monitored
  with the :ref:`watchqueue-command-ref` commands.


See also:

- `Introduction to Kafka <https://kafka.apache.org/intro>`_
  discusses how topics are implemented in Kafka.


