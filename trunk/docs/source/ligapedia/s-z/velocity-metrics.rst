
.. _velocity-metrics-term:

Velocity metrics
----------------

Velocity Metrics provides statistical information
that can be used to understand the execution performance
of :ref:`message<messages-term>` processing in a Kamanja cluster.
The output tallies the number of files/messages
that were processed or failed;
the feature can be configured to accumulate metrics
for any or all of the following:

- file/message
- message type
- messages by key value
- key string

To implement Velocity Metrics:

- Add the **VelocityStatsInfo** attribute to
  the :ref:`clusterconfig-config-ref` file.
  See :ref:`velmetr-clustconfig` for details.

- Add the **VelocityMetrics** attribute to each
  input and output adapter you are using.
  See :ref:`velmetr-adapter-ref` for details.

- Add the **KamanjaVelocityMetrics** message
  and associated :ref:`containers<container-term>`
  into the :ref:`adapter-binding-config-ref` file.
  See :ref:`kamanjavelocitymetrics-msg-ref`.

- Add the Velocity Metrics properties to the configuration file
  used for the :ref:`filedataconsumer-command-ref` command.

After you run your application with Velocity Metrics implemented,
the results can be viewed using the
:ref:`kafka-console-command-ref` command.
For example:

::

  $KAFKA_HOME/bin/kafka-console-consumer.sh --zookeeper localhost:2181
      --topic velocitymetrics --from-beginning

For more information:

- :ref:`outpututils-velmet` tells how to implement Velocity Metrics
  to the hdfs and jdbc OutputUtils.



